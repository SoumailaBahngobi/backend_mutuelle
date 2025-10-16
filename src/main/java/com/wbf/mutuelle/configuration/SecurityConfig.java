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
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Activer CORS
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques (auth non requise)
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

                        // Endpoints nécessitant des rôles de responsables (approbation de prêt, création de cotisation)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/mut/contribution/**").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")
                        .requestMatchers("/mut/loan_request/*/approve/**", "/mut/loan_request/*/reject").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")

                        // Autoriser explicitement la création de demandes de prêt aux utilisateurs authentifiés
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/mut/loan_request").authenticated()

                        // Autoriser les endpoints publics (en lecture) et l'inscription / login
                        // Le endpoint d'upload de photo de profil doit être accessible aux utilisateurs authentifiés
                        .requestMatchers(HttpMethod.POST, "/mut/member/upload-profile").authenticated()

                        // Par défaut, les routes sous /mut/** requièrent une authentification
                        .requestMatchers("/mut/register",
                                "/mut/login",
                                "/mut/contribution_period/**",
                                "/mut/contribution/upload/payment-proof/**",
                                "/mut/contribution/upload/payment-proof/",
                                "/mut/event/**",
                                "/mut/upload/**",
                                "/mut/repayment",
                                "/mut/notification",
                                "/mut/loan_request/**",
                                "/mut/loan_request/*/approve/**",
                                "/mut/loan_request/*/reject/**",
                                "/mut/loan",
                                "/mut/loan/**",
                                "/mut/repayment/**",
                                "/mut/notification").permitAll()

                        .requestMatchers("/mut/**").authenticated()

                        // Toute autre requête externe doit être authentifiée
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // Autoriser toutes les origines
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