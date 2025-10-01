/*package com.wbf.mutuelle.services;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;

@Service
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public LoanRequestService(LoanRequestRepository loanRequestRepository, MemberRepository memberRepository) {
        this.loanRequestRepository = loanRequestRepository;
        this.memberRepository = memberRepository;
    }

    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestRepository.findAll();
    }

    public Optional<LoanRequest> getLoanRequestById(Long id) {
        return loanRequestRepository.findById(id);
    }

    public List<LoanRequest> getLoanRequestsByMemberId(Long memberId) {
        return loanRequestRepository.findByMemberId(memberId);
    }

    public LoanRequest createLoanRequest(LoanRequest loanRequest, String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé !"));

        // Assurez-vous que l'ID est null pour que la base de données le génère automatiquement
       // loanRequest.setId(null);

        System.out.println("ID avant set null: " + loanRequest.getId()); // Debug
        loanRequest.setId(null);
        System.out.println("ID après set null: " + loanRequest.getId());


if (loanRequest.getIs_repaid() == null )  {
    loanRequest.setIs_repaid(false);

}

        return loanRequestRepository.save(loanRequest);
    }

    public LoanRequest updateLoanRequest(Long id, LoanRequest updatedRequest) {
        return loanRequestRepository.findById(id).map(existingRequest -> {
            existingRequest.setRequest_amount(updatedRequest.getRequest_amount());
            existingRequest.setDuration(updatedRequest.getDuration());
            existingRequest.setReason(updatedRequest.getReason());
            existingRequest.setStatus(updatedRequest.getStatus());
            existingRequest.setIs_repaid(updatedRequest.getIs_repaid());
           // existingRequest.setIsRepaid(updatedRequest.getIsRepaid());
            return loanRequestRepository.save(existingRequest);
        }).orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée avec l'id " + id));
    }

    public void deleteLoanRequest(Long id) {
        loanRequestRepository.deleteById(id);
    }

    public LoanRequest approveLoanRequest(Long id) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée avec l'id " + id));

        request.setStatus("APPROVED");
        return loanRequestRepository.save(request);
    }

    public LoanRequest rejectLoanRequest(Long id) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée avec l'id " + id));

        request.setStatus("REJECTED");
        return loanRequestRepository.save(request);
    }
}*/

