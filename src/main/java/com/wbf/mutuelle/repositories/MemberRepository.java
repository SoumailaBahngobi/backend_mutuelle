package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
        Optional<Member> findByEmail(String email);
        Optional<Member> findByKeycloakId(String keycloakId);
        Optional<Member> findByNpi(String npi);

        // ==================== NOUVELLES MÉTHODES POUR LA GESTION DES RÔLES ====================

        /**
         * Récupérer les membres par rôle
         */
        List<Member> findByRole(Role role);

        /**
         * Récupérer les membres dont le rôle n'est pas dans la liste
         */
        @Query("SELECT m FROM Member m WHERE m.role NOT IN :roles")
        List<Member> findByRoleNotIn(@Param("roles") List<Role> roles);

        /**
         * Récupérer les membres avec un rôle spécifique ou un autre
         */
        @Query("SELECT m FROM Member m WHERE m.role = :role1 OR m.role = :role2")
        List<Member> findByRoleOrRole(@Param("role1") Role role1, @Param("role2") Role role2);
}