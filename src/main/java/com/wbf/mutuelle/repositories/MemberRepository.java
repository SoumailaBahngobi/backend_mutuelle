package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
        Optional<Member> findByEmail(String email);
        Optional<Member> findByKeycloakId(String keycloakId);
        Optional<Member> findByNpi(String npi);
}