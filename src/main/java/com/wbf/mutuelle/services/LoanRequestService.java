package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.*;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final LoanAutoCreationService loanAutoCreationService;
    private final TreasurerLoanService treasurerLoanService;
    private final EmailService emailService; // ← AJOUTÉ

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<LoanRequest> getLoanRequestById(Long id) {
        return loanRequestRepository.findById(id);
    }

    public List<LoanRequest> getLoanRequestsByMemberId(Long memberId) {
        return loanRequestRepository.findByMemberId(memberId);
    }

    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestRepository.findAllWithMember();
    }

    @Transactional
    public LoanRequest createLoanRequest(LoanRequest loanRequest, String userIdentifier, String keycloakId) {
        // RECHERCHE DU MEMBRE : D'abord par keycloakId, puis par email
        Member member = null;

        // Essayer de trouver par keycloakId d'abord
        if (keycloakId != null && !keycloakId.isEmpty()) {
            Optional<Member> memberByKeycloakId = memberRepository.findByKeycloakId(keycloakId);
            if (memberByKeycloakId.isPresent()) {
                member = memberByKeycloakId.get();
                System.out.println(" Membre trouvé par keycloakId: " + member.getId() + " - " + member.getEmail());
            }
        }

        // Si pas trouvé par keycloakId, essayer par email
        if (member == null && userIdentifier != null && !userIdentifier.isEmpty()) {
            Optional<Member> memberByEmail = memberRepository.findByEmail(userIdentifier);
            if (memberByEmail.isPresent()) {
                member = memberByEmail.get();
                System.out.println(" Membre trouvé par email: " + member.getId() + " - " + member.getEmail());
            }
        }

        // Si toujours pas trouvé, erreur
        if (member == null) {
            System.err.println(" Membre non trouvé - keycloakId: " + keycloakId + ", email: " + userIdentifier);
            throw new RuntimeException("Membre non trouvé. Veuillez lier votre compte Keycloak à un membre existant.");
        }

        loanRequest.setMember(member);
        loanRequest.setStatus("PENDING");
        loanRequest.setRequestDate(new Date());
        loanRequest.setLoanCreated(false);
        loanRequest.setLoanGranted(false);

        // Utilisation de entityManager.persist() pour sauvegarder
        entityManager.persist(loanRequest);

        System.out.println(" Demande de prêt créée avec succès pour le membre: " + member.getEmail());

        // ==================== ENVOI D'EMAIL DE CONFIRMATION ====================
        if (member != null && member.getEmail() != null) {
            try {
                emailService.sendLoanRequestConfirmation(
                        member.getEmail(),
                        member.getFirstName() + " " + member.getName(),
                        loanRequest.getRequestAmount().doubleValue(),
                        loanRequest.getReason(),
                        loanRequest.getId() != null ? loanRequest.getId().toString() : "En cours"
                );
                log.info("Email de confirmation de demande de prêt envoyé à {}", member.getEmail());
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'email de confirmation de demande de prêt: {}", e.getMessage());
                // Ne pas bloquer l'opération principale
            }
        }

        return loanRequest;
    }

    @Transactional
    public LoanRequest approveByPresident(Long loanRequestId, String comment) {
        return approveLoanRequest(loanRequestId, Role.PRESIDENT, comment);
    }

    @Transactional
    public LoanRequest approveBySecretary(Long loanRequestId, String comment) {
        return approveLoanRequest(loanRequestId, Role.SECRETARY, comment);
    }

    @Transactional
    public LoanRequest approveByTreasurer(Long loanRequestId, String comment) {
        return approveLoanRequest(loanRequestId, Role.TREASURER, comment);
    }

    private LoanRequest approveLoanRequest(Long loanRequestId, Role role, String comment) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        if (!"PENDING".equals(loanRequest.getStatus()) && !"IN_REVIEW".equals(loanRequest.getStatus())) {
            throw new RuntimeException("La demande ne peut pas être approuvée dans son état actuel: " + loanRequest.getStatus());
        }

        switch (role) {
            case PRESIDENT:
                loanRequest.setPresidentApproved(true);
                loanRequest.setPresidentApprovalDate(new Date());
                loanRequest.setPresidentComment(comment);
                break;
            case SECRETARY:
                loanRequest.setSecretaryApproved(true);
                loanRequest.setSecretaryApprovalDate(new Date());
                loanRequest.setSecretaryComment(comment);
                break;
            case TREASURER:
                loanRequest.setTreasurerApproved(true);
                loanRequest.setTreasurerApprovalDate(new Date());
                loanRequest.setTreasurerComment(comment);
                break;
            default:
                throw new RuntimeException("Rôle non autorisé pour l'approbation");
        }

        boolean wasApproved = loanRequest.isFullyApproved();

        if (loanRequest.isFullyApproved()) {
            loanRequest.setStatus("APPROVED");
        } else {
            loanRequest.setStatus("IN_REVIEW");
        }

        LoanRequest savedRequest = loanRequestRepository.save(loanRequest);

        // ==================== ENVOI D'EMAIL D'APPROBATION ====================
        if (savedRequest.getStatus().equals("APPROVED") && savedRequest.getMember() != null && savedRequest.getMember().getEmail() != null) {
            try {
                emailService.sendLoanApprovalEmail(
                        savedRequest.getMember().getEmail(),
                        savedRequest.getMember().getFirstName() + " " + savedRequest.getMember().getName(),
                        savedRequest.getRequestAmount().doubleValue(),
                        new Date().toString()
                );
                log.info("Email d'approbation de prêt envoyé à {}", savedRequest.getMember().getEmail());
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'email d'approbation: {}", e.getMessage());
            }
        }

        return savedRequest;
    }

    @Transactional
    public LoanRequest rejectLoanRequest(Long loanRequestId, String rejectionReason, String rejectedByRole) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        if ("APPROVED".equals(loanRequest.getStatus()) || "REJECTED".equals(loanRequest.getStatus())) {
            throw new RuntimeException("La demande ne peut pas être rejetée dans son état actuel");
        }

        loanRequest.setStatus("REJECTED");
        loanRequest.setRejectionReason(rejectionReason);

        LoanRequest savedRequest = loanRequestRepository.save(loanRequest);

        // ==================== ENVOI D'EMAIL DE REJET ====================
        if (savedRequest.getMember() != null && savedRequest.getMember().getEmail() != null) {
            try {
                emailService.sendLoanRejectionEmail(
                        savedRequest.getMember().getEmail(),
                        savedRequest.getMember().getFirstName() + " " + savedRequest.getMember().getName(),
                        savedRequest.getRequestAmount().doubleValue(),
                        rejectionReason
                );
                log.info("Email de rejet de prêt envoyé à {}", savedRequest.getMember().getEmail());
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'email de rejet: {}", e.getMessage());
            }
        }

        return savedRequest;
    }

    @Transactional
    public LoanRequest resetApproval(Long loanRequestId, String role) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        switch (role.toUpperCase()) {
            case "PRESIDENT":
                loanRequest.setPresidentApproved(false);
                loanRequest.setPresidentApprovalDate(null);
                loanRequest.setPresidentComment(null);
                break;
            case "SECRETARY":
                loanRequest.setSecretaryApproved(false);
                loanRequest.setSecretaryApprovalDate(null);
                loanRequest.setSecretaryComment(null);
                break;
            case "TREASURER":
                loanRequest.setTreasurerApproved(false);
                loanRequest.setTreasurerApprovalDate(null);
                loanRequest.setTreasurerComment(null);
                break;
            default:
                throw new RuntimeException("Rôle invalide");
        }

        if (!"REJECTED".equals(loanRequest.getStatus())) {
            loanRequest.setStatus("IN_REVIEW");
        }

        loanRequest.setLoanCreated(false);
        loanRequest.setLoanGranted(false);

        return loanRequestRepository.save(loanRequest);
    }

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

    public Map<String, Object> getValidatorDashboard(String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("totalRequests", loanRequestRepository.count());
        dashboard.put("pendingRequests", getPendingRequests().size());
        dashboard.put("inReviewRequests", getInReviewRequests().size());
        dashboard.put("approvedRequests", getApprovedRequests().size());
        dashboard.put("rejectedRequests", getRejectedRequests().size());

        if (member.isPresident()) {
            dashboard.put("myPendingApprovals",
                    loanRequestRepository.findByPresidentApproved(false).stream()
                            .filter(lr -> "PENDING".equals(lr.getStatus()) || "IN_REVIEW".equals(lr.getStatus()))
                            .collect(Collectors.toList())
            );
        } else if (member.isSecretary()) {
            dashboard.put("myPendingApprovals",
                    loanRequestRepository.findBySecretaryApproved(false).stream()
                            .filter(lr -> "PENDING".equals(lr.getStatus()) || "IN_REVIEW".equals(lr.getStatus()))
                            .collect(Collectors.toList())
            );
        } else if (member.isTreasurer()) {
            dashboard.put("myPendingApprovals",
                    loanRequestRepository.findByTreasurerApproved(false).stream()
                            .filter(lr -> "PENDING".equals(lr.getStatus()) || "IN_REVIEW".equals(lr.getStatus()))
                            .collect(Collectors.toList())
            );
        }

        return dashboard;
    }

    public List<LoanRequest> getPendingApprovalsForCurrentUser(String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        List<LoanRequest> allPending = loanRequestRepository.findByStatus("PENDING");
        allPending.addAll(loanRequestRepository.findByStatus("IN_REVIEW"));

        return allPending.stream()
                .filter(loanRequest -> {
                    if (member.isPresident() && !loanRequest.getPresidentApproved()) return true;
                    if (member.isSecretary() && !loanRequest.getSecretaryApproved()) return true;
                    if (member.isTreasurer() && !loanRequest.getTreasurerApproved()) return true;
                    return false;
                })
                .collect(Collectors.toList());
    }

    private LoanRequest enrichWithApprovalDetails(LoanRequest loanRequest) {
        loanRequest.setApprovalProgress(calculateApprovalProgress(loanRequest));
        return loanRequest;
    }

    private Map<String, Object> calculateApprovalProgress(LoanRequest loanRequest) {
        Map<String, Object> progress = new HashMap<>();

        progress.put("presidentApproved", loanRequest.getPresidentApproved());
        progress.put("secretaryApproved", loanRequest.getSecretaryApproved());
        progress.put("treasurerApproved", loanRequest.getTreasurerApproved());

        int approvedCount = 0;
        if (loanRequest.getPresidentApproved()) approvedCount++;
        if (loanRequest.getSecretaryApproved()) approvedCount++;
        if (loanRequest.getTreasurerApproved()) approvedCount++;

        progress.put("approvalPercentage", (approvedCount * 100) / 3);
        progress.put("approvedCount", approvedCount);
        progress.put("totalApprovers", 3);

        List<String> pendingApprovers = new ArrayList<>();
        if (!loanRequest.getPresidentApproved()) pendingApprovers.add("PRESIDENT");
        if (!loanRequest.getSecretaryApproved()) pendingApprovers.add("SECRETARY");
        if (!loanRequest.getTreasurerApproved()) pendingApprovers.add("TREASURER");

        progress.put("pendingApprovers", pendingApprovers);
        progress.put("nextApprover", pendingApprovers.isEmpty() ? "COMPLETED" : pendingApprovers.get(0));

        return progress;
    }

    public Map<String, Object> getLoanRequestApprovalStatus(Long loanRequestId) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        Map<String, Object> status = new HashMap<>();
        status.put("loanRequest", loanRequest);
        status.put("approvalProgress", calculateApprovalProgress(loanRequest));
        status.put("currentStatus", loanRequest.getStatus());

        Map<String, Object> approvalDetails = new HashMap<>();
        approvalDetails.put("president", Map.of(
                "approved", loanRequest.getPresidentApproved(),
                "approvalDate", loanRequest.getPresidentApprovalDate(),
                "comment", loanRequest.getPresidentComment()
        ));
        approvalDetails.put("secretary", Map.of(
                "approved", loanRequest.getSecretaryApproved(),
                "approvalDate", loanRequest.getSecretaryApprovalDate(),
                "comment", loanRequest.getSecretaryComment()
        ));
        approvalDetails.put("treasurer", Map.of(
                "approved", loanRequest.getTreasurerApproved(),
                "approvalDate", loanRequest.getTreasurerApprovalDate(),
                "comment", loanRequest.getTreasurerComment()
        ));

        status.put("approvalDetails", approvalDetails);
        return status;
    }

    public List<LoanRequest> getAllLoanRequestsWithFilters(String status, Long memberId) {
        if (status != null && memberId != null) {
            return loanRequestRepository.findAllWithFilters(status, memberId);
        } else if (status != null) {
            return loanRequestRepository.findByStatus(status);
        } else if (memberId != null) {
            return loanRequestRepository.findByMemberId(memberId);
        } else {
            return loanRequestRepository.findAll();
        }
    }

    public Map<String, Object> getCompleteValidatorDashboard(String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalRequests", loanRequestRepository.count());
        dashboard.put("pendingRequests", getPendingRequests().size());
        dashboard.put("inReviewRequests", getInReviewRequests().size());
        dashboard.put("approvedRequests", getApprovedRequests().size());
        dashboard.put("rejectedRequests", getRejectedRequests().size());
        dashboard.put("allRequests", getAllLoanRequestsWithApprovalDetails());
        dashboard.put("myPendingApprovals", getPendingApprovalsForCurrentUser(userEmail));
        dashboard.put("pendingList", getPendingRequests());
        dashboard.put("inReviewList", getInReviewRequests());
        dashboard.put("approvedList", getApprovedRequests());
        dashboard.put("rejectedList", getRejectedRequests());

        return dashboard;
    }

    public List<LoanRequest> getValidationHistoryByUser(String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        return loanRequestRepository.findAll().stream()
                .filter(request -> {
                    if (member.isPresident() && request.getPresidentApprovalDate() != null) return true;
                    if (member.isSecretary() && request.getSecretaryApprovalDate() != null) return true;
                    if (member.isTreasurer() && request.getTreasurerApprovalDate() != null) return true;
                    return false;
                })
                .sorted((r1, r2) -> {
                    Date date1 = getLatestApprovalDate(r1, member);
                    Date date2 = getLatestApprovalDate(r2, member);
                    return date2.compareTo(date1);
                })
                .collect(Collectors.toList());
    }

    private Date getLatestApprovalDate(LoanRequest request, Member member) {
        if (member.isPresident() && request.getPresidentApprovalDate() != null) {
            return request.getPresidentApprovalDate();
        }
        if (member.isSecretary() && request.getSecretaryApprovalDate() != null) {
            return request.getSecretaryApprovalDate();
        }
        if (member.isTreasurer() && request.getTreasurerApprovalDate() != null) {
            return request.getTreasurerApprovalDate();
        }
        return new Date(0);
    }

    public List<LoanRequest> getApprovedLoans() {
        return loanRequestRepository.findByStatusAndIsRepaid("APPROVED", false);
    }

    public List<Repayment> getRepaymentsByLoanRequest(Long id) {
        LoanRequest loanRequest = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));
        return loanRequest.getRepayments();
    }

    public void generateRepaymentScheduleForLoanRequest(Long id) {
        LoanRequest loanRequest = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        List<Repayment> repayments = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        for (int i = 1; i <= loanRequest.getDuration(); i++) {
            calendar.add(Calendar.MONTH, 1);
            Repayment repayment = new Repayment();
            repayment.setLoanRequest(loanRequest);
            repayment.setDueDate(calendar.getTime());
            repayments.add(repayment);
        }

        loanRequest.setRepayments(repayments);
        loanRequestRepository.save(loanRequest);
    }

    @Transactional
    public void forceCreateLoanFromRequest(Long loanRequestId) {
        loanAutoCreationService.forceCreateLoan(loanRequestId);
    }

    public List<LoanRequest> getApprovedPendingGrant() {
        return treasurerLoanService.getApprovedPendingGrant();
    }

    public List<Loan> getGrantedLoans() {
        return treasurerLoanService.getGrantedLoans();
    }

    @Transactional
    public Loan grantLoan(Long loanRequestId, String comment) {
        return treasurerLoanService.grantApprovedLoan(loanRequestId, comment);
    }

    @Transactional
    public void cancelLoanGrant(Long loanRequestId, String reason) {
        treasurerLoanService.cancelLoanGrant(loanRequestId, reason);
    }

    public List<LoanRequest> getLoanRequestsByMemberEmail(String email) {
        log.info("🔍 Récupération des demandes de prêt pour l'email: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                log.error("❌ Email est null ou vide");
                return new ArrayList<>();
            }

            Optional<Member> memberOpt = memberRepository.findByEmail(email);

            if (memberOpt.isEmpty()) {
                log.error("❌ Membre non trouvé avec l'email: {}", email);
                return new ArrayList<>();
            }

            Member member = memberOpt.get();
            log.info("✅ Membre trouvé: ID={}", member.getId());

            List<LoanRequest> requests = loanRequestRepository.findByMemberIdWithDetails(member.getId());

            log.info("📊 {} demande(s) trouvée(s)", requests.size());
            return requests;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des demandes pour {}", email, e);
            return new ArrayList<>();
        }
    }

    public List<LoanRequest> getAllLoanRequestsWithApprovalDetails() {
        List<LoanRequest> requests = loanRequestRepository.findAllWithAllDetails();
        return requests.stream()
                .map(this::enrichWithApprovalDetails)
                .collect(Collectors.toList());
    }
}