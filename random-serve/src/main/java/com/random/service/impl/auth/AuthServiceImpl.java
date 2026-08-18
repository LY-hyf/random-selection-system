package com.random.service.impl.auth;

import com.random.constant.JwtClaimsConstant;
import com.random.constant.MessageConstant;
import com.random.constant.StatusConstant;
import com.random.context.BaseContext;
import com.random.exception.*;
import com.random.mapper.permission.SysPermissionMapper;
import com.random.mapper.role.SysRoleMapper;
import com.random.mapper.user.SysUserMapper;
import com.random.mapper.user.SysUserRoleMapper;
import com.random.pojo.dto.auth.LoginRequest;
import com.random.pojo.dto.auth.RegisterRequest;
import com.random.pojo.entity.role.SysRole;
import com.random.pojo.entity.user.SysUser;
import com.random.pojo.vo.auth.LoginVO;
import com.random.pojo.vo.auth.UserInfoVO;
import com.random.properties.JwtProperties;
import com.random.service.auth.AuthService;
import com.random.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现类。
 *
 * <p>实现用户登录、注册与获取当前用户信息的具体业务逻辑。</p>
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    /** 系统用户数据访问接口 */
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 角色数据访问接口 */
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 权限数据访问接口 */
    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    /** 密码编码器 */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** JWT 配置属性 */
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /**
     * 用户登录。
     *
     * <p>登录流程如下：</p>
     * <ol>
     *   <li>根据用户名查询用户信息，若不存在则抛出账号不存在异常；</li>
     *   <li>校验密码是否匹配，不匹配则抛出密码错误异常；</li>
     *   <li>检查账号状态，若被禁用则抛出账号锁定异常；</li>
     *   <li>生成包含用户ID和用户名的 JWT Token；</li>
     *   <li>查询当前用户拥有的所有角色编码和权限编码（用于前端动态路由和按钮级权限控制）；</li>
     *   <li>组装返回登录成功信息（Token + 用户基本信息 + 角色 + 权限）。</li>
     * </ol>
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录成功后的视图对象，包含 JWT Token 和用户详细信息
     * @throws AccountNotFoundException 账号不存在时抛出
     * @throws PasswordErrorException   密码错误时抛出
     * @throws AccountLockedException   账号被禁用时抛出
     */
    @Override
    public LoginVO login(LoginRequest request) {
        // 1. 提取用户名和密码
        String username = request.getUsername();
        String password = request.getPassword();
        // 2. 根据用户名查询用户信息（含密码密文、状态等）
        SysUser user = sysUserMapper.getByUsername(username);
        if (user == null) {
            // 若用户不存在，直接抛出异常，由全局异常处理器返回友好提示
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 3. 密码校验：使用 BCrypt 密码编码器比对明文和密文
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        // 4. 账号状态校验：如果状态为禁用（0），不允许登录
        if (StatusConstant.DISABLE.equals(user.getStatus())) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        // 5. 构建 JWT 的载荷（Payload）信息
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());   // 用户ID
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername()); // 用户名
        // 6. 使用 JwtUtil 工具类生成 Token（包含过期时间、密钥等）
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),  // 签名密钥
                jwtProperties.getAdminTtl(),        // Token 有效期（毫秒）
                claims
        );
        // 7. 查询当前用户拥有的所有角色编码（如 "ADMIN", "USER"）
        List<String> roles = sysRoleMapper.getRoleCodesByUserId(user.getId());
        if (roles == null) {
            roles = Collections.emptyList(); // 防止空指针
        }
        // 8. 查询当前用户拥有的所有权限编码（如 "expert:add:submit"）
        List<String> permissions = sysPermissionMapper.getPermissionCodesByUserId(user.getId());
        if (permissions == null) {
            permissions = Collections.emptyList();
        }
        // 9. 组装用户信息视图对象（用于前端展示和权限渲染）
        UserInfoVO userInfo = UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .roles(roles)               // 角色列表
                .permissions(permissions)   // 权限列表
                .build();
        // 10. 记录登录成功日志（便于审计和排查）
        log.info("登录成功，userId: {}, username: {}, roles: {}", user.getId(), user.getUsername(), roles);
        // 11. 返回最终登录结果（Token + 用户信息）
        return LoginVO.builder()
                .token(token)
                .userInfo(userInfo)
                .build();
    }

    /**
     * 用户注册。
     *
     * <p>校验用户名是否已存在，不存在则加密密码并写入用户表。</p>
     *
     * @param request 注册请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 1. 校验用户名
        SysUser existing = sysUserMapper.getByUsername(request.getUsername());
        if (existing != null) {
            throw new LoginFailedException("用户名已存在");
        }
        // 2. 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(StatusConstant.ENABLE);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(user);  // 需要 @Options(useGeneratedKeys=true)
        // 3. 查询普通用户角色
        SysRole userRole = sysRoleMapper.getByRoleCode("USER");
        if (userRole == null) {
            log.error("普通用户角色（code=USER）不存在，无法为新用户分配角色");
            throw new BaseException("系统配置错误，请联系管理员");
        }
        // 4. 分配角色（使用单条插入）
        sysUserRoleMapper.insert(user.getId(), userRole.getId());
        log.info("注册成功，userId: {}, username: {}, 分配角色: {}",
                user.getId(), user.getUsername(), userRole.getRoleName());
    }

    /**
     * 获取当前登录用户信息。
     *
     * <p>从请求上下文获取当前用户 ID，查询用户并组装角色与权限信息。</p>
     *
     * @return 当前登录用户信息
     */
    @Override
    public UserInfoVO getCurrentUser() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new LoginFailedException(MessageConstant.USER_NOT_LOGIN);
        }

        SysUser user = sysUserMapper.getById(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        List<String> roles = sysRoleMapper.getRoleCodesByUserId(userId);
        if (roles == null) {
            roles = Collections.emptyList();
        }
        List<String> permissions = sysPermissionMapper.getPermissionCodesByUserId(userId);
        if (permissions == null) {
            permissions = Collections.emptyList();
        }

        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

}
