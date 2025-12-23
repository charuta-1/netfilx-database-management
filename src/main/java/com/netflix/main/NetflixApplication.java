package com.netflix.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

@SpringBootApplication
@ComponentScan(basePackages = "com.netflix")
public class NetflixApplication {

    private static final Logger log = LoggerFactory.getLogger(NetflixApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NetflixApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logFrontendLink(ApplicationReadyEvent event) {
        int port = 8080;
        if (event.getApplicationContext() instanceof ServletWebServerApplicationContext) {
            port = ((ServletWebServerApplicationContext) event.getApplicationContext()).getWebServer().getPort();
        }

        String baseUrl = "http://localhost:" + port;

        log.info("\n====================================================" +
                 "\n Backend ready!" +
                 "\n" +
                 "\n Frontend entry point: {}" +
                 "\n User experience:      {}/user/" +
                 "\n Admin dashboard:      {}/admin/" +
                 "\n API health check:     {}/api/health" +
                 "\n====================================================\n",
                baseUrl,
                baseUrl,
                baseUrl,
                baseUrl);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
            .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}