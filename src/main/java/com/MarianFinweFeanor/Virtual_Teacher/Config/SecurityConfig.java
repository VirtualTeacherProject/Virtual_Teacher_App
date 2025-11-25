package com.MarianFinweFeanor.Virtual_Teacher.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private CustomUserDetailsService userDetailsService;


    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService) {

        this.userDetailsService = userDetailsService;
    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return org.springframework.security.crypto.password.NoOpPasswordEncoder.
//                getInstance();
//    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        // cost 12 is a good default; 10–12 is common for web apps
//        return new org.springframework.security.crypto.bcrypt.
//                BCryptPasswordEncoder(12);
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        String idForEncode = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12));
        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
        return new org.springframework.security.crypto.password.DelegatingPasswordEncoder(idForEncode, encoders);
    }


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return auth.build();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        // 1) Public: home, login, register, H2 console
                        .requestMatchers(
                                new AntPathRequestMatcher("/", "GET"),
                                new AntPathRequestMatcher("/login", "GET"),
                                new AntPathRequestMatcher("/home", "GET"),
                                new AntPathRequestMatcher("/register", "GET"),
                                new AntPathRequestMatcher("/h2-console/**")
                        ).permitAll()

                        // 2) Public: browse courses & lectures (GET only)
                        .requestMatchers(
                                new AntPathRequestMatcher("/courses", "GET"),
                                new AntPathRequestMatcher("/courses/**", "GET"),
                                new AntPathRequestMatcher("/courses/*/lectures", "GET"),
                                new AntPathRequestMatcher("/courses/*/lectures/**", "GET")
                        ).permitAll()

                        // 3) Public: user registration POST
                        .requestMatchers(
                                new AntPathRequestMatcher("/api/users", "POST"),
                                new AntPathRequestMatcher("/register", "POST")
                        ).permitAll()

                        // 4) Teacher-only: add lectures
                        .requestMatchers(
                                new AntPathRequestMatcher("/courses/*/lectures/add-lecture", "GET"),
                                new AntPathRequestMatcher("/courses/*/lectures/add-lecture", "POST"),
                                new AntPathRequestMatcher("/courses/*/lectures/*/edit", "POST"),
                                new AntPathRequestMatcher("/courses/add", "POST"),
                                new AntPathRequestMatcher("/courses/*/edit", "POST")
                        ).hasRole("TEACHER")

                        // 5) only teachers/admins can see list of enrolled students
                        .requestMatchers(new AntPathRequestMatcher("/courses/*/students", "GET"))
                        .hasAnyRole("TEACHER","ADMIN")

                        // 6) Authenticated (STUDENT/TEACHER/ADMIN): enroll & submit
                        .requestMatchers(
                                new AntPathRequestMatcher("/courses/*/enroll", "POST"),
                                new AntPathRequestMatcher("/courses/*/lectures/*/assignments", "POST")

                        ).hasAnyRole("STUDENT","TEACHER","ADMIN")

                        // 7) Secure all other API endpoints
                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()




                        //8) Admin Panel features
                        .requestMatchers(
                                new AntPathRequestMatcher("/admin/**"))

                        .hasRole("ADMIN")   // or hasAuthority("ADMIN")


                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/register"),
                                new AntPathRequestMatcher("/login"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**")
                        )
                        .permitAll()

                        // 9) Everything else (e.g. /home, /profile) needs login
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .headers(headers -> headers.frameOptions().disable()) // required for H2 console
                .build();
    }

}


