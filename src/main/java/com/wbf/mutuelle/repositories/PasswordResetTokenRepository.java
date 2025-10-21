package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    PasswordResetToken findByToken(String token);
    void deleteByMember(Member member);
    PasswordResetToken findByMember(Member member);
}
