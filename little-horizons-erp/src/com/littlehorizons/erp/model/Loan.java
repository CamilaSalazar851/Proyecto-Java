package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A loan of a resource. borrowerType is "Estudiante" or "Profesor". */
public record Loan(int id, String borrowerType, String resourceCode, String resourceName,
                   String borrowerName, LocalDate loanDate, LocalDate dueDate,
                   LocalDate returnDate, String status) {
}
