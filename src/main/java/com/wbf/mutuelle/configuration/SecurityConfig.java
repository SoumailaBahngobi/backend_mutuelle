package com.wbf.mutuelle.configuration;

import com.wbf.mutuelle.configuration.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
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

                                "/mutuelle/**",
                                "/mutuelle/register",
                                "/mutuelle/login",
                                "/mutuelle/contribution_period/**",
                                "/mutuelle/contribution/upload/payment-proof/**",
                                "/mutuelle/event/**",
                                "/mutuelle/repayment/history",
                                "/mutuelle/repayment/history/**",
                                "/mutuelle/upload/**",
                                "/mutuelle/member/upload-profile",
                                "/mutuelle/member/profile/photo",
                                "/mutuelle/member/**",
                                "/mutuelle/loans",
                                "/mutuelle/notification",
                                "/mutuelle/contribution/individual",
                                "/mutuelle/member/profile/update/**",
                                "/mutuelle/member/forgot-password",
                                "/mutuelle/member/reset-password",
                                "/mutuelle/repayment/**",
                                "/mutuelle/repayment/simple",
                                "mutuelle/repayment"
                        ).permitAll()

                        // Endpoints spécifiques pour le trésorier
                        .requestMatchers("/mutuelle/treasurer/**").hasRole("TREASURER")
                        .requestMatchers("/mutuelle/loan_request/treasurer/**").hasRole("TREASURER")

                        // Endpoints nécessitant des rôles de responsables
                        .requestMatchers("/mutuelle/loan_request/*/approve", "/mutuelle/loan_request/*/reject").hasAnyRole("PRESIDENT", "SECRETARY", "TREASURER", "ADMIN")

                        // Upload de profil
                        .requestMatchers(HttpMethod.POST, "/mutuelle/member/upload-profile").authenticated()

                        // Demandes de prêt
                        .requestMatchers(HttpMethod.POST, "/mutuelle/loan_request").authenticated()
                        .requestMatchers(HttpMethod.GET, "/mutuelle/loan_request/**").authenticated()

                        // Cotisations
                        .requestMatchers(HttpMethod.POST, "/mutuelle/contribution/**").authenticated()

                        // Par défaut, toutes les autres routes sous /mutuelle/** requièrent une authentification
                        .requestMatchers("/mutuelle/**").authenticated()

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