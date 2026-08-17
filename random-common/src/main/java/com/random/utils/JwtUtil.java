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
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                .setExpiration(exp);

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
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token).getBody();
        return claims;
    }

}
