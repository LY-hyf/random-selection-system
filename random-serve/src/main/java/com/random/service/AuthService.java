package com.random.service;

import com.random.pojo.dto.LoginRequest;
import com.random.pojo.dto.RegisterRequest;
import com.random.pojo.vo.LoginVO;
import com.random.pojo.vo.UserInfoVO;

/**
 * 认证服务接口。
 *
 * <p>定义用户登录、注册与获取当前用户信息的业务能力。</p>
 */
public interface AuthService {

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录结果，包含 JWT Token 与用户信息
     */
    LoginVO login(LoginRequest request);

    /**
     * 用户注册。
     *
     * @param request 注册请求
     */
    void register(RegisterRequest request);

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前登录用户信息
     */
    UserInfoVO getCurrentUser();

}
