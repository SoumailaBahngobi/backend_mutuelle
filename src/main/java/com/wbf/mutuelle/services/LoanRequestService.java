package com.wbf.mutuelle.services;
import com.wbf.mutuelle.entities.*;
import com.wbf.mutuelle.repositories.LoanRepository;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
/*
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


        loan.setRepaymentAmount(request.getRequestAmount());
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
    }*/
    public LoanRequest rejectLoanRequest(Long id) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));
        request.setStatus("REJECTED");
        return loanRequestRepository.save(request);
    }

/// //////
 // Méthodes d'approbation avec commentaires
    @Transactional
    public LoanRequest approveByPresident(Long id, String comment) {
        return approveLoanRequest(id, "PRESIDENT", comment);
    }

    @Transactional
    public LoanRequest approveBySecretary(Long id, String comment) {
        return approveLoanRequest(id, "SECRETARY", comment);
    }

    @Transactional
    public LoanRequest approveByTreasurer(Long id, String comment) {
        return approveLoanRequest(id, "TREASURER", comment);
    }

    private LoanRequest approveLoanRequest(Long id, String role, String comment) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        // Vérifier le statut
        if ("REJECTED".equals(request.getStatus())) {
            throw new RuntimeException("Impossible d'approuver une demande rejetée");
        }

        if ("APPROVED".equals(request.getStatus())) {
            throw new RuntimeException("Cette demande est déjà approuvée");
        }

        // Vérifier le rôle de l'utilisateur
        Member currentUser = memberService.getCurrentMember(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        if (!hasRequiredRole(currentUser, role)) {
            throw new RuntimeException("Seul le " + role.toLowerCase() + " ou l'admin peut approuver");
        }

        // Vérifier si l'utilisateur n'a pas déjà approuvé
        if (hasAlreadyApproved(request, role)) {
            throw new RuntimeException("Vous avez déjà approuvé cette demande");
        }

        // Mettre à jour l'approbation
        switch (role) {
            case "PRESIDENT" -> {
                request.setPresidentApproved(true);
                request.setPresidentApprovalDate(new Date());
                request.setPresidentComment(comment);
            }
            case "SECRETARY" -> {
                request.setSecretaryApproved(true);
                request.setSecretaryApprovalDate(new Date());
                request.setSecretaryComment(comment);
            }
            case "TREASURER" -> {
                request.setTreasurerApproved(true);
                request.setTreasurerApprovalDate(new Date());
                request.setTreasurerComment(comment);
            }
        }

        return checkAndUpdateFinalStatus(request);
    }

    private boolean hasAlreadyApproved(LoanRequest request, String role) {
        return switch (role) {
            case "PRESIDENT" -> Boolean.TRUE.equals(request.getPresidentApproved());
            case "SECRETARY" -> Boolean.TRUE.equals(request.getSecretaryApproved());
            case "TREASURER" -> Boolean.TRUE.equals(request.getTreasurerApproved());
            default -> false;
        };
    }

    // Méthode de rejet améliorée
    @Transactional
    public LoanRequest rejectLoanRequest(Long id, String rejectionReason, String rejectedByRole) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        if ("APPROVED".equals(request.getStatus())) {
            throw new RuntimeException("Impossible de rejeter une demande déjà approuvée");
        }

        // Vérifier le rôle de l'utilisateur
        Member currentUser = memberService.getCurrentMember(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        if (!hasRequiredRole(currentUser, rejectedByRole)) {
            throw new RuntimeException("Vous n'avez pas les droits pour rejeter cette demande");
        }

        request.setStatus("REJECTED");
        request.setRejectionReason(rejectionReason);

        return loanRequestRepository.save(request);
    }

    // Méthode pour réinitialiser une approbation (admin seulement)
    @Transactional
    public LoanRequest resetApproval(Long id, String role) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        // Vérifier si l'utilisateur est admin
        Member currentUser = memberService.getCurrentMember(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        if (!currentUser.isAdmin()) {
            throw new RuntimeException("Seul l'administrateur peut réinitialiser les approbations");
        }

        switch (role) {
            case "PRESIDENT" -> {
                request.setPresidentApproved(false);
                request.setPresidentApprovalDate(null);
                request.setPresidentComment(null);
            }
            case "SECRETARY" -> {
                request.setSecretaryApproved(false);
                request.setSecretaryApprovalDate(null);
                request.setSecretaryComment(null);
            }
            case "TREASURER" -> {
                request.setTreasurerApproved(false);
                request.setTreasurerApprovalDate(null);
                request.setTreasurerComment(null);
            }
        }

        // Revenir au statut "IN_REVIEW" si la demande était partiellement approuvée
        if (request.hasAnyApproval()) {
            request.setStatus("IN_REVIEW");
        } else {
            request.setStatus("PENDING");
        }

        return loanRequestRepository.save(request);
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

        // Calcul intérêts
        BigDecimal interest = request.getRequestAmount()
                .multiply(request.getInterestRate())
                .divide(new BigDecimal("100"));
        loan.setRepaymentAmount(request.getRequestAmount().add(interest));
        loan.setMember(request.getMember());
        loan.setLoanRequest(request);
        loan.setIsRepaid(false);

        loanRepository.save(loan);
    }

    // Méthodes de consultation améliorées
    public List<LoanRequest> getPendingRequests() {
        return loanRequestRepository.findByStatus("PENDING");
    }

    public List<LoanRequest> getInReviewRequests() {
        return loanRequestRepository.findByStatus("IN_REVIEW");
    }

    public List<LoanRequest> getApprovedRequests() {
        return loanRequestRepository.findByStatus("APPROVED");
    }

    public List<LoanRequest> getRejectedRequests() {
        return loanRequestRepository.findByStatus("REJECTED");
    }

    // Méthodes existantes...
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

    public Map<String, Object> getValidatorDashboard(String userEmail) {
        Member currentUser = memberService.getCurrentMember(userEmail);

        List<LoanRequest> pendingRequests = getPendingRequests();
        List<LoanRequest> inReviewRequests = getInReviewRequests();
        List<LoanRequest> myPendingApprovals = getPendingApprovalsForCurrentUser(userEmail);

        return Map.of(
                "userRole", memberService.getUserRole(currentUser),
                "pendingRequestsCount", pendingRequests.size(),
                "inReviewRequestsCount", inReviewRequests.size(),
                "myPendingApprovals", myPendingApprovals,
                "statistics", getValidationStatistics()
        );
    }

    public List<LoanRequest> getPendingApprovalsForCurrentUser(String userEmail) {
        Member currentUser = memberService.getCurrentMember(userEmail);
        List<LoanRequest> allPending = getPendingRequests();
        List<LoanRequest> inReview = getInReviewRequests();

        // Fusionner les listes
        allPending.addAll(inReview);

        // Filtrer selon le rôle
        return allPending.stream()
                .filter(request -> needsApprovalFromUser(request, currentUser))
                .collect(Collectors.toList());
    }

    private boolean needsApprovalFromUser(LoanRequest request, Member user) {
        if (user.isPresident() && !Boolean.TRUE.equals(request.getPresidentApproved())) {
            return true;
        }
        if (user.isSecretary() && !Boolean.TRUE.equals(request.getSecretaryApproved())) {
            return true;
        }
        if (user.isTreasurer() && !Boolean.TRUE.equals(request.getTreasurerApproved())) {
            return true;
        }
        return false;
    }

    private Map<String, Object> getValidationStatistics() {
        List<LoanRequest> allRequests = getAllLoanRequests();

        long total = allRequests.size();
        long approved = allRequests.stream().filter(r -> "APPROVED".equals(r.getStatus())).count();
        long rejected = allRequests.stream().filter(r -> "REJECTED".equals(r.getStatus())).count();
        long pending = allRequests.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        long inReview = allRequests.stream().filter(r -> "IN_REVIEW".equals(r.getStatus())).count();

        return Map.of(
                "total", total,
                "approved", approved,
                "rejected", rejected,
                "pending", pending,
                "inReview", inReview,
                "approvalRate", total > 0 ? (double) approved / total * 100 : 0
        );
    }

}




