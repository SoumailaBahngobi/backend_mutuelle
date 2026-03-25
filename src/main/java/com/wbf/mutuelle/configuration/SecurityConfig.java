package com.wbf.mutuelle.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final KeycloakSyncFilter keycloakSyncFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques (sans authentification)
                        .requestMatchers("/mutuelle/auth/**").permitAll()
                        .requestMatchers("/mutuelle/public/**").permitAll()
                        .requestMatchers("/mutuelle/register").permitAll()
                        .requestMatchers("/mutuelle/**").permitAll()
                        .requestMatchers("/mutuelle/admin/**").permitAll()
                        //http://localhost:3000/mutuelle/contribution/individual

                        // Routes protégées (authentification requise)
                        .requestMatchers("/mutuelle/member/**").authenticated()
                        .requestMatchers("/mutuelle/loan_request/**").authenticated()
                        .requestMatchers("/mutuelle/loans/**").authenticated()
                        .requestMatchers("/mutuelle/contribution/**").authenticated()
                        .requestMatchers("/mutuelle/contribution_period/**").authenticated()
                        .requestMatchers("/mutuelle/event/**").authenticated()
                        .requestMatchers("/mutuelle/notification/**").authenticated()
                        .requestMatchers("/mutuelle/repayment/**").authenticated()
                        .requestMatchers("/mutuelle/treasurer/**").authenticated()// mutuelle/contribution/individual
                        .requestMatchers("/mutuelle/contribution/individual/**").authenticated()
                        // Toutes les autres routes nécessitent une authentification
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        http.addFilterAfter(keycloakSyncFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // En développement, accepter toutes les origines locales
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*.*:*"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || realmAccess.isEmpty()) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }
    }
}