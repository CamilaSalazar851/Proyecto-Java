package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record Delinquent(int studentId, String studentName, String guardianName, String guardianPhone,
                         LocalDate lastPensionDate) {
}
