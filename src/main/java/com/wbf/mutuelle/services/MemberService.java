package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final LoanRequestRepository loanRequestRepository;

    public MemberService(MemberRepository memberRepository, LoanRequestRepository loanRequestRepository) {
        this.memberRepository = memberRepository;
        this.loanRequestRepository = loanRequestRepository;
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
        System.out.println("🔍 [DEBUG] validateMemberForLoan appelé pour memberId: " + memberId);

        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            System.out.println("❌ [DEBUG] Membre non trouvé: " + memberId);
            return false;
        }

        Member member = memberOpt.get();
        System.out.println("👤 [DEBUG] Membre trouvé: " + member.getEmail() + " (ID: " + member.getId() + ")");

        // APPROCHE ALTERNATIVE : Récupérer toutes les demandes et filtrer manuellement
        List<LoanRequest> allMemberRequests = loanRequestRepository.findByMemberId(memberId);
        System.out.println("📋 [DEBUG] Total demandes du membre: " + allMemberRequests.size());

        List<LoanRequest> pendingRequests = allMemberRequests.stream()
                .filter(request -> "PENDING".equals(request.getStatus()) || "IN_REVIEW".equals(request.getStatus()))
                .collect(Collectors.toList());

        System.out.println("⏳ [DEBUG] Demandes PENDING/IN_REVIEW: " + pendingRequests.size());

        for (LoanRequest request : pendingRequests) {
            System.out.println("   - Demande ID: " + request.getId() +
                    ", Statut: " + request.getStatus() +
                    ", Montant: " + request.getRequestAmount());
        }

        boolean isEligible = pendingRequests.isEmpty();
        System.out.println("✅ [DEBUG] Résultat éligibilité: " + isEligible);

        return isEligible;
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