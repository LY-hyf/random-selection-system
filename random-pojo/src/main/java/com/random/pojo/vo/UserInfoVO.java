package com.random.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户信息视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO implements Serializable {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 用户拥有的角色编码集合 */
    private List<String> roles;

    /** 用户拥有的权限编码集合 */
    private List<String> permissions;
}
