package com.wbf.mutuelle.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbf.mutuelle.PaymentStatus;
import com.wbf.mutuelle.PaymentType;
import com.wbf.mutuelle.configuration.KkiapayConfig;
import com.wbf.mutuelle.entities.*;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KkiapayService {

    private final RestTemplate restTemplate;
    private final KkiapayConfig kkiapayConfig;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    /**
     * Initie un paiement avec Kkiapay
     */
    @Transactional
    public Payment initiatePayment(Long memberId, BigDecimal amount, String phoneNumber, PaymentType paymentType) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé avec ID: " + memberId));

            // Vérifier que le montant est valide
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Montant invalide");
            }

            // Vérifier le numéro de téléphone
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new RuntimeException("Numéro de téléphone requis");
            }

            // Nettoyer le numéro de téléphone
            String cleanPhoneNumber = phoneNumber.replaceAll("[^0-9]", "");

            // Créer une transaction unique
            String transactionId = generateTransactionId();

            // Créer l'enregistrement de paiement
            Payment payment = new Payment();
            payment.setTransactionId(transactionId);
            payment.setAmount(amount);
            payment.setPhoneNumber(cleanPhoneNumber);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentType(paymentType);
            payment.setMember(member);
            payment.setPaymentDate(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);
            log.info("✅ Paiement initié: {} pour {} FCFA (membre: {})",
                    transactionId, amount, member.getEmail());

            return savedPayment;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initiation du paiement", e);
            throw new RuntimeException("Erreur lors de l'initiation du paiement: " + e.getMessage());
        }
    }

    /**
     * Vérifie le statut d'une transaction
     */
    @Transactional
    public Payment verifyTransaction(String transactionId) {
        try {
            String url = kkiapayConfig.getBaseUrl() + "/api/v1/transactions/" + transactionId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", kkiapayConfig.getApiKey());
            headers.set("X-SECRET-KEY", kkiapayConfig.getSecretKey());
            headers.set("X-PRIVATE-KEY", kkiapayConfig.getPrivateKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("Vérification de la transaction: {}", transactionId);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                Payment payment = paymentRepository.findByTransactionId(transactionId)
                        .orElseThrow(() -> new RuntimeException("Transaction non trouvée: " + transactionId));

                String status = jsonResponse.has("status") ? jsonResponse.get("status").asText() : "";
                log.info("Statut reçu pour {}: {}", transactionId, status);

                // Mettre à jour le statut
                PaymentStatus previousStatus = payment.getStatus();

                if ("SUCCESS".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    log.info("✅ Paiement réussi: {}", transactionId);
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.FAILED);
                    log.warn("❌ Paiement échoué: {}", transactionId);
                } else if ("CANCELLED".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.CANCELLED);
                    log.info("Paiement annulé: {}", transactionId);
                }

                // Si le statut a changé, sauvegarder
                if (previousStatus != payment.getStatus()) {
                    payment = paymentRepository.save(payment);
                }

                return payment;
            }

            return null;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification de la transaction: {}", transactionId, e);
            throw new RuntimeException("Erreur lors de la vérification du paiement: " + e.getMessage());
        }
    }

    /**
     * Rembourse une transaction
     */
    @Transactional
    public Payment refundTransaction(String transactionId) {
        try {
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction non trouvée: " + transactionId));

            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                throw new RuntimeException("Seules les transactions réussies peuvent être remboursées");
            }

            String url = kkiapayConfig.getBaseUrl() + "/api/v1/transactions/" + transactionId + "/refund";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", kkiapayConfig.getApiKey());
            headers.set("X-SECRET-KEY", kkiapayConfig.getSecretKey());
            headers.set("X-PRIVATE-KEY", kkiapayConfig.getPrivateKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("Tentative de remboursement: {}", transactionId);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                payment.setStatus(PaymentStatus.REFUNDED);
                Payment refundedPayment = paymentRepository.save(payment);
                log.info("✅ Remboursement effectué: {}", transactionId);
                return refundedPayment;
            }

            return null;

        } catch (Exception e) {
            log.error(" Erreur lors du remboursement: {}", transactionId, e);
            throw new RuntimeException("Erreur lors du remboursement: " + e.getMessage());
        }
    }

    /**
     * Génère un ID de transaction unique
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    /**
     * Récupère les paiements d'un membre
     */
    public List<Payment> getMemberPayments(Long memberId) {
        return paymentRepository.findByMemberId(memberId);
    }

    /**
     * Récupère un paiement par ID de transaction
     */
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée: " + transactionId));
    }

    /**
     * Récupère les paiements réussis d'un membre
     */
    public List<Payment> getSuccessfulPaymentsByMember(Long memberId) {
        return paymentRepository.findByMemberIdAndStatus(memberId, PaymentStatus.SUCCESS);
    }

    /**
     * Calcule le total des paiements réussis d'un membre
     */
    public BigDecimal getTotalSuccessfulPaymentsByMember(Long memberId) {
        return paymentRepository.getTotalSuccessfulPaymentsByMember(memberId);
    }
}