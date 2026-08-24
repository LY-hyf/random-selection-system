package com.random.pojo.dto;

import lombok.Data;

/**
 * 重置密码请求。
 */
@Data
public class ResetPasswordRequest {

    /** 新密码 */
    private String newPassword;
}
