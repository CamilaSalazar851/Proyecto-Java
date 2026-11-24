package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/** Domain entity for a student (table: estudiantes). */
public class Student {

    public static final List<String> DOCUMENT_TYPES = List.of("TI", "CC", "CE", "Pasaporte");
    public static final List<String> GENDERS = List.of("M", "F", "Otro");
    public static final List<String> STATUSES = List.of("Activo", "Graduado", "Retirado", "Suspendido");

    private int id;
    private String documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstLastName;
    private String secondLastName;
    private LocalDate birthDate;
    private String gender;
    private String address;
    private String phone;
    private String email;
    private LocalDate admissionDate;
    private String status;

    public Student() {
    }

    public String getFullName() {
        StringBuilder name = new StringBuilder(firstName);
        if (middleName != null) {
            name.append(' ').append(middleName);
        }
        name.append(' ').append(firstLastName);
        if (secondLastName != null) {
            name.append(' ').append(secondLastName);
        }
        return name.toString();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getFirstLastName() { return firstLastName; }
    public void setFirstLastName(String firstLastName) { this.firstLastName = firstLastName; }

    public String getSecondLastName() { return secondLastName; }
    public void setSecondLastName(String secondLastName) { this.secondLastName = secondLastName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
