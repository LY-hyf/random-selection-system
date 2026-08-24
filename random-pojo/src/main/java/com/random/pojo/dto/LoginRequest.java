package com.random.pojo.dto;

import com.sun.xml.internal.bind.v2.TODO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户登录请求数据传输对象（DTO）。
 */
@Data
public class LoginRequest {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
