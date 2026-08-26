package com.random.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类。
 *
 * <p>提供 JWT Token 的生成与解析能力，使用 HS256 签名算法。</p>
 */
public class JwtUtil {

    /**
     * 生成 JWT Token。
     *
     * @param secretKey 签名密钥
     * @param ttlMillis Token 有效期（毫秒）
     * @param claims    负载信息（键值对）
     * @return 生成的 JWT Token 字符串
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // SignatureAlgorithm类选择HS256算法
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        // currTimeMills毫秒级方法记录jwt到期时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);
        // JwtBuilder接口创建jwt
        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                // getBytes()方法将secretKey签名密钥字符串转换成字节序列存到字节数组中，标准UTF_8编码数据集
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                .setExpiration(exp);
        // 将已设置好 Header、Claims和签名的JWT序列化为最终的可验证的字符串形式（JWS）
        return builder.compact();
    }

    /**
     * 解析并校验 JWT Token。
     *
     * @param secretKey 签名密钥
     * @param token     待解析的 JWT Token 字符串
     * @return 解析出的负载（Claims）对象，签名不合法或过期时抛出异常
     */
    public static Claims parseJWT(String secretKey, String token) {
        Claims claims = Jwts.parser()
                // 解析签名密钥
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token).getBody();
        // 返回claims（用户信息<userid:username>）
        return claims;
    }

}
