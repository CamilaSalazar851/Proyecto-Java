package com.littlehorizons.erp.model;

import java.util.List;

public record AcademicRecord(int studentId, String studentName, int subjectId, String subjectName,
                             int year, String period, double value) {

    public static final List<String> PERIODS = List.of("I", "II", "III", "IV", "Final");
}
