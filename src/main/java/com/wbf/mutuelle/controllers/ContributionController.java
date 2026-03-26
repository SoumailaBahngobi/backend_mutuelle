package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.*;
import com.wbf.mutuelle.repositories.ContributionPeriodRepository;
import com.wbf.mutuelle.repositories.ContributionRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.repositories.PaymentRepository;
import com.wbf.mutuelle.services.ContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mutuelle/contribution")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ContributionController {

    private final ContributionService contributionService;
    private final MemberRepository memberRepository;
    private final ContributionPeriodRepository contributionPeriodRepository;
    private final PaymentRepository paymentRepository;
    private final String UPLOAD_DIR = "./uploads/payment-proofs/";

    private final ContributionRepository contributionRepository;

    // =============================================
    // ENDPOINTS D'UPLOAD DE FICHIERS
    // =============================================

    @PostMapping("/upload/payment-proof")
    public ResponseEntity<?> uploadPaymentProof(@RequestParam("file") MultipartFile file) {
        try {
            log.info("=== DÉBUT UPLOAD ===");
            log.info("Nom: " + file.getOriginalFilename());
            log.info("Taille: " + file.getSize());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier est vide");
            }

            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest().body("Type de fichier non supporté. Formats acceptés: images et PDF");
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Le fichier est trop volumineux. Taille maximale: 5MB");
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("=== UPLOAD RÉUSSI: " + fileName + " ===");
            return ResponseEntity.ok(fileName);

        } catch (Exception e) {
            log.error("=== ERREUR UPLOAD ===", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    @GetMapping("/upload/payment-proof/{filename}")
    public ResponseEntity<byte[]> getPaymentProof(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =============================================
    // ENDPOINTS CRUD COTISATIONS
    // =============================================

    @GetMapping
    public ResponseEntity<List<Contribution>> getAllContributions() {
        try {
            List<Contribution> contributions = contributionService.getAllContributions();
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            log.error("Erreur getAllContributions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contribution> getContributionById(@PathVariable Long id) {
        try {
            Contribution contribution = contributionService.getContributionById(id);
            return ResponseEntity.ok(contribution);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =============================================
    // ENDPOINT POUR LE RÉSUMÉ DES COTISATIONS (DASHBOARD)
    // =============================================

    @GetMapping("/summary")
    public ResponseEntity<?> getContributionSummary(@AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("📊 Récupération du résumé des cotisations pour le dashboard");

            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email non trouvé dans le token"));
            }

            String finalEmail = email;
            Member connectedMember = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            log.info("👤 Membre connecté: {} {}", connectedMember.getFirstName(), connectedMember.getName());

            BigDecimal totalAmount = contributionService.getTotalContributionsAmountByMember(connectedMember.getId());
            Long totalCount = contributionService.getContributionsCountByMember(connectedMember.getId());
            List<Contribution> recentContributions = contributionService.getRecentContributionsByMember(connectedMember.getId(), 5);
            //List<Object[]> contributionsByPeriod = contributionRepository.findAmountByPeriodForMember(connectedMember.getId());
            List<Object[]> contributionsByPeriod = contributionRepository.findAmountByPeriodForMember(connectedMember.getId());

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
            summary.put("totalCount", totalCount != null ? totalCount : 0L);
            summary.put("recentContributions", recentContributions != null ? recentContributions : List.of());
            summary.put("contributionsByPeriod", contributionsByPeriod != null ? contributionsByPeriod : List.of());
            summary.put("memberName", (connectedMember.getFirstName() != null ? connectedMember.getFirstName() : "") + " " +
                    (connectedMember.getName() != null ? connectedMember.getName() : ""));
            summary.put("memberEmail", connectedMember.getEmail() != null ? connectedMember.getEmail() : "");
            summary.put("memberId", connectedMember.getId());

            log.info("✅ Résumé généré: totalAmount={}, totalCount={}", totalAmount, totalCount);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du résumé des cotisations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =============================================
    // CRÉATION DE COTISATIONS INDIVIDUELLES
    // =============================================

    @PostMapping("/individual")
    public ResponseEntity<?> createIndividualContribution(@RequestBody ContributionRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("=== CRÉATION COTISATION INDIVIDUELLE ===");
            log.info("📥 Données reçues:");
            log.info("   - amount: {}", request.getAmount());
            log.info("   - paymentDate: {}", request.getPaymentDate());
            log.info("   - paymentMode: {}", request.getPaymentMode());
            log.info("   - contributionPeriodId: {}", request.getContributionPeriodId());
            log.info("   - paymentId: {}", request.getPaymentId());

            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            if (email == null) {
                return ResponseEntity.badRequest().body("Email non trouvé dans le token");
            }

            final String finalEmail = email;
            Member connectedMember = memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            log.info("✅ Membre trouvé: ID={}", connectedMember.getId());

            Contribution contribution = new Contribution();
            contribution.setAmount(request.getAmount());
            contribution.setPaymentDate(request.getPaymentDate() != null ?
                    java.sql.Date.valueOf(request.getPaymentDate()) : new java.util.Date());
            contribution.setPaymentMode(request.getPaymentMode() != null ?
                    request.getPaymentMode() : "KKIAPAY");
            contribution.setPaymentProof(request.getPaymentProof());
            contribution.setContributionType(ContributionType.INDIVIDUAL);
            contribution.setMember(connectedMember);
            contribution.setMembers(null);

            ContributionPeriod period = contributionPeriodRepository.findById(request.getContributionPeriodId())
                    .orElseThrow(() -> new RuntimeException("Période de cotisation non trouvée"));
            contribution.setContributionPeriod(period);
            log.info("✅ Période trouvée: ID={}", period.getId());

            if (request.getPaymentId() != null) {
                final Long paymentId = request.getPaymentId();
                Payment payment = paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec ID: " + paymentId));
                contribution.setPayment(payment);
                log.info("✅ Paiement lié: ID={}", paymentId);
            }

            Contribution createdContribution = contributionService.createContribution(contribution);
            log.info("✅ Cotisation créée avec succès: ID={}", createdContribution.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(createdContribution);

        } catch (RuntimeException e) {
            log.error("❌ Erreur métier: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur technique:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur: " + e.getMessage());
        }
    }

    // =============================================
    // CRÉATION DE COTISATIONS GROUPÉES
    // =============================================

    @PostMapping("/group")
    public ResponseEntity<?> createGroupContribution(@RequestBody GroupContributionRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        try {
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            if (email == null) {
                return ResponseEntity.badRequest().body("Email non trouvé dans le token");
            }

            final String finalEmail = email;
            memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            if (request.getMemberIds() == null || request.getMemberIds().size() < 2) {
                return ResponseEntity.badRequest().body("Une cotisation groupée doit concerner au moins 2 membres !");
            }

            final Long periodId = request.getContributionPeriodId();
            ContributionPeriod contributionPeriod = contributionPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new RuntimeException("Période de cotisation non trouvée avec ID: " + periodId));

            Payment payment = null;
            if (request.getPaymentId() != null) {
                final Long paymentId = request.getPaymentId();
                payment = paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec ID: " + paymentId));
            }

            List<Contribution> createdContributions = new ArrayList<>();

            for (Long memberId : request.getMemberIds()) {
                final Long currentMemberId = memberId;
                Member member = memberRepository.findById(currentMemberId)
                        .orElseThrow(() -> new RuntimeException("Membre non trouvé avec ID: " + currentMemberId));

                Contribution individualContribution = new Contribution();
                individualContribution.setAmount(request.getAmount());

                if (request.getPaymentDate() != null && !request.getPaymentDate().isEmpty()) {
                    individualContribution.setPaymentDate(java.sql.Date.valueOf(request.getPaymentDate()));
                } else {
                    individualContribution.setPaymentDate(new java.util.Date());
                }

                individualContribution.setPaymentMode(request.getPaymentMode() != null ?
                        request.getPaymentMode() : "KKIAPAY");
                individualContribution.setPaymentProof(request.getPaymentProof());
                individualContribution.setContributionPeriod(contributionPeriod);
                individualContribution.setContributionType(ContributionType.INDIVIDUAL);
                individualContribution.setMember(member);

                if (payment != null) {
                    individualContribution.setPayment(payment);
                }

                Contribution created = contributionService.createContribution(individualContribution);
                createdContributions.add(created);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdContributions);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur: " + e.getMessage());
        }
    }

    // =============================================
    // ENDPOINTS DE LECTURE PAR TYPE
    // =============================================

    @GetMapping("/individual")
    public ResponseEntity<List<Contribution>> getIndividualContributions() {
        try {
            List<Contribution> contributions = contributionService.getContributionsByType(ContributionType.INDIVIDUAL);
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/group")
    public ResponseEntity<List<Contribution>> getGroupContributions() {
        try {
            List<Contribution> contributions = contributionService.getContributionsByType(ContributionType.GROUP);
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =============================================
    // ENDPOINTS PERSONNALISÉS (MES COTISATIONS)
    // =============================================

    @GetMapping("/my-contributions")
    public ResponseEntity<List<Contribution>> getMyAllContributions(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            final String finalEmail = email;
            Member connectedMember = memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            List<Contribution> contributions = contributionService.getContributionsByMember(connectedMember.getId());
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            log.error("Erreur récupération toutes les cotisations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/individual/my-contributions")
    public ResponseEntity<List<Contribution>> getMyIndividualContributions(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            final String finalEmail = email;
            Member connectedMember = memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            List<Contribution> contributions = contributionService.getIndividualContributionsByMember(connectedMember.getId());
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            log.error("Erreur récupération cotisations individuelles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/group/my-contributions")
    public ResponseEntity<List<Contribution>> getMyGroupContributions(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            final String finalEmail = email;
            Member connectedMember = memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            List<Contribution> contributions = contributionService.getGroupContributionsByMember(connectedMember.getId());
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            log.error("Erreur récupération cotisations groupées: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =============================================
    // STATISTIQUES
    // =============================================

    @GetMapping("/total-amount")
    public ResponseEntity<BigDecimal> getTotalContributionsAmount() {
        try {
            BigDecimal totalAmount = contributionService.getTotalContributionsAmount();
            return ResponseEntity.ok(totalAmount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BigDecimal.ZERO);
        }
    }

    @GetMapping("/my-total-amount")
    public ResponseEntity<BigDecimal> getMyTotalContributionsAmount(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            final String finalEmail = email;
            Member connectedMember = memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            BigDecimal totalAmount = contributionService.getTotalContributionsAmountByMember(connectedMember.getId());
            return ResponseEntity.ok(totalAmount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BigDecimal.ZERO);
        }
    }

    @GetMapping("/total-amount/{contributionType}")
    public ResponseEntity<BigDecimal> getTotalAmountByType(@PathVariable ContributionType contributionType) {
        try {
            BigDecimal totalAmount = contributionService.getTotalAmountByType(contributionType);
            return ResponseEntity.ok(totalAmount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BigDecimal.ZERO);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<ContributionStatistics> getContributionStatistics() {
        try {
            BigDecimal totalAmount = contributionService.getTotalContributionsAmount();
            BigDecimal individualTotal = contributionService.getTotalAmountByType(ContributionType.INDIVIDUAL);
            BigDecimal groupTotal = contributionService.getTotalAmountByType(ContributionType.GROUP);

            ContributionStatistics statistics = new ContributionStatistics(totalAmount, individualTotal, groupTotal);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =============================================
    // CLASSES INTERNES
    // =============================================

    public static class ContributionStatistics {
        private final BigDecimal totalAmount;
        private final BigDecimal individualAmount;
        private final BigDecimal groupAmount;

        public ContributionStatistics(BigDecimal totalAmount, BigDecimal individualAmount, BigDecimal groupAmount) {
            this.totalAmount = totalAmount;
            this.individualAmount = individualAmount;
            this.groupAmount = groupAmount;
        }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getIndividualAmount() { return individualAmount; }
        public BigDecimal getGroupAmount() { return groupAmount; }
    }

    public static class ContributionRequest {
        private BigDecimal amount;
        private String paymentDate;
        private String paymentMode;
        private String paymentProof;
        private Long contributionPeriodId;
        private Long paymentId;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPaymentDate() { return paymentDate; }
        public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
        public String getPaymentMode() { return paymentMode; }
        public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
        public String getPaymentProof() { return paymentProof; }
        public void setPaymentProof(String paymentProof) { this.paymentProof = paymentProof; }
        public Long getContributionPeriodId() { return contributionPeriodId; }
        public void setContributionPeriodId(Long contributionPeriodId) { this.contributionPeriodId = contributionPeriodId; }
        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    }

    public static class GroupContributionRequest {
        private BigDecimal amount;
        private String paymentDate;
        private String paymentMode;
        private String paymentProof;
        private Long contributionPeriodId;
        private List<Long> memberIds;
        private Long paymentId;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPaymentDate() { return paymentDate; }
        public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
        public String getPaymentMode() { return paymentMode; }
        public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
        public String getPaymentProof() { return paymentProof; }
        public void setPaymentProof(String paymentProof) { this.paymentProof = paymentProof; }
        public Long getContributionPeriodId() { return contributionPeriodId; }
        public void setContributionPeriodId(Long contributionPeriodId) { this.contributionPeriodId = contributionPeriodId; }
        public List<Long> getMemberIds() { return memberIds; }
        public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    }
}