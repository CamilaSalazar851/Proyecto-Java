package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AttendanceImportDao;
import com.littlehorizons.erp.dao.AttendanceImportDao.AttendanceWriter;
import com.littlehorizons.erp.model.AttendanceImportResult;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;

/**
 * Business rules of the CSV attendance import. The file is processed line by line: a valid line is
 * inserted right away, an invalid one is only counted as failed and the next line is processed.
 */
public class AttendanceImportController {

    public static final String FILE_NAME = "asistencias_importar.csv";
    public static final String HEADER = "id_estudiante;id_materia;fecha;estado";

    private static final Set<String> VALID_STATES = Set.of("Presente", "Ausente", "Tardanza", "Excusa Médica");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final int FIELD_COUNT = 4;

    private final AttendanceImportDao dao;

    public AttendanceImportController(AttendanceImportDao dao) {
        this.dao = dao;
    }

    /** Imports asistencias_importar.csv from the project root folder. */
    public AttendanceImportResult importFromCsv() {
        return importFromCsv(Path.of(FILE_NAME));
    }

    /**
     * Imports the given file.
     *
     * @throws AppException with the message to show when the file is missing or the header is wrong
     */
    public AttendanceImportResult importFromCsv(Path file) {
        // Step 1: the file must exist
        if (!Files.isRegularFile(file)) {
            throw new AppException("Error: El archivo '" + FILE_NAME
                    + "' no se encuentra en la carpeta raíz del proyecto.");
        }

        try (BufferedReader reader = openReader(file)) {
            // Step 2: the header must be exactly the expected one
            String header = reader.readLine();
            if (header != null && header.startsWith("\uFEFF")) {
                header = header.substring(1); // UTF-8 byte order mark added by Excel / Notepad
            }
            if (!HEADER.equals(header)) {
                throw new AppException("Error: El archivo no tiene el formato correcto. "
                        + "La cabecera debe ser: " + HEADER);
            }

            Set<Integer> studentIds = dao.findStudentIds();
            Set<Integer> subjectIds = dao.findSubjectIds();

            // Step 3: every data line is independent
            int total = 0;
            int success = 0;
            try (AttendanceWriter writer = dao.openWriter()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue; // blank lines are ignored and not counted
                    }
                    total++;
                    if (processLine(line, studentIds, subjectIds, writer)) {
                        success++;
                    }
                }
            }
            AttendanceImportResult result = new AttendanceImportResult(total, success, total - success);
            AppLogger.info("Attendance import finished: total=" + total + ", ok=" + success
                    + ", failed=" + result.failed());
            return result;
        } catch (IOException e) {
            AppLogger.error("Could not read " + file, e);
            throw new AppException("Error: No se pudo leer el archivo '" + FILE_NAME + "'.", e);
        } catch (SQLException e) {
            AppLogger.error("Database error during attendance import", e);
            throw new AppException("Error: No se pudo conectar con la base de datos para importar.", e);
        }
    }

    /** Validates one line and inserts it. Returns true only when the row ended up in the database. */
    private boolean processLine(String line, Set<Integer> studentIds, Set<Integer> subjectIds,
                                AttendanceWriter writer) {
        try {
            // 3.2 exactly 4 fields (limit -1 keeps empty trailing fields, so "1;2;3;4;" has 5)
            String[] fields = line.split(";", -1);
            if (fields.length != FIELD_COUNT) {
                return false;
            }
            int studentId = Integer.parseInt(fields[0].trim());
            int subjectId = Integer.parseInt(fields[1].trim());
            // 3.3 and 3.4 the student and the subject must exist
            if (!studentIds.contains(studentId) || !subjectIds.contains(subjectId)) {
                return false;
            }
            // 3.5 a real date in YYYY-MM-DD (rejects 2024-02-30, 2024-13-01, 24-1-7, ...)
            LocalDate date = LocalDate.parse(fields[2].trim(), DATE_FORMAT);
            // 3.6 exact state: case, spaces and accents matter
            String status = fields[3];
            if (!VALID_STATES.contains(status)) {
                return false;
            }
            // 3.7 insert
            return writer.insert(studentId, subjectId, date, status);
        } catch (NumberFormatException | DateTimeParseException e) {
            return false;
        } catch (RuntimeException e) {
            AppLogger.error("Unexpected error importing line: " + line, e);
            return false;
        }
    }

    /** UTF-8 reader that never throws on bad bytes (they become a replacement character). */
    private static BufferedReader openReader(Path file) throws IOException {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new BufferedReader(new InputStreamReader(Files.newInputStream(file), decoder));
    }
}
