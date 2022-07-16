package com.xxx.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${wechat.rootUrl}")
    private String rootUrl;

    @Resource
    private RestTemplateBuilder restTemplateBuilder;

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate buildRestTemplate() {
        return restTemplateBuilder
                .rootUri(rootUrl)
                // 设置超时时间
                .setConnectTimeout(Duration.ofSeconds(5))
                .build();
    }
}
