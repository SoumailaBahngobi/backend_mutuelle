package com.wbf.mutuelle.configuration;

import com.wbf.mutuelle.services.KeycloakUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakSyncFilter extends OncePerRequestFilter {

    private final KeycloakUserService keycloakUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String email = jwt.getClaimAsString("email");
                String keycloakId = jwt.getSubject();

                if (email != null && keycloakId != null) {
                    try {
                        keycloakUserService.syncUserWithDatabase(email, keycloakId);
                    } catch (Exception e) {
                        log.warn("Échec synchronisation Keycloak->BDD pour {}: {}", email, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("KeycloakSyncFilter erreur silencieuse: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
