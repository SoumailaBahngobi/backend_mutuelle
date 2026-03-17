package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeycloakUserServiceTest {

    private Keycloak keycloak;
    private MemberRepository memberRepository;
    private KeycloakUserService service;

    @BeforeEach
    void setUp() {
        keycloak = Mockito.mock(Keycloak.class);
        memberRepository = Mockito.mock(MemberRepository.class);
        service = Mockito.spy(new KeycloakUserService(keycloak, memberRepository));
    }

    @Test
    void sync_existing_member_updates_fields_from_keycloak() {
        String email = "user@example.com";
        String kcId = "kc-123";

        Member existing = new Member();
        existing.setEmail(email);
        existing.setPhone("old");

        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(existing));

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId(kcId);
        kcUser.setFirstName("John");
        kcUser.setLastName("Doe");
        kcUser.setEmail(email);
        Map<String, java.util.List<String>> attrs = new HashMap<>();
        attrs.put("phone", java.util.List.of("+221700000000"));
        attrs.put("npi", java.util.List.of("NPI-42"));
        kcUser.setAttributes((Map) attrs);

        doReturn(kcUser).when(service).getUserById(kcId);

        when(memberRepository.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));

        Member result = service.syncUserWithDatabase(email, kcId);

        assertThat(result.getKeycloakId()).isEqualTo(kcId);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getName()).isEqualTo("Doe");
        assertThat(result.getPhone()).isEqualTo("+221700000000");
        assertThat(result.getNpi()).isEqualTo("NPI-42");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void sync_creates_new_member_when_missing() {
        String email = "new@example.com";
        String kcId = "kc-999";

        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setId(kcId);
        kcUser.setFirstName("Alice");
        kcUser.setLastName("Smith");
        kcUser.setEmail(email);
        Map<String, java.util.List<String>> attrs = new HashMap<>();
        attrs.put("phone", java.util.List.of("+221712345678"));
        kcUser.setAttributes((Map) attrs);

        doReturn(kcUser).when(service).getUserByEmail(email);

        when(memberRepository.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));

        Member created = service.syncUserWithDatabase(email, "");

        assertThat(created.getEmail()).isEqualTo(email);
        assertThat(created.getKeycloakId()).isEqualTo(kcId);
        assertThat(created.getFirstName()).isEqualTo("Alice");
        assertThat(created.getName()).isEqualTo("Smith");
        assertThat(created.getPhone()).isEqualTo("+221712345678");
        verify(memberRepository).save(any(Member.class));
    }
}
