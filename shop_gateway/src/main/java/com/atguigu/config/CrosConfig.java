package com.atguigu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CrosConfig {
    @Bean
    public CorsWebFilter corsWebFilter(){
        CorsConfiguration config=new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource configSource=new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**",config);
        return new CorsWebFilter(configSource);
    }
}
