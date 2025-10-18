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
                        // Routes publiques (auth non requise) - DOIVENT ÊTRE EN PREMIER
                        .requestMatchers(
                                "/mut/register",
                                "/mut/login",
                                "/mut/contribution_period/**",
                                "/mut/contribution/upload/payment-proof/**",
                                "/mut/event/**",
                                "/mut/upload/**",
                                "/mut/notification",
                                "/mut/contribution/individual"
                        ).permitAll()

                        // ✅ CORRECTION : Permettre à tous les utilisateurs authentifiés de faire des cotisations
                        .requestMatchers(HttpMethod.POST, "/mut/contribution/**").authenticated()

                        // Endpoints spécifiques pour le trésorier
                        .requestMatchers("/mut/treasurer/**").hasRole("TREASURER")
                        .requestMatchers("/mut/loan_request/treasurer/**").hasRole("TREASURER")

                        // Endpoints nécessitant des rôles de responsables (uniquement l'approbation/rejet de prêt)
                        .requestMatchers("/mut/loan_request/*/approve", "/mut/loan_request/*/reject").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")

                        // Upload de profil pour utilisateurs authentifiés
                        .requestMatchers(HttpMethod.POST, "/mut/member/upload-profile").authenticated()

                        // Création de demandes de prêt pour utilisateurs authentifiés
                        .requestMatchers(HttpMethod.POST, "/mut/loan_request").authenticated()

                        // Par défaut, toutes les autres routes sous /mut/** requièrent une authentification
                        .requestMatchers("/mut/**").authenticated()

                        // Toute autre requête doit être authentifiée
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