package com.wbf.mutuelle.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RepaymentPaymentRequest {
    private BigDecimal amountPaid;
    private String paymentMethod;
    private String transactionReference;
}
