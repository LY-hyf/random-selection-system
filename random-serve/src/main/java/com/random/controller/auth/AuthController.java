package com.random.controller.auth;

import com.random.annotation.Log;
import com.random.pojo.dto.auth.LoginRequest;
import com.random.pojo.dto.auth.RegisterRequest;
import com.random.pojo.vo.auth.LoginVO;
import com.random.pojo.vo.auth.UserInfoVO;
import com.random.result.Result;
import com.random.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证控制器。
 *
 * <p>提供用户登录、注册、登出及获取当前用户信息等认证相关接口。</p>
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    /** 认证服务 */
    @Autowired
    private AuthService authService;

    /**
     * 用户登录接口。
     *
     * @param request 登录请求体，包含用户名与密码
     * @return 登录结果，包含 JWT Token 与用户信息
     */
    @Log(module = "认证模块", operation = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());
        LoginVO loginVO = authService.login(request);
        return Result.success(loginVO);
    }

    /**
     * 用户注册接口。
     *
     * @param request 注册请求体，包含用户名、密码、昵称与手机号
     * @return 注册结果
     */
    @Log(module = "认证模块", operation = "用户注册")
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());
        authService.register(request);
        return Result.success();
    }

    /**
     * 用户登出接口。
     *
     * @return 登出结果
     */
    @Log(module = "认证模块", operation = "用户登出")
    @PostMapping("/logout")
    public Result logout() {
        log.info("用户登出");
        return Result.success();
    }

    /**
     * 获取当前登录用户信息接口。
     *
     * @return 当前登录用户信息
     */
    @GetMapping("/current-user")
    public Result<UserInfoVO> currentUser() {
        log.debug("获取当前登录用户信息");
        UserInfoVO userInfo = authService.getCurrentUser();
        return Result.success(userInfo);
    }

}
