package com.littlehorizons.erp.model;

/** Guardian (acudiente). An id of 0 means the guardian is not stored yet. */
public record Guardian(int id, String documentType, String documentNumber, String firstName,
                       String firstLastName, String secondLastName, String relationship,
                       String phone, String email, String address) {

    public String fullName() {
        return firstName + " " + firstLastName + (secondLastName == null ? "" : " " + secondLastName);
    }
}
