package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.LoanRequest;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.entities.Role;
import com.wbf.mutuelle.repositories.LoanRequestRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

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
        return loanRequestRepository.findByMemberEmail(email);
    }

    @Transactional
    public LoanRequest createLoanRequest(LoanRequest loanRequest, String memberEmail) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        // CORRECTION : Utiliser MemberService pour vérifier l'éligibilité
        if (!memberService.validateMemberForLoan(member.getId())) {
            throw new RuntimeException("Vous avez déjà une ou plusieurs demandes de prêt en attente de validation. Veuillez attendre leur traitement avant de soumettre une nouvelle demande.");
        }

        loanRequest.setMember(member);
        loanRequest.setStatus("PENDING");
        loanRequest.setRequestDate(new Date());

        return loanRequestRepository.save(loanRequest);
    }

    // Méthodes d'approbation par rôle
    @Transactional
    public LoanRequest approveByPresident(Long loanRequestId) {
        return approveLoanRequest(loanRequestId, Role.PRESIDENT);
    }

    @Transactional
    public LoanRequest approveBySecretary(Long loanRequestId) {
        return approveLoanRequest(loanRequestId, Role.SECRETARY);
    }

    @Transactional
    public LoanRequest approveByTreasurer(Long loanRequestId) {
        return approveLoanRequest(loanRequestId, Role.TREASURER);
    }

    private LoanRequest approveLoanRequest(Long loanRequestId, Role role) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        // Vérifier que la demande est en attente
        if (!"PENDING".equals(loanRequest.getStatus()) && !"IN_REVIEW".equals(loanRequest.getStatus())) {
            throw new RuntimeException("La demande ne peut pas être approuvée dans son état actuel");
        }

        // Mettre à jour l'approbation selon le rôle
        switch (role) {
            case PRESIDENT:
                loanRequest.setPresidentApproved(true);
                loanRequest.setPresidentApprovalDate(new Date());
                break;
            case SECRETARY:
                loanRequest.setSecretaryApproved(true);
                loanRequest.setSecretaryApprovalDate(new Date());
                break;
            case TREASURER:
                loanRequest.setTreasurerApproved(true);
                loanRequest.setTreasurerApprovalDate(new Date());
                break;
            default:
                throw new RuntimeException("Rôle non autorisé pour l'approbation");
        }

        // Mettre à jour le statut
        if (loanRequest.isFullyApproved()) {
            loanRequest.setStatus("APPROVED");
        } else {
            loanRequest.setStatus("IN_REVIEW");
        }

        return loanRequestRepository.save(loanRequest);
    }

    // Méthode de rejet
    @Transactional
    public LoanRequest rejectLoanRequest(Long loanRequestId, String rejectionReason, String rejectedByRole) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée"));

        // Vérifier que la demande peut être rejetée
        if ("APPROVED".equals(loanRequest.getStatus()) || "REJECTED".equals(loanRequest.getStatus())) {
            throw new RuntimeException("La demande ne peut pas être rejetée dans son état actuel");
        }

        loanRequest.setStatus("REJECTED");
        loanRequest.setRejectionReason(rejectionReason);

        return loanRequestRepository.save(loanRequest);
    }

    // Réinitialiser une approbation (admin seulement)
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

        // Revenir au statut IN_REVIEW si ce n'est pas déjà REJECTED
        if (!"REJECTED".equals(loanRequest.getStatus())) {
            loanRequest.setStatus("IN_REVIEW");
        }

        return loanRequestRepository.save(loanRequest);
    }

    // Méthodes de consultation par statut
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

    // Tableau de bord pour les validateurs
    public Map<String, Object> getValidatorDashboard(String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        Map<String, Object> dashboard = new HashMap<>();

        // Statistiques générales
        dashboard.put("totalRequests", loanRequestRepository.count());
        dashboard.put("pendingRequests", getPendingRequests().size());
        dashboard.put("inReviewRequests", getInReviewRequests().size());
        dashboard.put("approvedRequests", getApprovedRequests().size());
        dashboard.put("rejectedRequests", getRejectedRequests().size());

        // Demandes selon le rôle de l'utilisateur
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

    // Demandes en attente de validation par l'utilisateur courant
    public List<LoanRequest> getPendingApprovalsForCurrentUser(String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        List<LoanRequest> allPending = loanRequestRepository.findByStatus("PENDING");
        allPending.addAll(loanRequestRepository.findByStatus("IN_REVIEW"));

        return allPending.stream()
                .filter(loanRequest -> {
                    if (member.isPresident() && !loanRequest.getPresidentApproved()) {
                        return true;
                    }
                    if (member.isSecretary() && !loanRequest.getSecretaryApproved()) {
                        return true;
                    }
                    if (member.isTreasurer() && !loanRequest.getTreasurerApproved()) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
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

        // Vérifier que la demande est en attente
        if (!"PENDING".equals(loanRequest.getStatus()) && !"IN_REVIEW".equals(loanRequest.getStatus())) {
            throw new RuntimeException("La demande ne peut pas être approuvée dans son état actuel: " + loanRequest.getStatus());
        }

        // Mettre à jour l'approbation selon le rôle
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

        // Mettre à jour le statut
        if (loanRequest.isFullyApproved()) {
            loanRequest.setStatus("APPROVED");
        } else {
            loanRequest.setStatus("IN_REVIEW");
        }

        return loanRequestRepository.save(loanRequest);
    }

    // NOUVELLES METHODES POUR LES RESPONSABLES
    public List<LoanRequest> getAllLoanRequestsWithApprovalDetails() {
        return loanRequestRepository.findAll().stream()
                .map(this::enrichWithApprovalDetails)
                .collect(Collectors.toList());
    }

    private LoanRequest enrichWithApprovalDetails(LoanRequest loanRequest) {
        // Ajouter des informations calculées sur l'état d'approbation
        loanRequest.setApprovalProgress(calculateApprovalProgress(loanRequest));
        return loanRequest;
    }

    private Map<String, Object> calculateApprovalProgress(LoanRequest loanRequest) {
        Map<String, Object> progress = new HashMap<>();

        // Statut des approbations
        progress.put("presidentApproved", loanRequest.getPresidentApproved());
        progress.put("secretaryApproved", loanRequest.getSecretaryApproved());
        progress.put("treasurerApproved", loanRequest.getTreasurerApproved());

        // Pourcentage d'approbation
        int approvedCount = 0;
        if (loanRequest.getPresidentApproved()) approvedCount++;
        if (loanRequest.getSecretaryApproved()) approvedCount++;
        if (loanRequest.getTreasurerApproved()) approvedCount++;

        progress.put("approvalPercentage", (approvedCount * 100) / 3);
        progress.put("approvedCount", approvedCount);
        progress.put("totalApprovers", 3);

        // Prochain approbateur requis
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

        // Détails des approbations
        Map<String, Object> approvalDetails = new HashMap<>();

        // Président
        approvalDetails.put("president", Map.of(
                "approved", loanRequest.getPresidentApproved(),
                "approvalDate", loanRequest.getPresidentApprovalDate(),
                "comment", loanRequest.getPresidentComment()
        ));

        // Secrétaire
        approvalDetails.put("secretary", Map.of(
                "approved", loanRequest.getSecretaryApproved(),
                "approvalDate", loanRequest.getSecretaryApprovalDate(),
                "comment", loanRequest.getSecretaryComment()
        ));

        // Trésorier
        approvalDetails.put("treasurer", Map.of(
                "approved", loanRequest.getTreasurerApproved(),
                "approvalDate", loanRequest.getTreasurerApprovalDate(),
                "comment", loanRequest.getTreasurerComment()
        ));

        status.put("approvalDetails", approvalDetails);

        return status;
    }
}