package com.littlehorizons.erp.model;

import java.math.BigDecimal;

public record PaymentConcept(int id, String code, String name, BigDecimal baseAmount) {
}
