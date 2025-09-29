package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Contribution;
import com.wbf.mutuelle.entities.ContributionPeriod;
import com.wbf.mutuelle.entities.ContributionType;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.services.ContributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
@RequestMapping("/mut/contribution")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;
    private final MemberRepository memberRepository;

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

    // Créer une cotisation INDIVIDUELLE
    @PostMapping("/individual")
    public ResponseEntity<?> createIndividualContribution(@RequestBody Contribution contribution,
                                                          @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Member connectedMember = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé !"));

            // Forcer le type individuel
            contribution.setContributionType(ContributionType.INDIVIDUAL);

            Contribution createdContribution = createIndividualContribution(contribution, connectedMember);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdContribution);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur");
        }
    }

    // Créer une cotisation GROUPÉE
    @PostMapping("/group")
    public ResponseEntity<?> createGroupContribution(@RequestBody Contribution contribution,
                                                     @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Member connectedMember = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé !"));

            // Forcer le type groupé
            contribution.setContributionType(ContributionType.GROUP);

            Contribution createdContribution = createGroupContribution(contribution, connectedMember);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdContribution);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur");
        }
    }

    /**
     * Crée une cotisation individuelle
     */
    private Contribution createIndividualContribution(Contribution contribution, Member connectedMember) {
        // Validation pour individuel
        if (contribution.getMembers() != null && !contribution.getMembers().isEmpty()) {
            throw new RuntimeException("Une cotisation individuelle ne peut pas avoir une liste de membres !");
        }

        // Configuration pour individuel
        contribution.setMember(connectedMember); // Le membre connecté est le contributeur
        contribution.setMembers(null); // S'assurer que la liste est null

        return contributionService.createContribution(contribution);
    }

    /**
     * Crée une cotisation groupée
     */
    private Contribution createGroupContribution(Contribution contribution, Member connectedMember) {
        // Validation pour groupé
        if (contribution.getMember() != null) {
            throw new RuntimeException("Une cotisation groupée ne peut pas avoir un membre individuel !");
        }

        // Initialiser la liste si elle est null
        if (contribution.getMembers() == null) {
            contribution.setMembers(new ArrayList<>());
        }

        // S'assurer que le membre connecté est inclus dans la liste
        boolean connectedMemberInList = contribution.getMembers().stream()
                .anyMatch(m -> m.getId() != null && m.getId().equals(connectedMember.getId()));

        if (!connectedMemberInList) {
            // Ajouter le membre connecté à la liste
            Member memberRef = new Member();
            memberRef.setId(connectedMember.getId());
            contribution.getMembers().add(memberRef);
        }

        // S'assurer qu'il y a au moins 2 membres pour une cotisation groupée
        if (contribution.getMembers().size() < 2) {
            throw new RuntimeException("Une cotisation groupée doit concerner au moins 2 membres !");
        }

        return contributionService.createContribution(contribution);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContribution(@PathVariable Long id, @RequestBody Contribution contribution) {
        try {
            Contribution updatedContribution = contributionService.updateContribution(id, contribution);
            return ResponseEntity.ok(updatedContribution);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la mise à jour");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContribution(@PathVariable Long id) {
        try {
            contributionService.deleteContribution(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Obtenir toutes les contributions INDIVIDUELLES
    @GetMapping("/individual")
    public ResponseEntity<List<Contribution>> getIndividualContributions() {
        try {
            List<Contribution> contributions = contributionService.getContributionsByType(ContributionType.INDIVIDUAL);
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Obtenir toutes les contributions GROUPÉES
    @GetMapping("/group")
    public ResponseEntity<List<Contribution>> getGroupContributions() {
        try {
            List<Contribution> contributions = contributionService.getContributionsByType(ContributionType.GROUP);
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Obtenir les contributions individuelles du membre connecté
    @GetMapping("/individual/my-contributions")
    public ResponseEntity<List<Contribution>> getMyIndividualContributions(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Member connectedMember = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé !"));

            List<Contribution> contributions = contributionService.getIndividualContributionsByMember(connectedMember.getId());
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Obtenir les contributions groupées du membre connecté
    @GetMapping("/group/my_contributions")
    public ResponseEntity<List<Contribution>> getMyGroupContributions(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Member connectedMember = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé !"));

            List<Contribution> contributions = contributionService.getGroupContributionsByMember(connectedMember.getId());
            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // NOUVEAUX ENDPOINTS POUR LES STATISTIQUES FINANCIÈRES

    /**
     * Montant total de toutes les contributions
     */
    @GetMapping("/total_amount")
    public ResponseEntity<BigDecimal> getTotalContributionsAmount() {
        try {
            BigDecimal totalAmount = contributionService.getTotalContributionsAmount();
            return ResponseEntity.ok(totalAmount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BigDecimal.ZERO);
        }
    }

    /**
     * Montant total des contributions du membre connecté
     */
    @GetMapping("/my_total_amount")
    public ResponseEntity<BigDecimal> getMyTotalContributionsAmount(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Member connectedMember = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé !"));

            BigDecimal totalAmount = contributionService.getTotalContributionsAmountByMember(connectedMember.getId());
            return ResponseEntity.ok(totalAmount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BigDecimal.ZERO);
        }
    }

    /**
     * Montant total par type de contribution
     */
    @GetMapping("/total_amount/{contributionType}")
    public ResponseEntity<BigDecimal> getTotalAmountByType(@PathVariable ContributionType contributionType) {
        try {
            BigDecimal totalAmount = contributionService.getTotalAmountByType(contributionType);
            return ResponseEntity.ok(totalAmount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BigDecimal.ZERO);
        }
    }

    /**
     * Statistiques complètes
     */
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

    // Classe interne pour les statistiques
    public static class ContributionStatistics {
        private final BigDecimal totalAmount;
        private final BigDecimal individualAmount;
        private final BigDecimal groupAmount;

        public ContributionStatistics(BigDecimal totalAmount, BigDecimal individualAmount, BigDecimal groupAmount) {
            this.totalAmount = totalAmount;
            this.individualAmount = individualAmount;
            this.groupAmount = groupAmount;
        }

        // Getters
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getIndividualAmount() { return individualAmount; }
        public BigDecimal getGroupAmount() { return groupAmount; }
    }


    @RequestMapping("/mut/upload")
    @CrossOrigin(origins = "http://localhost:3000")
    public class FileUploadController {

        private final String UPLOAD_DIR = "./uploads/payment-proofs/";

        @PostMapping("/payment-proof")
        public ResponseEntity<?> uploadPaymentProof(@RequestParam("file") MultipartFile file) {
            try {
                // Vérifier si le fichier est vide
                if (file.isEmpty()) {
                    return ResponseEntity.badRequest().body("Le fichier est vide");
                }

                // Vérifier le type de fichier
                String contentType = file.getContentType();
                if (contentType == null ||
                        (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                    return ResponseEntity.badRequest().body("Type de fichier non supporté. Formats acceptés: images et PDF");
                }

                // Vérifier la taille du fichier (5MB max)
                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body("Le fichier est trop volumineux. Taille maximale: 5MB");
                }

                // Créer le répertoire s'il n'existe pas
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Générer un nom de fichier unique
                String originalFileName = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFileName != null && originalFileName.contains(".")) {
                    fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                }

                String fileName = UUID.randomUUID().toString() + fileExtension;
                Path filePath = uploadPath.resolve(fileName);

                // Sauvegarder le fichier
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Retourner le nom du fichier
                return ResponseEntity.ok(fileName);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erreur lors de l'upload du fichier: " + e.getMessage());
            }
        }

        // Endpoint pour récupérer un fichier
        @GetMapping("/payment-proof/{filename}")
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
    }


}