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
            throws ServletException, IOException {
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null) {
            try {
                Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
                Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
                String username = claims.get(JwtClaimsConstant.USERNAME).toString();

                log.debug("jwt校验通过，userId: {}, username: {}", userId, username);

                // 同时加载角色编码与权限编码，作为鉴权依据（权限禁用后 getPermissionCodesByUserId 不再返回）
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                List<String> roleCodes = sysRoleMapper.getRoleCodesByUserId(userId);
                if (roleCodes != null) {
                    roleCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
                }
                List<String> permissionCodes = sysPermissionMapper.getPermissionCodesByUserId(userId);
                if (permissionCodes != null) {
                    permissionCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                BaseContext.setCurrentId(userId);
            } catch (Exception e) {
                // token 过期/无效属于正常情况，用 warn 级别避免刷屏
                log.warn("jwt校验失败: {}", e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            BaseContext.removeCurrentId();
        }
    }

}
