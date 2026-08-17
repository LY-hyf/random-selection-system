package com.random.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性类。
 *
 * <p>从配置文件（application.yml）中以 {@code jwt} 为前缀读取
 * 管理员 JWT 相关的密钥、有效期和 Token 名称。</p>
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /** 管理员 JWT 签名密钥 */
    private String adminSecretKey;

    /** 管理员 JWT 有效期（毫秒） */
    private long adminTtl;

    /** 管理员 Token 在请求头中的名称 */
    private String adminTokenName;

}
