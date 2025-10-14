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
<<<<<<< HEAD
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/mut/register",
                                "/mut/login",
                                "/mut/contribution_period/**",
                                "/mut/contribution/**",
                                "/mut/loan/**",  //  Tous les endpoints loan sont autorisés
                                "/mut/contribution/upload/payment-proof/**",
                                "/mut/contribution/upload/payment-proof/",
                                "/mut/contribution/individual/my-contributions",
                                "/mut/contribution/individual/**",
                                "mut/contribution/individual/member/**",
                                "/mut/admin/**",
                                "/mut/event/**",
                                "/mut/upload/**",
                                "/mut/notification",
                                "/mut/loans/**"
                        ).permitAll()
                        .requestMatchers("/mut/loan-validator/**").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")
                        .requestMatchers("/mut/loan_request/**").hasAnyRole("MEMBER","SECRETARY", "ADMIN","PRESIDENT","TREASURER")
                        .requestMatchers("/mut/loan_request/approval/**", "/mut/loan_request/status/**").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")
                        .requestMatchers("/mut/loan_request/all-with-approval", "/mut/loan_request/my-pending-approvals",
                                "/mut/loan_request/validator-dashboard", "/mut/loan_request/*/approval-status")
                        .hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")
                        .anyRequest().authenticated()
=======
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
            "mut/repayment",
            "/mut/notification").permitAll()

        .requestMatchers("/mut/**").authenticated()

        // Toute autre requête externe doit être authentifiée
        .anyRequest().authenticated()
>>>>>>> cab43455d1c7321b3be4720b9866b944178a04ff
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