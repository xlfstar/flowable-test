package com.example.flowable.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签名密钥
     */
    private String secret = "flowable-demo-secret-key-change-me-in-production-0123456789abcdef";

    /**
     * 过期时间, 单位: 秒
     */
    private long expire = 86400;
}
