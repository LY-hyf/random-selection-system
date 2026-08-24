package com.random.pojo.dto;

import lombok.Data;

/**
 * 编辑用户请求。
 */
@Data
public class UserUpdateRequest {

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 状态：1 启用，0 禁用 */
    private Integer status;
}
