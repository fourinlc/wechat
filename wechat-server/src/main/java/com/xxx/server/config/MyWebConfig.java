package com.xxx.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {

    @Value("${wechat.file.basePath}")
    private String basePath;

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //项目上传图片的映射
        registry.addResourceHandler("images/**").addResourceLocations("file:" + basePath);
    }
}