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
        try {
            System.out.println("=== DÉBUT CRÉATION DEMANDE PRÊT ===");
            System.out.println("Email utilisateur: " + userEmail);
            System.out.println("Données reçues: " + loanRequest.toString());

            Member member = memberRepository.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        System.out.println("❌ Membre non trouvé pour email: " + userEmail);
                        return new RuntimeException("Membre non trouvé !");
                    });

            System.out.println("✅ Membre trouvé: " + member.getEmail());

            validateLoanRequest(member, loanRequest);
            System.out.println("✅ Validation passée");

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

            LoanRequest saved = loanRequestRepository.save(loanRequest);
            System.out.println("✅ Demande sauvegardée avec ID: " + saved.getId());
            System.out.println("=== FIN CRÉATION DEMANDE PRÊT ===");

            return saved;

        } catch (Exception e) {
            System.out.println("❌ ERREUR dans createLoanRequest: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la création de la demande: " + e.getMessage());
        }
    }


    private void validateLoanRequest(Member member, LoanRequest loanRequest) {
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
    protected void createLoanFromRequest(LoanRequest request) {
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