package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.*;
import com.wbf.mutuelle.repositories.LoanRepository;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final MemberService memberService;

    public LoanRequestService(LoanRequestRepository loanRequestRepository,
                              MemberRepository memberRepository,
                              LoanRepository loanRepository,
                              MemberService memberService) {
        this.loanRequestRepository = loanRequestRepository;
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
        this.memberService = memberService;
    }

    @Transactional
    public LoanRequest createLoanRequest(LoanRequest loanRequest, String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé !"));

        validateLoanRequest(member, loanRequest);

        // Initialisation
        loanRequest.setId(null);
        loanRequest.setMember(member);
        loanRequest.setRequestDate(new Date());
        loanRequest.setStatus("PENDING");
        loanRequest.setIsRepaid(false);
        loanRequest.setPresidentApproved(false);
        loanRequest.setSecretaryApproved(false);
        loanRequest.setTreasurerApproved(false);

        if (loanRequest.getInterestRate() == null) {
            loanRequest.setInterestRate(new BigDecimal("5.0"));
        }

        return loanRequestRepository.save(loanRequest);
    }

    private void validateLoanRequest(Member member, LoanRequest loanRequest) {
        // 1. Vérifier la régularité des cotisations
        /*
        if (!member.isSubscriptionActive()) {
            throw new RuntimeException("Le membre n'est pas régulier pour les cotisations");
        }

        // 2. Vérifier les dettes antérieures

        if (Boolean.TRUE.equals(member.getHasPreviousDebt())) {
            throw new RuntimeException("Le membre a des dettes antérieures");
        }

        // 3. Vérifier les prêts en cours
        if (loanRepository.existsByMemberAndIsRepaidFalse(member)) {
            throw new RuntimeException("Le membre a déjà un prêt en cours non remboursé");
        }*/

        // 4. Accepter les termes avec intérêts
        if (!Boolean.TRUE.equals(loanRequest.getAcceptTerms())) {
            throw new RuntimeException("Vous devez accepter les termes de remboursement avec intérêts de 5%");
        }

        // 5. Validation du montant et durée
        if (loanRequest.getRequestAmount() == null ||
                loanRequest.getRequestAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant du prêt doit être positif");
        }

        if (loanRequest.getDuration() == null || loanRequest.getDuration() <= 0) {
            throw new RuntimeException("La durée du prêt doit être positive");
        }
    }

    // Méthodes d'approbation
    @Transactional
    public LoanRequest approveByPresident(Long id) {
        return approveLoanRequest(id, "PRESIDENT");
    }

    @Transactional
    public LoanRequest approveBySecretary(Long id) {
        return approveLoanRequest(id, "SECRETARY");
    }

    @Transactional
    public LoanRequest approveByTreasurer(Long id) {
        return approveLoanRequest(id, "TREASURER");
    }

    private LoanRequest approveLoanRequest(Long id, String role) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        // Vérifier le statut
        if (!"PENDING".equals(request.getStatus()) && !"IN_REVIEW".equals(request.getStatus())) {
            throw new RuntimeException("Cette demande ne peut plus être modifiée");
        }

        // Vérifier le rôle de l'utilisateur
        Member currentUser = memberService.getCurrentMember(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        if (!hasRequiredRole(currentUser, role)) {
            throw new RuntimeException("Seul le " + role.toLowerCase() + " ou l'admin peut approuver");
        }

        // Mettre à jour l'approbation
        switch (role) {
            case "PRESIDENT" -> request.setPresidentApproved(true);
            case "SECRETARY" -> request.setSecretaryApproved(true);
            case "TREASURER" -> request.setTreasurerApproved(true);
        }

        return checkAndUpdateFinalStatus(request);
    }

    private boolean hasRequiredRole(Member member, String requiredRole) {
        return switch (requiredRole) {
            case "PRESIDENT" -> member.isPresident() || member.isAdmin();
            case "SECRETARY" -> member.isSecretary() || member.isAdmin();
            case "TREASURER" -> member.isTreasurer() || member.isAdmin();
            default -> false;
        };
    }

    private LoanRequest checkAndUpdateFinalStatus(LoanRequest request) {
        if (request.isFullyApproved()) {
            request.setStatus("APPROVED");
            createLoanFromRequest(request);
        } else {
            request.setStatus("IN_REVIEW");
        }
        return loanRequestRepository.save(request);
    }

    @Transactional
    private void createLoanFromRequest(LoanRequest request) {
        Loan loan = new Loan();
        loan.setAmount(request.getRequestAmount());
        loan.setDuration(request.getDuration());
        loan.setBeginDate(new Date());

        // Calcul date de fin
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, request.getDuration());
        loan.setEndDate(calendar.getTime());

        // Calcul intérêts 5%
        BigDecimal interest = request.getRequestAmount()
                .multiply(request.getInterestRate())
                .divide(new BigDecimal("100"));
        loan.setRepaymentAmount(request.getRequestAmount().add(interest));
        loan.setInterestRate(request.getInterestRate());
        loan.setMember(request.getMember());
        loan.setLoanRequest(request);
        loan.setIsRepaid(false);

        loanRepository.save(loan);
    }

    // Méthodes de consultation
    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestRepository.findAll();
    }

    public Optional<LoanRequest> getLoanRequestById(Long id) {
        return loanRequestRepository.findById(id);
    }

    public List<LoanRequest> getLoanRequestsByMemberId(Long memberId) {
        return loanRequestRepository.findByMemberId(memberId);
    }

    public List<LoanRequest> getLoanRequestsByMemberEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        return loanRequestRepository.findByMember(member);
    }

    public LoanRequest rejectLoanRequest(Long id) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));
        request.setStatus("REJECTED");
        return loanRequestRepository.save(request);
    }
}