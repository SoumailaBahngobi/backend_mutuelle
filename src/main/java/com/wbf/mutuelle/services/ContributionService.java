package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Contribution;
import com.wbf.mutuelle.entities.ContributionPeriod;
import com.wbf.mutuelle.entities.ContributionType;
import com.wbf.mutuelle.entities.Payment;
import com.wbf.mutuelle.repositories.PaymentRepository;
import com.wbf.mutuelle.repositories.ContributionRepository;
import com.wbf.mutuelle.repositories.ContributionPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final ContributionPeriodRepository contributionPeriodRepository;
    private final PaymentRepository paymentRepository;

    // =============================================
    // MÉTHODES CRUD DE BASE
    // =============================================

    @Transactional
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

            // Sauvegarder d'abord la contribution
            Contribution savedContribution = contributionRepository.save(contribution);

            // Gérer la relation avec le paiement si présent
            if (contribution.getPayment() != null && contribution.getPayment().getId() != null) {
                Payment payment = paymentRepository.findById(contribution.getPayment().getId())
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec ID: " + contribution.getPayment().getId()));

                // Mettre à jour la relation bidirectionnelle
                payment.setContribution(savedContribution);
                paymentRepository.save(payment);

                // Mettre à jour la contribution avec la référence au paiement
                savedContribution.setPayment(payment);
                savedContribution = contributionRepository.save(savedContribution);

                log.info("Paiement {} lié à la contribution {}", payment.getTransactionId(), savedContribution.getId());
            }

            // Calculer et assigner la balance totale après sauvegarde
            BigDecimal totalBalance = calculateTotalBalance();
            savedContribution.setBalance(totalBalance);

            log.info("Contribution créée avec succès: ID {}", savedContribution.getId());
            return savedContribution;

        } catch (Exception e) {
            log.error("Erreur lors de la création de la contribution", e);
            throw new RuntimeException("Erreur lors de la création de la contribution : " + e.getMessage());
        }
    }

    @Transactional
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

            // Mettre à jour la relation avec le paiement si fourni
            if (contributionDetails.getPayment() != null && contributionDetails.getPayment().getId() != null) {
                Payment payment = paymentRepository.findById(contributionDetails.getPayment().getId())
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

                // Mettre à jour la relation bidirectionnelle
                payment.setContribution(contribution);
                paymentRepository.save(payment);
                contribution.setPayment(payment);
            }

            Contribution updatedContribution = contributionRepository.save(contribution);

            // Recalculer et assigner la balance totale après mise à jour
            BigDecimal totalBalance = calculateTotalBalance();
            updatedContribution.setBalance(totalBalance);

            log.info("Contribution mise à jour: ID {}", id);
            return updatedContribution;

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la contribution ID: " + id, e);
            throw new RuntimeException("Erreur lors de la mise à jour de la contribution : " + e.getMessage());
        }
    }

    @Transactional
    public void deleteContribution(Long id) {
        try {
            Contribution contribution = getContributionById(id);

            // Dissocier le paiement si présent
            if (contribution.getPayment() != null) {
                Payment payment = contribution.getPayment();
                payment.setContribution(null);
                paymentRepository.save(payment);
            }

            contributionRepository.delete(contribution);
            log.info("Contribution supprimée: ID {}", id);

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

    public BigDecimal getTotalContributionsAmountByMember(Long memberId) {
        try {
            BigDecimal total = contributionRepository.getTotalAmountByMember(memberId);

            log.info("Montant total des contributions pour le membre {}: {}", memberId, total);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul du montant total des contributions du membre ID: " + memberId, e);
            return BigDecimal.ZERO;
        }
    }

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
    // MÉTHODES UTILITAIRES
    // =============================================

    private BigDecimal calculateContributionAmount(Contribution contribution, ContributionPeriod period) {
        BigDecimal individualAmount = period.getIndividualAmount();

        if (individualAmount == null || individualAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant individuel de la période n'est pas défini ou invalide !");
        }

        if (contribution.getContributionType() == ContributionType.INDIVIDUAL) {
            return individualAmount;
        } else if (contribution.getContributionType() == ContributionType.GROUP) {
            if (contribution.getMembers() == null || contribution.getMembers().isEmpty()) {
                throw new RuntimeException("Une cotisation groupée doit avoir au moins un membre !");
            }

            int numberOfMembers = contribution.getMembers().size();
            return individualAmount.multiply(BigDecimal.valueOf(numberOfMembers));
        } else {
            throw new RuntimeException("Type de contribution non supporté !");
        }
    }

    public Contribution saveContributionWithBalance(Contribution contribution) {
        Contribution savedContribution = contributionRepository.save(contribution);

        BigDecimal balance = calculateTotalBalance();
        savedContribution.setBalance(balance);

        return savedContribution;
    }

    public List<Contribution> getAllContributionsWithBalance() {
        try {
            List<Contribution> contributions = contributionRepository.findAll();
            BigDecimal totalBalance = calculateTotalBalance();

            contributions.forEach(contribution -> contribution.setBalance(totalBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions avec balance", e);
            return Collections.emptyList();
        }
    }

    public List<Contribution> getMemberContributionsWithBalance(Long memberId) {
        try {
            List<Contribution> contributions = contributionRepository.findByMemberId(memberId);
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);

            contributions.forEach(contribution -> contribution.setBalance(memberBalance));

            return contributions;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions du membre avec balance", e);
            return Collections.emptyList();
        }
    }

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

    // =============================================
    // MÉTHODES CORRIGÉES POUR LA RECHERCHE PAR PAIEMENT
    // =============================================

    /**
     * Récupère la contribution associée à un paiement (relation OneToOne)
     * @param paymentId L'ID du paiement
     * @return La contribution trouvée ou null
     */
    public Contribution getContributionByPaymentId(Long paymentId) {
        try {
            Optional<Contribution> contribution = contributionRepository.findByPaymentId(paymentId);
            if (contribution.isPresent()) {
                BigDecimal totalBalance = calculateTotalBalance();
                contribution.get().setBalance(totalBalance);
                log.info("Contribution trouvée pour le paiement ID: {}", paymentId);
                return contribution.get();
            }
            log.info("Aucune contribution trouvée pour le paiement ID: {}", paymentId);
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la contribution par paiement ID: " + paymentId, e);
            return null;
        }
    }

    /**
     * Récupère la contribution associée à un paiement sous forme d'Optional
     * @param paymentId L'ID du paiement
     * @return Optional contenant la contribution ou vide
     */
    public Optional<Contribution> findContributionByPaymentId(Long paymentId) {
        try {
            Optional<Contribution> contribution = contributionRepository.findByPaymentId(paymentId);
            contribution.ifPresent(c -> {
                BigDecimal totalBalance = calculateTotalBalance();
                c.setBalance(totalBalance);
            });
            return contribution;
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de contribution par paiement ID: " + paymentId, e);
            return Optional.empty();
        }
    }

    /**
     * Vérifie si un paiement est déjà associé à une contribution
     * @param paymentId L'ID du paiement
     * @return true si une contribution existe pour ce paiement
     */
    public boolean existsContributionByPaymentId(Long paymentId) {
        try {
            return contributionRepository.findByPaymentId(paymentId).isPresent();
        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'existence de contribution par paiement ID: " + paymentId, e);
            return false;
        }
    }

    /**
     * Récupère les contributions associées à un paiement (version List pour compatibilité)
     * @param paymentId L'ID du paiement
     * @return Liste contenant 0 ou 1 contribution
     */
    public List<Contribution> getContributionsByPaymentIdAsList(Long paymentId) {
        try {
            Optional<Contribution> contribution = contributionRepository.findByPaymentId(paymentId);
            if (contribution.isPresent()) {
                BigDecimal totalBalance = calculateTotalBalance();
                contribution.get().setBalance(totalBalance);
                return List.of(contribution.get());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des contributions par paiement ID: " + paymentId, e);
            return Collections.emptyList();
        }
    }
}