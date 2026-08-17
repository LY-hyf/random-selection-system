package com.random.constant;

/**
 * JWT 负载（Claims）键名常量类。
 *
 * <p>用于统一管理生成与解析 JWT Token 时使用的键名，
 * 避免在业务代码中散落魔法字符串。</p>
 */
public class JwtClaimsConstant {

    /** JWT 负载中存放用户 ID 的键名 */
    public static final String USER_ID = "userId";

    /** JWT 负载中存放用户名的键名 */
    public static final String USERNAME = "username";

}
