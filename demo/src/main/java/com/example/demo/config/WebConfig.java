package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-url}")
    private String accessUrl;

    @Value("${post.image.upload-dir}")
    private String postImageUploadDir;

    @Value("${post.image.access-url}")
    private String postImageAccessUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 头像目录映射
        registry.addResourceHandler(accessUrl + "**")
                .addResourceLocations("file:" + uploadDir);
        // 帖子图片目录映射
        registry.addResourceHandler(postImageAccessUrl + "**")
                .addResourceLocations("file:" + postImageUploadDir);
    }
}