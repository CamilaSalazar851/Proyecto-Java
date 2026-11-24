package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

public record Teacher(int id, String documentType, String documentNumber, String firstName,
                      String firstLastName, String secondLastName, String email, String phone,
                      String specialization, LocalDate hireDate, String status) {

    public static final List<String> DOCUMENT_TYPES = List.of("CC", "CE", "Pasaporte");

    public String fullName() {
        return firstName + " " + firstLastName + (secondLastName == null ? "" : " " + secondLastName);
    }
}
