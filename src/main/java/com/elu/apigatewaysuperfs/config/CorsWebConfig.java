package com.elu.apigatewaysuperfs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;


@Configuration
public class CorsWebConfig {

    @Bean
    public CorsFilter corsFilter() {

        System.out.println("INSIDET CORS FILTER");
        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        corsConfiguration.addAllowedOrigin("*");
//        corsConfiguration.addAllowedHeader("*");
//        corsConfiguration.setAllowedOrigins(List.of("http://localhost:5000", "http://localhost:5001", "http://127.0.0.1:5000", "http://192.168.49.2:5000"));
        corsConfiguration.addAllowedOriginPattern("*"); // 👈 allows all origins
        corsConfiguration.setAllowedMethods(List.of("*"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setAllowCredentials(true);


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(source);
    }
}
