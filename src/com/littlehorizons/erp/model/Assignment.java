package com.littlehorizons.erp.model;

public record Assignment(int id, int teacherId, String teacherName, int subjectId, String subjectName,
                         int year, String section, String schedule) {
}
