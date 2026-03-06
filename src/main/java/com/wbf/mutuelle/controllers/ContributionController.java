package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.*;
import com.wbf.mutuelle.repositories.ContributionPeriodRepository;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.repositories.PaymentRepository;
import com.wbf.mutuelle.services.ContributionService;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.UUID;

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

    // =============================================
    // ENDPOINTS D'UPLOAD DE FICHIERS
    // =============================================

    @PostMapping("/upload/payment-proof")
    public ResponseEntity<?> uploadPaymentProof(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== DÉBUT UPLOAD ===");
            System.out.println("Nom: " + file.getOriginalFilename());
            System.out.println("Taille: " + file.getSize());

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

            System.out.println("=== UPLOAD RÉUSSI: " + fileName + " ===");
            return ResponseEntity.ok(fileName);

        } catch (Exception e) {
            System.err.println("=== ERREUR UPLOAD ===");
            e.printStackTrace();
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
    // CRÉATION DE COTISATIONS INDIVIDUELLES
    // =============================================

    @PostMapping("/individual")
    public ResponseEntity<?> createIndividualContribution(@RequestBody ContributionRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        try {
            // Récupérer l'email depuis le token JWT
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            if (email == null) {
                return ResponseEntity.badRequest().body("Email non trouvé dans le token");
            }

            // Créer une copie finale de l'email pour l'utiliser dans les lambdas
            final String finalEmail = email;

            // Chercher le membre dans la base de données
            Member connectedMember = memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            // Créer la contribution
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

            // Récupérer la période de cotisation
            ContributionPeriod period = contributionPeriodRepository.findById(request.getContributionPeriodId())
                    .orElseThrow(() -> new RuntimeException("Période de cotisation non trouvée"));
            contribution.setContributionPeriod(period);

            // Si un paymentId est fourni, récupérer le paiement
            if (request.getPaymentId() != null) {
                final Long paymentId = request.getPaymentId(); // Variable final pour lambda
                Payment payment = paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec ID: " + paymentId));
                contribution.setPayment(payment);
            }

            // Sauvegarder
            Contribution createdContribution = contributionService.createContribution(contribution);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdContribution);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
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
            // Récupérer l'email depuis le token JWT
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            if (email == null) {
                return ResponseEntity.badRequest().body("Email non trouvé dans le token");
            }

            // Créer une copie finale de l'email
            final String finalEmail = email;

            // Vérifier que l'utilisateur est authentifié
            memberRepository.findByEmail(finalEmail)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec email: " + finalEmail));

            // Vérifier qu'il y a au moins 2 membres
            if (request.getMemberIds() == null || request.getMemberIds().size() < 2) {
                return ResponseEntity.badRequest().body("Une cotisation groupée doit concerner au moins 2 membres !");
            }

            // Récupérer la période de cotisation
            final Long periodId = request.getContributionPeriodId(); // Variable final pour lambda
            ContributionPeriod contributionPeriod = contributionPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new RuntimeException("Période de cotisation non trouvée avec ID: " + periodId));

            // Récupérer le paiement si fourni
            Payment payment = null;
            if (request.getPaymentId() != null) {
                final Long paymentId = request.getPaymentId(); // Variable final pour lambda
                payment = paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec ID: " + paymentId));
            }

            // Créer une cotisation pour chaque membre
            List<Contribution> createdContributions = new ArrayList<>();

            for (Long memberId : request.getMemberIds()) {
                final Long currentMemberId = memberId; // Variable final pour lambda
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

                // Lier le paiement si fourni
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
            System.err.println("Erreur récupération toutes les cotisations: " + e.getMessage());
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
            System.err.println("Erreur récupération cotisations individuelles: " + e.getMessage());
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
            System.err.println("Erreur récupération cotisations groupées: " + e.getMessage());
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

    // =============================================
    // DTO POUR COTISATIONS INDIVIDUELLES
    // =============================================

    public static class ContributionRequest {
        private BigDecimal amount;
        private String paymentDate;
        private String paymentMode;
        private String paymentProof;
        private Long contributionPeriodId;
        private Long paymentId;

        // Getters et Setters
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

    // =============================================
    // DTO POUR COTISATIONS GROUPÉES
    // =============================================

    public static class GroupContributionRequest {
        private BigDecimal amount;
        private String paymentDate;
        private String paymentMode;
        private String paymentProof;
        private Long contributionPeriodId;
        private List<Long> memberIds;
        private Long paymentId;

        // Getters et Setters
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