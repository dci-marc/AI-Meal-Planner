package org.dci.aimealplanner.security;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final CustomLoginSuccessHandler loginSuccessHandler;
    private final UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/auth/**", "/index", "/index/", "/css/**", "/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recipes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recipes/").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/auth/login")
                        .usernameParameter("email")
                        .successHandler(loginSuccessHandler)
                        .permitAll()
                )
                .oauth2Login(oauth2Login -> {
                    oauth2Login.loginPage("/auth/login")
                            .successHandler(loginSuccessHandler);
                })
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth/login")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        builder.userDetailsService(userService)
                .passwordEncoder(passwordEncoder);

        return builder.build();
    }

}
