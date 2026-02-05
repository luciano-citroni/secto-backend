package com.bridge.secto.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "secto")
@Data
public class SectoProperties {
    private Map<String, PackageInfo> packages;

    @Data
    public static class PackageInfo {
        private String name;
        private Long priceInCents;
        private int credits;
    }
}
