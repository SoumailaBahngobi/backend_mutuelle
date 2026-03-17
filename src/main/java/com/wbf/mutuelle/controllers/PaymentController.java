package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Payment;
import com.wbf.mutuelle.PaymentStatus;
import com.wbf.mutuelle.PaymentType;
import com.wbf.mutuelle.entities.Member;
import com.wbf.mutuelle.repositories.MemberRepository;
import com.wbf.mutuelle.repositories.PaymentRepository;
import com.wbf.mutuelle.services.KkiapayService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/mutuelle/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentInitiateRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        try {
            // Récupérer l'email depuis le token
            String email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            // Récupérer le membre
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

            // Générer un ID de transaction unique
            String transactionId = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);

            // Créer le paiement en base de données
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

            // Retourner l'ID de transaction
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", transactionId);
            response.put("paymentId", savedPayment.getId());
            response.put("amount", request.getAmount());
            response.put("phoneNumber", request.getPhoneNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/verify/{transactionId}")
    public ResponseEntity<?> verifyPayment(@PathVariable String transactionId) {
        try {
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", payment.getStatus());
            response.put("payment", payment);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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