package com.random.pojo.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录结果视图对象（VO）。
 *
 * <p>封装登录成功后返回的 Token 与用户信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    /** JWT 访问令牌 */
    private String token;

    /** 当前登录用户信息 */
    private UserInfoVO userInfo;
}
