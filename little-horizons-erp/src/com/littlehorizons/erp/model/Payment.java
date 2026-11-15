package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Payment(int id, String receipt, int studentId, String studentName, String concept,
                      BigDecimal amount, LocalDate date, String method, String period) {

    public static final List<String> METHODS =
            List.of("Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito", "Cheque");
    public static final List<String> PERIODS = List.of("I", "II", "III", "IV", "Anual");
}
