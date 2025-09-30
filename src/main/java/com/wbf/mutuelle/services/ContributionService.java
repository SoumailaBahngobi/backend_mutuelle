package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Contribution;
import com.wbf.mutuelle.entities.ContributionPeriod;
import com.wbf.mutuelle.entities.ContributionType;
import com.wbf.mutuelle.repositories.ContributionRepository;
import com.wbf.mutuelle.repositories.ContributionPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final ContributionPeriodRepository contributionPeriodRepository;

    public Contribution createContribution(Contribution contribution) {
        try {
            // Validation de base
            if (contribution.getContributionPeriod() == null || contribution.getContributionPeriod().getId() == null) {
                throw new RuntimeException("La période de contribution doit être spécifiée !");
            }

            // Récupérer la période complète avec le montant individuel
            ContributionPeriod period = contributionPeriodRepository
                    .findById(contribution.getContributionPeriod().getId())
                    .orElseThrow(() -> new RuntimeException("Période de contribution non trouvée !"));

            // Calculer le montant selon le type de contribution
            BigDecimal calculatedAmount = calculateContributionAmount(contribution, period);
            contribution.setAmount(calculatedAmount);

            // Date de paiement par défaut
            if (contribution.getPaymentDate() == null) {
                contribution.setPaymentDate(new java.util.Date());
            }

            Contribution savedContribution = contributionRepository.save(contribution);

            // Calculer et assigner la balance totale après sauvegarde
            BigDecimal totalBalance = calculateTotalBalance();
            savedContribution.setBalance(totalBalance);

            return savedContribution;

        } catch (Exception e) {
            log.error("Erreur lors de la création de la contribution", e);
            throw new RuntimeException("Erreur lors de la création de la contribution : " + e.getMessage());
        }
    }

    public Contribution updateContribution(Long id, Contribution contributionDetails) {
        try {
            Contribution contribution = getContributionById(id);

            // Mettre à jour les champs modifiables
            if (contributionDetails.getPaymentDate() != null) {
                contribution.setPaymentDate(contributionDetails.getPaymentDate());
            }
            if (contributionDetails.getPaymentMode() != null) {
                contribution.setPaymentMode(contributionDetails.getPaymentMode());
            }
            if (contributionDetails.getPaymentProof() != null) {
                contribution.setPaymentProof(contributionDetails.getPaymentProof());
            }
            /*
             * if (contributionDetails.getPaymentProofFileName() != null) {
             * contribution.setPaymentProofFileName(contributionDetails.
             * getPaymentProofFileName());
             * }
             */

            // Si la période change, recalculer le montant
            if (contributionDetails.getContributionPeriod() != null &&
                    contributionDetails.getContributionPeriod().getId() != null &&
                    !contributionDetails.getContributionPeriod().getId()
                            .equals(contribution.getContributionPeriod().getId())) {

                ContributionPeriod newPeriod = contributionPeriodRepository
                        .findById(contributionDetails.getContributionPeriod().getId())
                        .orElseThrow(() -> new RuntimeException("Nouvelle période non trouvée !"));

                BigDecimal newAmount = calculateContributionAmount(contribution, newPeriod);
                contribution.setAmount(newAmount);
                contribution.setContributionPeriod(newPeriod);
            }

            Contribution updatedContribution = contributionRepository.save(contribution);

            // Recalculer et assigner la balance totale après mise à jour
            BigDecimal totalBalance = calculateTotalBalance();
            updatedContribution.setBalance(totalBalance);

            return updatedContribution;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la contribution ID: " + id, e);
            throw new RuntimeException("Erreur lors de la mise à jour de la contribution : " + e.getMessage());
        }
    }

    public void deleteContribution(Long id) {
        try {
            Contribution contribution = getContributionById(id);
            contributionRepository.delete(contribution);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la contribution ID: " + id, e);
            throw new RuntimeException("Erreur lors de la suppression de la contribution : " + e.getMessage());
        }
    }

    /**
     * Calcule le montant total des contributions par membre (individuelles + part
     * groupées)
     */
    public BigDecimal getTotalContributionsAmountByMember(Long memberId) {
        try {
            // Contributions individuelles du membre
            List<Contribution> individualContributions = getIndividualContributionsByMember(memberId);
            BigDecimal individualTotal = individualContributions.stream()
                    .map(Contribution::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Contributions groupées du membre
            List<Contribution> groupContributions = getGroupContributionsByMember(memberId);
            BigDecimal groupTotal = groupContributions.stream()
                    .map(Contribution::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal total = individualTotal.add(groupTotal);
            log.info("Montant total des contributions pour le membre {}: {} (individuel: {}, groupe: {})",
                    memberId, total, individualTotal, groupTotal);

            return total;
        } catch (Exception e) {
            log.error("Erreur lors du calcul du montant total des contributions du membre ID: " + memberId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calcule le montant de la contribution selon le type
     */
    private BigDecimal calculateContributionAmount(Contribution contribution, ContributionPeriod period) {
        BigDecimal individualAmount = period.getIndividualAmount();

        if (individualAmount == null || individualAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant individuel de la période n'est pas défini ou invalide !");
        }

        if (contribution.getContributionType() == ContributionType.INDIVIDUAL) {
            // Cotisation individuelle : montant individuel
            return individualAmount;

        } else if (contribution.getContributionType() == ContributionType.GROUP) {
            // Cotisation groupée : montant individuel × nombre de membres
            if (contribution.getMembers() == null || contribution.getMembers().isEmpty()) {
                throw new RuntimeException("Une cotisation groupée doit avoir au moins un membre !");
            }

            int numberOfMembers = contribution.getMembers().size();
            return individualAmount.multiply(BigDecimal.valueOf(numberOfMembers));

        } else {
            throw new RuntimeException("Type de contribution non supporté !");
        }
    }

    /**
     * Calcule la balance totale d'un membre spécifique
     */
    public BigDecimal calculateMemberBalance(Long memberId) {
        try {
            List<Contribution> contributions = contributionRepository.findByMemberId(memberId);
            BigDecimal balance = contributions.stream()
                    .map(Contribution::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return balance;
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la balance du membre ID: " + memberId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Met à jour la balance d'une cotisation après sauvegarde
     */
    public Contribution saveContributionWithBalance(Contribution contribution) {
        Contribution savedContribution = contributionRepository.save(contribution);

        // Calculer et définir la balance totale
        BigDecimal balance = calculateTotalBalance();
        savedContribution.setBalance(balance);

        return savedContribution;
    }

    /**
     * Récupère toutes les contributions avec leur balance totale
     */
    public List<Contribution> getAllContributionsWithBalance() {
        try {
            List<Contribution> contributions = contributionRepository.findAll();
            BigDecimal totalBalance = calculateTotalBalance();

            // Assigner la balance totale à chaque contribution
            contributions.forEach(contribution -> contribution.setBalance(totalBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions avec balance", e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les contributions d'un membre avec sa balance totale
     */
    public List<Contribution> getMemberContributionsWithBalance(Long memberId) {
        try {
            List<Contribution> contributions = contributionRepository.findByMemberId(memberId);
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);

            // Assigner la balance du membre à chaque contribution
            contributions.forEach(contribution -> contribution.setBalance(memberBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions du membre avec balance", e);
            return Collections.emptyList();
        }
    }

    public List<Contribution> getAllContributions() {
        try {
            List<Contribution> contributions = contributionRepository.findAll();

            // Calculer la balance totale une seule fois
            BigDecimal totalBalance = calculateTotalBalance();

            // Assigner la balance à chaque contribution
            contributions.forEach(contribution -> contribution.setBalance(totalBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de toutes les contributions", e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère une contribution par ID AVEC sa balance calculée
     */
    public Contribution getContributionById(Long id) {
        Contribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contribution non trouvée avec l'ID : " + id));

        // Calculer et assigner la balance
        BigDecimal totalBalance = calculateTotalBalance();
        contribution.setBalance(totalBalance);

        return contribution;
    }

    /**
     * Récupère les contributions par type AVEC balance calculée
     */
    public List<Contribution> getContributionsByType(ContributionType contributionType) {
        try {
            List<Contribution> contributions = contributionRepository.findByContributionType(contributionType);

            // Calculer la balance totale
            BigDecimal totalBalance = calculateTotalBalance();

            // Assigner la balance à chaque contribution
            contributions.forEach(contribution -> contribution.setBalance(totalBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions par type: " + contributionType, e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les contributions individuelles d'un membre AVEC balance
     */
    public List<Contribution> getIndividualContributionsByMember(Long memberId) {
        try {
            List<Contribution> contributions = contributionRepository
                    .findByContributionTypeAndMemberId(ContributionType.INDIVIDUAL, memberId);

            // Calculer la balance totale du membre
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);

            // Assigner la balance du membre à chaque contribution
            contributions.forEach(contribution -> contribution.setBalance(memberBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions individuelles du membre ID: " + memberId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les contributions groupées d'un membre AVEC balance
     */
    public List<Contribution> getGroupContributionsByMember(Long memberId) {
        try {
            List<Contribution> allGroupContributions = contributionRepository
                    .findByContributionType(ContributionType.GROUP);
            List<Contribution> memberContributions = allGroupContributions.stream()
                    .filter(contribution -> contribution.getMembers() != null)
                    .filter(contribution -> contribution.getMembers().stream()
                            .anyMatch(member -> member.getId().equals(memberId)))
                    .toList();

            // Calculer la balance totale du membre
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);

            // Assigner la balance du membre à chaque contribution
            memberContributions.forEach(contribution -> contribution.setBalance(memberBalance));

            return memberContributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions groupées du membre ID: " + memberId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Calcule la balance totale de toutes les cotisations (individuelles +
     * groupées)
     * C'est la SOMME TOTALE de tous les montants
     */
    public BigDecimal calculateTotalBalance() {
        try {
            // Utiliser la méthode du repository pour plus d'efficacité
            BigDecimal totalBalance = contributionRepository.calculateTotalBalance();
            log.info("Balance totale calculée: {}", totalBalance);
            return totalBalance != null ? totalBalance : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la balance totale", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Montant total de toutes les contributions (alias de calculateTotalBalance)
     */
    public BigDecimal getTotalContributionsAmount() {
        return calculateTotalBalance();
    }

    /**
     * Montant total par type de contribution
     */
    public BigDecimal getTotalAmountByType(ContributionType contributionType) {
        try {
            if (contributionType == null) {
                throw new IllegalArgumentException("Le type de contribution ne peut pas être null");
            }

            List<Contribution> contributions = contributionRepository.findByContributionType(contributionType);
            BigDecimal total = contributions.stream()
                    .map(Contribution::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("Montant total pour le type {}: {}", contributionType, total);
            return total;

        } catch (Exception e) {
            log.error("Erreur lors du calcul du montant total par type: " + contributionType, e);
            return BigDecimal.ZERO;
        }
    }

}