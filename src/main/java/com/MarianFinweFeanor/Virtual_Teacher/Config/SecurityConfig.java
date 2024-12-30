package com.MarianFinweFeanor.Virtual_Teacher.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain chain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // Disable CSRF protection for the H2 Console
                .csrf(AbstractHttpConfigurer::disable)

                // Allow H2 console to be displayed in iframes
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return httpSecurity.build();
    }
}


