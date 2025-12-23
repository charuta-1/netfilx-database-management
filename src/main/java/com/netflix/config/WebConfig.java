package com.netflix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve frontend resources
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/frontend/")
                .setCachePeriod(0);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Default index page
        registry.addViewController("/").setViewName("forward:/login.html");
        registry.addViewController("/login").setViewName("forward:/login.html");
    }
}

