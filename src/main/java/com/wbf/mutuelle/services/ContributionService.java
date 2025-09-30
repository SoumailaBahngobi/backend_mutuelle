package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Contribution;
import com.wbf.mutuelle.entities.ContributionPeriod;
import com.wbf.mutuelle.entities.ContributionType;
import com.wbf.mutuelle.repositories.ContributionRepository;
import com.wbf.mutuelle.repositories.ContributionPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    // =============================================
    // MÉTHODES CRUD DE BASE
    // =============================================

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

    public Contribution getContributionById(Long id) {
        Contribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contribution non trouvée avec l'ID : " + id));

        // Calculer et assigner la balance
        BigDecimal totalBalance = calculateTotalBalance();
        contribution.setBalance(totalBalance);

        return contribution;
    }

    // =============================================
    // MÉTHODES DE RECHERCHE ET FILTRAGE
    // =============================================

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

    // NOUVELLE MÉTHODE : Toutes les contributions d'un membre (individuelles + groupées)
    public List<Contribution> getContributionsByMember(Long memberId) {
        try {
            List<Contribution> contributions = contributionRepository.findByMemberIdOrMembersId(memberId);

            // Calculer la balance totale du membre
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);

            // Assigner la balance du membre à chaque contribution
            contributions.forEach(contribution -> contribution.setBalance(memberBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de toutes les contributions du membre ID: " + memberId, e);
            return Collections.emptyList();
        }
    }

    // NOUVELLE MÉTHODE : Contributions individuelles d'un membre (version optimisée)
    public List<Contribution> getIndividualContributionsByMember(Long memberId) {
        try {
            List<Contribution> contributions = contributionRepository
                    .findByMemberIdAndContributionType(memberId, ContributionType.INDIVIDUAL);

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

    // NOUVELLE MÉTHODE : Contributions groupées d'un membre (version optimisée)
    public List<Contribution> getGroupContributionsByMember(Long memberId) {
        try {
            List<Contribution> contributions = contributionRepository
                    .findByMemberIdInMembersAndContributionType(memberId, ContributionType.GROUP);

            // Calculer la balance totale du membre
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);

            // Assigner la balance du membre à chaque contribution
            contributions.forEach(contribution -> contribution.setBalance(memberBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions groupées du membre ID: " + memberId, e);
            return Collections.emptyList();
        }
    }

    // =============================================
    // MÉTHODES DE STATISTIQUES
    // =============================================

    /**
     * Calcule le montant total des contributions par membre (individuelles + part groupées)
     */
    public BigDecimal getTotalContributionsAmountByMember(Long memberId) {
        try {
            // Utiliser la méthode optimisée du repository
            BigDecimal total = contributionRepository.getTotalAmountByMember(memberId);
            
            log.info("Montant total des contributions pour le membre {}: {}", memberId, total);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul du montant total des contributions du membre ID: " + memberId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Montant total de toutes les contributions
     */
    public BigDecimal getTotalContributionsAmount() {
        try {
            BigDecimal total = contributionRepository.getTotalAmount();
            log.info("Montant total de toutes les contributions: {}", total);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul du montant total des contributions", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Montant total par type de contribution
     */
    public BigDecimal getTotalAmountByType(ContributionType contributionType) {
        try {
            if (contributionType == null) {
                throw new IllegalArgumentException("Le type de contribution ne peut pas être null");
            }

            BigDecimal total = contributionRepository.getTotalAmountByType(contributionType);
            
            log.info("Montant total pour le type {}: {}", contributionType, total);
            return total != null ? total : BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("Erreur lors du calcul du montant total par type: " + contributionType, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calcule la balance totale de toutes les cotisations
     */
    public BigDecimal calculateTotalBalance() {
        try {
            BigDecimal totalBalance = contributionRepository.calculateTotalBalance();
            log.info("Balance totale calculée: {}", totalBalance);
            return totalBalance != null ? totalBalance : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la balance totale", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calcule la balance d'un membre spécifique
     */
    public BigDecimal calculateMemberBalance(Long memberId) {
        try {
            BigDecimal balance = contributionRepository.calculateBalanceByMemberId(memberId);
            return balance != null ? balance : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la balance du membre ID: " + memberId, e);
            return BigDecimal.ZERO;
        }
    }

    // =============================================
    // MÉTHODES UTILITAIRES SUPPLÉMENTAIRES
    // =============================================

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

    // NOUVELLES MÉTHODES UTILITAIRES

    public List<Contribution> getContributionsByPeriod(Long periodId) {
        try {
            return contributionRepository.findByContributionPeriodId(periodId);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions par période ID: " + periodId, e);
            return Collections.emptyList();
        }
    }

    public List<Contribution> getRecentContributionsByMember(Long memberId, int limit) {
        try {
            return contributionRepository.findRecentContributionsByMemberId(memberId, PageRequest.of(0, limit));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions récentes du membre ID: " + memberId, e);
            return Collections.emptyList();
        }
    }

    public Long getContributionsCountByMember(Long memberId) {
        try {
            return contributionRepository.countContributionsByMemberId(memberId);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des contributions du membre ID: " + memberId, e);
            return 0L;
        }
    }
}