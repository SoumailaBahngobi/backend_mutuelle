package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getAllMembers(){
        return memberRepository.findAll();
    }

    public Optional<Member> getMemberById(Long id){
        return memberRepository.findById(id);
    }

    public Member createMember(Member member){
        if (member.getIsRegular() == null) member.setIsRegular(false);
        if (member.getHasPreviousDebt() == null) member.setHasPreviousDebt(false);
        if (member.getSubscriptionStatus() == null) member.setSubscriptionStatus("PENDING");
       return memberRepository.save(member);
    }

    public Member updateMember(Long id, Member memberDetails){
     Member member = memberRepository.findById(id).orElseThrow();
     member.setName(memberDetails.getName());
        member.setFirstName(memberDetails.getFirstName());
        member.setName(memberDetails.getName());
        member.setEmail(memberDetails.getEmail());
        member.setPassword(memberDetails.getPassword());
        member.setNpi(memberDetails.getNpi());
        member.setPhone(memberDetails.getPhone());
        member.setRole(memberDetails.getRole());

        return memberRepository.save(member);
    }

    public void deleteMember(Long id){
        memberRepository.deleteById(id);
    }
    @Transactional
    public Member updateSubscriptionStatus(Long memberId, Boolean isRegular, LocalDate subscriptionDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        member.setIsRegular(isRegular);
        member.setLastSubscriptionDate(subscriptionDate);
        member.setSubscriptionStatus(isRegular ? "ACTIVE" : "EXPIRED");
        return memberRepository.save(member);
    }

    @Transactional
    public Member updateDebtStatus(Long memberId, Boolean hasDebt) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        member.setHasPreviousDebt(hasDebt);
        return memberRepository.save(member);
    }

    public boolean validateMemberForLoan(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        return member.canRequestLoan();
    }

    public Member getCurrentMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
    }

   public Optional<Member> getMemberByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public String getUserRole(Member member) {
        if (member.isPresident()) return "PRESIDENT";
        if (member.isSecretary()) return "SECRETARY";
        if (member.isTreasurer()) return "TREASURER";
        if (member.isAdmin()) return "ADMIN";
        return "MEMBER";
    }
}
