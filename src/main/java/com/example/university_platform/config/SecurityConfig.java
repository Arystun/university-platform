package com.example.university_platform.config;


import com.example.university_platform.service.CustomUserDetailsService; // Импортируем наш сервис
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;



@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService; // Внедряем зависимость
    private final AuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          AuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService); // Используем наш UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider()) // Регистрируем наш провайдер
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/h2-console/**", "/login", "/error", "/register").permitAll() // Добавим /register
                        .requestMatchers("/api/messages/public").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/messages/**").hasRole("USER") // <<< ДОБАВЬТЕ ЭТУ СТРОКУ (или .authenticated())
                        .requestMatchers("/user/dashboard").hasRole("USER")
                        .requestMatchers("/api/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login") // URL, на который будут отправляться данные формы
                        //.defaultSuccessUrl("/admin/dashboard", true)
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true") // URL при ошибке логина
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/perform_logout") // URL для выхода
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                        .ignoringRequestMatchers("/api/**")
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .exceptionHandling(exceptions -> exceptions
                                .defaultAuthenticationEntryPointFor( // Для путей /api/**
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), // Возвращать 401 Unauthorized
                                        new AntPathRequestMatcher("/api/**") // Применять это для путей, начинающихся с /api/
                                )
                        // Для остальных путей будет использоваться стандартная логика formLogin (редирект на /login)
                );

        return http.build();
    }
}