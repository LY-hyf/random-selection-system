package com.random.security;

import com.random.constant.JwtClaimsConstant;
import com.random.context.BaseContext;
import com.random.mapper.permission.SysPermissionMapper;
import com.random.mapper.role.SysRoleMapper;
import com.random.properties.JwtProperties;
import com.random.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器。
 *
 * <p>在每个请求处理前解析并校验 JWT Token，
 * 校验通过后将认证信息写入安全上下文并记录当前用户 ID。</p>
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 配置属性 */
    @Autowired
    private JwtProperties jwtProperties;

    /** 角色数据访问接口 */
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 权限数据访问接口 */
    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    /**
     * 执行 JWT 认证过滤逻辑。
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链
     * @throws ServletException 过滤过程发生 Servlet 异常时抛出
     * @throws IOException      过滤过程发生 IO 异常时抛出
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            // 声明FilterChain.doFilter()可能出现的异常
            throws ServletException, IOException {
        // 从请求标头Authorization : Bearer token中获取Bearer token
        String tokenName = request.getHeader(jwtProperties.getAdminTokenName());
        // 校验token名称是否以"Bearer"开头
        String token = null;
        if (tokenName != null && tokenName.startsWith("Bearer ")) {
            // 截取tokenName的token(第7位-最后一位)
            token = tokenName.substring(7);
        }
        /**
         * 构建认证令牌存入当前线程上下文
         */
        if (token != null) {
            try {
                // 调用jwt工具类的解析方法解析签名和token返回为Claims类的用户信息（userid:username）
                Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
                // 返回Long对象userid，保存由字符串表示的值
                Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
                String username = claims.get(JwtClaimsConstant.USERNAME).toString();
                log.debug("jwt校验通过，userId: {}, username: {}", userId, username);
                /**
                 * 同时加载角色编码与权限编码，作为鉴权依据（权限禁用后 getPermissionCodesByUserId不再返回权限则方法级鉴权校验失败）
                 * 全部包装成SpringSecurity可识别的GrantedAuthority对象
                 */
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                List<String> roleCodes = sysRoleMapper.getRoleCodesByUserId(userId);
                if (roleCodes != null) {
                    roleCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
                }
                List<String> permissionCodes = sysPermissionMapper.getPermissionCodesByUserId(userId);
                if (permissionCodes != null) {
                    permissionCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
                }
                // 构建认证令牌(username,角色与权限编码)，存入SecurityContextHolder
                UsernamePasswordAuthenticationToken authentication =
                        // 认证令牌不存储用户密码，使用验证签代替
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                /**
                 * 如何清理SecurityContext?
                 * Spring Security 内部有一个 SecurityContextPersistenceFilter
                 * 它会在请求结束后自动调用 SecurityContextHolder.clearContext()
                 */
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // 存入当前线程上下文中
                BaseContext.setCurrentId(userId);
            } catch (Exception e) {
                // token 过期/无效属于正常情况，用 warn 级别避免刷屏
                log.warn("jwt校验失败: {}", e.getMessage());
            }
        }

        try {
            /**
             * FilterChain.doFilter(request, response)方法将请求和响应传递给链中的下一个过滤器，确保请求继续通过链中的所有过滤器
             * 如果过滤器链中没有过滤器，则会将请求转发给目标资源（通常是 Servlet），并将响应发送给客户端
             * 在身份验证或授权失败的情况下，则需要跳过方法调用并中断调用链
             */
            filterChain.doFilter(request, response);
        }finally {
            // 移除当前线程的用户 ID，防止线程复用导致的数据污染或内存泄漏
            /**
             * Tomcat 为了节省资源，不会在请求结束后销毁线程，而是将线程放回线程池（http-nio-8080-exec-*）中复用。
             * ThreadLocal 的数据是绑定在当前线程对象上的（存在线程的 ThreadLocalMap 里）。
             * 请求 A 处理完毕，线程回到池中，但线程的 ThreadLocalMap 里还存着用户 A 的 userId。
             * 请求 B（来自另一个用户）被分配到了同一个线程。
             * 请求 B 在 Controller 中调用 BaseContext.getCurrentId()，拿到的会是用户 A 的 ID！
             * 结果：用户 B 的操作（如新增专家）会被记录为“由用户 A 操作”，导致严重的串号事故（数据污染）。
             * 内存泄漏（OOM）
             * 如果 ThreadLocal 不被 remove()，由于线程是长期存活的，ThreadLocalMap 会一直持有 ThreadLocal 的引用以及 Value
             * （userId 虽然是 Long，但如果存的是大对象，内存会不断累积），最终导致 PermGen/Metaspace 或堆内存溢出
             * 为什么一定要在 finally 里清理？
             * 因为 filterChain.doFilter(request, response); 这行代码会执行后续所有的业务逻辑（Controller、Service、Interceptor 等）。如果业务中抛出异常，finally 块依然会执行，确保即使程序出错，ThreadLocal 也能被清空。
             * 如果写在 try 块的末尾（没有 finally），一旦业务抛出异常，清理代码就不会执行，导致脏数据残留。
             */
            BaseContext.removeCurrentId();
        }
    }

}
