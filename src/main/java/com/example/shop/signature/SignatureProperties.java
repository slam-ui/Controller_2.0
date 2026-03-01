package com.example.shop.signature;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "signature")
@Getter
@Setter
public class SignatureProperties {
    private String keyStorePath;
    private String keyStoreType = "JKS";
    private String keyStorePassword;
    private String keyAlias;
    private String keyPassword;
}
