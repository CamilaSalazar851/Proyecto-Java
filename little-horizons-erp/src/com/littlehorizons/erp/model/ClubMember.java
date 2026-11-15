package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record ClubMember(int studentId, String studentName, LocalDate joinDate, String status) {
}
