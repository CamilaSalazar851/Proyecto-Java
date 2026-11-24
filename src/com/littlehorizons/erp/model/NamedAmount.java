package com.littlehorizons.erp.model;

import java.math.BigDecimal;

public record NamedAmount(String name, BigDecimal total, long count) {
}
