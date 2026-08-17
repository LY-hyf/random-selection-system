package com.random.service.impl.auth;

import com.random.constant.JwtClaimsConstant;
import com.random.constant.MessageConstant;
import com.random.constant.StatusConstant;
import com.random.context.BaseContext;
import com.random.exception.AccountLockedException;
import com.random.exception.AccountNotFoundException;
import com.random.exception.LoginFailedException;
import com.random.exception.PasswordErrorException;
import com.random.mapper.permission.SysPermissionMapper;
import com.random.mapper.role.SysRoleMapper;
import com.random.mapper.user.SysUserMapper;
import com.random.pojo.dto.auth.LoginRequest;
import com.random.pojo.dto.auth.RegisterRequest;
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

    /**
     * 用户登录。
     *
     * <p>依次校验账号是否存在、密码是否正确、账号是否可用，
     * 校验通过后生成 JWT Token 并组装用户信息与权限返回。</p>
     *
     * @param request 登录请求
     * @return 登录结果，包含 JWT Token 与用户信息
     */
    @Override
    public LoginVO login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        SysUser user = sysUserMapper.getByUsername(username);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (StatusConstant.DISABLE.equals(user.getStatus())) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        String token = JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);

        List<String> roles = sysRoleMapper.getRoleCodesByUserId(user.getId());
        if (roles == null) {
            roles = Collections.emptyList();
        }
        List<String> permissions = sysPermissionMapper.getPermissionCodesByUserId(user.getId());
        if (permissions == null) {
            permissions = Collections.emptyList();
        }

        UserInfoVO userInfo = UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .roles(roles)
                .permissions(permissions)
                .build();

        log.info("登录成功，userId: {}, username: {}, roles: {}", user.getId(), user.getUsername(), roles);
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
    public void register(RegisterRequest request) {
        SysUser existing = sysUserMapper.getByUsername(request.getUsername());
        if (existing != null) {
            throw new LoginFailedException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(StatusConstant.ENABLE);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());

        sysUserMapper.insert(user);
        log.info("注册成功，userId: {}, username: {}", user.getId(), user.getUsername());
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
