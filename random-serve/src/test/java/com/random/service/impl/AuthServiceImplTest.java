package com.random.service.impl;

import com.random.context.BaseContext;
import com.random.exception.AccountLockedException;
import com.random.exception.AccountNotFoundException;
import com.random.exception.LoginFailedException;
import com.random.exception.PasswordErrorException;
import com.random.mapper.permission.SysPermissionMapper;
import com.random.mapper.role.SysRoleMapper;
import com.random.mapper.user.SysUserMapper;
import com.random.mapper.user.SysUserRoleMapper;
import com.random.pojo.dto.LoginRequest;
import com.random.pojo.dto.RegisterRequest;
import com.random.pojo.entity.SysRole;
import com.random.pojo.entity.SysUser;
import com.random.pojo.vo.LoginVO;
import com.random.pojo.vo.UserInfoVO;
import com.random.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        BaseContext.removeCurrentId();
    }

    @Test
    void login_成功返回token和用户信息() {
        when(jwtProperties.getAdminSecretKey()).thenReturn("test-secret-key");
        when(jwtProperties.getAdminTtl()).thenReturn(7200000L);
        when(sysUserMapper.getByUsername("admin")).thenReturn(user(1));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        LoginVO result = authService.login(loginRequest());

        assertNotNull(result.getToken());
        assertNotNull(result.getUserInfo());
        assertEquals("admin", result.getUserInfo().getUsername());
    }

    @Test
    void login_用户不存在抛异常() {
        when(sysUserMapper.getByUsername("nobody")).thenReturn(null);

        assertThrows(AccountNotFoundException.class, () -> authService.login(loginRequest("nobody")));
    }

    @Test
    void login_密码错误抛异常() {
        when(sysUserMapper.getByUsername("admin")).thenReturn(user(1));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(PasswordErrorException.class, () -> authService.login(loginRequest("admin", "wrong")));
    }

    @Test
    void login_账号被禁用抛异常() {
        when(sysUserMapper.getByUsername("admin")).thenReturn(user(0));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        assertThrows(AccountLockedException.class, () -> authService.login(loginRequest()));
    }

    @Test
    void register_用户名已存在抛异常() {
        when(sysUserMapper.getByUsername("admin")).thenReturn(user(1));

        assertThrows(LoginFailedException.class, () -> authService.register(registerRequest()));
    }

    @Test
    void register_成功插入用户() {
        when(sysUserMapper.getByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(sysRoleMapper.getByRoleCode("USER")).thenReturn(userRole());

        authService.register(registerRequest("newuser"));

        verify(sysUserMapper).insert(any(SysUser.class));
    }

    @Test
    void getCurrentUser_未登录抛异常() {
        BaseContext.removeCurrentId();

        assertThrows(LoginFailedException.class, () -> authService.getCurrentUser());
    }

    @Test
    void getCurrentUser_成功返回当前用户() {
        BaseContext.setCurrentId(1L);
        when(sysUserMapper.getById(1L)).thenReturn(user(1));

        UserInfoVO result = authService.getCurrentUser();

        assertEquals("admin", result.getUsername());
        BaseContext.removeCurrentId();
    }

    private SysUser user(int status) {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setUsername("admin");
        u.setPassword("encoded");
        u.setNickname("管理员");
        u.setPhone("13800000001");
        u.setStatus(status);
        return u;
    }

    private SysRole userRole() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleName("普通用户");
        role.setRoleCode("USER");
        return role;
    }

    private LoginRequest loginRequest() {
        return loginRequest("admin", "123456");
    }

    private LoginRequest loginRequest(String username) {
        return loginRequest(username, "123456");
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUsername(username);
        r.setPassword(password);
        return r;
    }

    private RegisterRequest registerRequest() {
        return registerRequest("admin");
    }

    private RegisterRequest registerRequest(String username) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setPassword("123456");
        r.setNickname("用户");
        r.setPhone("13800000000");
        return r;
    }
}
