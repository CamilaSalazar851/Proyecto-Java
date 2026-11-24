package com.littlehorizons.erp.model;

/**
 * Summary of one CSV attendance import.
 *
 * @param total   data lines read (header and blank lines are not counted)
 * @param success lines inserted into the database
 * @param failed  lines rejected by a validation or by the insert (total = success + failed)
 */
public record AttendanceImportResult(int total, int success, int failed) {
}
