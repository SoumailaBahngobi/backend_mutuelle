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
    private final EmailService emailService;

    // =============================================
    // MÉTHODES CRUD DE BASE
    // =============================================

    @Transactional
    public Contribution createContribution(Contribution contribution) {
        try {
            if (contribution.getContributionPeriod() == null || contribution.getContributionPeriod().getId() == null) {
                throw new RuntimeException("La période de contribution doit être spécifiée !");
            }

            ContributionPeriod period = contributionPeriodRepository
                    .findById(contribution.getContributionPeriod().getId())
                    .orElseThrow(() -> new RuntimeException("Période de contribution non trouvée !"));

            BigDecimal calculatedAmount = calculateContributionAmount(contribution, period);
            contribution.setAmount(calculatedAmount);

            if (contribution.getPaymentDate() == null) {
                contribution.setPaymentDate(new java.util.Date());
            }

            Contribution savedContribution = contributionRepository.save(contribution);

            if (contribution.getPayment() != null && contribution.getPayment().getId() != null) {
                Payment payment = paymentRepository.findById(contribution.getPayment().getId())
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec ID: " + contribution.getPayment().getId()));

                payment.setContribution(savedContribution);
                paymentRepository.save(payment);

                savedContribution.setPayment(payment);
                savedContribution = contributionRepository.save(savedContribution);

                log.info("Paiement {} lié à la contribution {}", payment.getTransactionId(), savedContribution.getId());
            }

            BigDecimal totalBalance = calculateTotalBalance();
            savedContribution.setBalance(totalBalance);

            log.info("Contribution créée avec succès: ID {}", savedContribution.getId());

            if (savedContribution.getMember() != null && savedContribution.getMember().getEmail() != null) {
                try {
                    String memberName = savedContribution.getMember().getFirstName() + " " + savedContribution.getMember().getName();
                    String periodName = savedContribution.getContributionPeriod() != null ?
                            savedContribution.getContributionPeriod().getName() : "Période en cours";
                    String paymentDate = savedContribution.getPaymentDate() != null ?
                            savedContribution.getPaymentDate().toString() : new java.util.Date().toString();

                    emailService.sendContributionConfirmation(
                            savedContribution.getMember().getEmail(),
                            memberName,
                            savedContribution.getAmount().doubleValue(),
                            periodName,
                            paymentDate
                    );
                    log.info("Email de confirmation de cotisation envoyé à {}", savedContribution.getMember().getEmail());
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi de l'email de confirmation de cotisation: {}", e.getMessage());
                }
            }

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

            if (contributionDetails.getPaymentDate() != null) {
                contribution.setPaymentDate(contributionDetails.getPaymentDate());
            }
            if (contributionDetails.getPaymentMode() != null) {
                contribution.setPaymentMode(contributionDetails.getPaymentMode());
            }
            if (contributionDetails.getPaymentProof() != null) {
                contribution.setPaymentProof(contributionDetails.getPaymentProof());
            }

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

            if (contributionDetails.getPayment() != null && contributionDetails.getPayment().getId() != null) {
                Payment payment = paymentRepository.findById(contributionDetails.getPayment().getId())
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

                payment.setContribution(contribution);
                paymentRepository.save(payment);
                contribution.setPayment(payment);
            }

            Contribution updatedContribution = contributionRepository.save(contribution);

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
            BigDecimal totalBalance = calculateTotalBalance();
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
            BigDecimal totalBalance = calculateTotalBalance();
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
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);
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
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);
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
            BigDecimal memberBalance = getTotalContributionsAmountByMember(memberId);
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
    // MÉTHODES POUR LE RÉSUMÉ (DASHBOARD)
    // =============================================

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

    // =============================================
    // MÉTHODES POUR LA RECHERCHE PAR PAIEMENT
    // =============================================

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

    public boolean existsContributionByPaymentId(Long paymentId) {
        try {
            return contributionRepository.findByPaymentId(paymentId).isPresent();
        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'existence de contribution par paiement ID: " + paymentId, e);
            return false;
        }
    }

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