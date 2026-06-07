package com.sport.club.config;

import com.sport.club.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    config.setAllowedOriginPatterns(List.of(
                            "http://localhost:5173",
                            "https://force-lab.vercel.app",
                            "https://force-lab-front.vercel.app",
                            "https://*.vercel.app"
                    ));

                    config.setAllowedMethods(List.of(
                            "GET",
                            "POST",
                            "PUT",
                            "DELETE",
                            "PATCH",
                            "OPTIONS"
                    ));

                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);

                    return config;
                }))

                .authorizeHttpRequests(auth -> auth

                        // CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // AUTH
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // SSE
                        .requestMatchers("/api/sse/subscribe").permitAll()

                        // PUBLIC API
                        .requestMatchers("/api/trainings/upcoming").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trainings/upcoming").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trainings/active-for-marking").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trainings/completed").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trainings/all").permitAll()

                        // PRIVATE API
                        .requestMatchers("/api/trainings/my-with-status").authenticated()

                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider)

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}