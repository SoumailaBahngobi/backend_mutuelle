package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Payment;
import com.wbf.mutuelle.PaymentStatus;
import com.wbf.mutuelle.PaymentType;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.repositories.PaymentRepository;
import com.wbf.mutuelle.services.KkiapayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mutuelle/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final KkiapayService kkiapayService;

    /**
     * ✅ Initiation du paiement - Crée un paiement avec status PENDING
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentInitiateRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("📝 Initiation paiement - Montant: {}, Téléphone: {}, Type: {}",
                    request.getAmount(), request.getPhoneNumber(), request.getPaymentType());

            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

            // Générer un ID de transaction unique
            String transactionId = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

            log.info("✅ Transaction ID généré: {}", transactionId);

            Payment payment = new Payment();
            payment.setTransactionId(transactionId);
            payment.setAmount(request.getAmount());
            payment.setPhoneNumber(request.getPhoneNumber());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentType(request.getPaymentType());
            payment.setMember(member);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setCurrency("XOF");

            Payment savedPayment = paymentRepository.save(payment);

            log.info("✅ Paiement créé: ID={}, TransactionId={}", savedPayment.getId(), transactionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", transactionId);
            response.put("paymentId", savedPayment.getId());
            response.put("amount", request.getAmount());
            response.put("phoneNumber", request.getPhoneNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur initiation paiement", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ✅ Vérification d'un paiement avec appel à l'API Kkiapay
     */
    @GetMapping("/verify/{transactionId}")
    public ResponseEntity<?> verifyPayment(@PathVariable String transactionId) {
        try {
            log.info("🔍 Vérification du paiement: {}", transactionId);

            // Vérifier si le paiement existe localement
            Payment localPayment = paymentRepository.findByTransactionId(transactionId).orElse(null);

            if (localPayment == null) {
                log.warn("⚠️ Transaction non trouvée: {}", transactionId);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("status", "NOT_FOUND");
                response.put("message", "Transaction non trouvée: " + transactionId);
                return ResponseEntity.ok(response);
            }

            log.info("📦 Paiement trouvé localement: ID={}, status={}", localPayment.getId(), localPayment.getStatus());

            // Appeler l'API Kkiapay pour vérifier le statut réel
            Map<String, Object> verificationResult = kkiapayService.verifyTransactionWithDetails(transactionId);

            log.info("📊 Résultat vérification Kkiapay: success={}, status={}",
                    verificationResult.get("success"), verificationResult.get("status"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", verificationResult.get("success"));
            response.put("status", verificationResult.get("status"));
            response.put("payment", verificationResult.get("payment"));
            response.put("transactionId", transactionId);
            response.put("amount", localPayment.getAmount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du paiement: {}", transactionId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Vérification simplifiée (sans appel API externe) - utile pour le débogage
     */
    @GetMapping("/verify-local/{transactionId}")
    public ResponseEntity<?> verifyPaymentLocal(@PathVariable String transactionId) {
        try {
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", payment != null);
            response.put("status", payment != null ? payment.getStatus().name() : "NOT_FOUND");
            response.put("payment", payment);
            response.put("transactionId", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> getMemberPayments(@PathVariable Long memberId) {
        try {
            return ResponseEntity.ok(paymentRepository.findByMemberId(memberId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/member/{memberId}/successful")
    public ResponseEntity<?> getMemberSuccessfulPayments(@PathVariable Long memberId) {
        try {
            return ResponseEntity.ok(paymentRepository.findByMemberIdAndStatus(memberId, PaymentStatus.SUCCESS));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    public static class PaymentInitiateRequest {
        private Long memberId;
        private BigDecimal amount;
        private String phoneNumber;
        private PaymentType paymentType;

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public PaymentType getPaymentType() { return paymentType; }
        public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    }
}