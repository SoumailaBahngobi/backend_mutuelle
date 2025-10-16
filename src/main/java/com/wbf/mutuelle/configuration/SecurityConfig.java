package com.wbf.mutuelle.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques
                        .requestMatchers(
                                "/mut/register",
                                "/mut/login",
                                "/mut/contribution_period/**",
                                "/mut/contribution/upload/payment-proof/**",
                                "/mut/contribution/upload/payment-proof/",
                                "/mut/event/**",
                                "/mut/upload/**",
                                "/mut/notification"
                        ).permitAll()

                        // 🔥 CORRECTION DÉFINITIVE : Autoriser les remboursements pour les rôles
                        .requestMatchers(HttpMethod.GET, "/mut/repayment/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/mut/repayment/**").hasAnyRole("TREASURER", "ADMIN", "SECRETARY", "PRESIDENT")
                        .requestMatchers(HttpMethod.PUT, "/mut/repayment/**").hasAnyRole("TREASURER", "ADMIN", "SECRETARY", "PRESIDENT")
                        .requestMatchers(HttpMethod.DELETE, "/mut/repayment/**").hasAnyRole("TREASURER", "ADMIN")

                        // Endpoints pour responsables
                        .requestMatchers(HttpMethod.POST, "/mut/contribution/**").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")
                        .requestMatchers("/mut/loan_request/*/approve/**", "/mut/loan_request/*/reject").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")

                        // Endpoints trésorier
                        .requestMatchers("/mut/treasurer/**").hasRole("TREASURER")
                        .requestMatchers("/mut/loan_request/treasurer/**").hasRole("TREASURER")

                        // Endpoints authentifiés généraux
                        .requestMatchers(HttpMethod.POST, "/mut/loan_request").authenticated()
                        .requestMatchers(HttpMethod.POST, "/mut/member/upload-profile").authenticated()

                        // Endpoints de lecture
                        .requestMatchers(HttpMethod.GET, "/mut/loan_request/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/mut/loan/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/mut/member/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/mut/loan-requests/approved").authenticated()

                        // Par défaut - authentification requise
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}