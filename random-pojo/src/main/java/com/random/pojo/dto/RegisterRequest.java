package com.random.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户注册请求数据传输对象（DTO）。
 */
@Data
public class RegisterRequest {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在3-20位之间")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    private String nickname;

    /** 手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
