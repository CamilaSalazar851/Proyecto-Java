package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

public record SchoolEvent(int id, String name, String type, LocalDate date, String place, boolean competition) {

    public static final List<String> TYPES = List.of("Académico", "Cultural", "Deportivo", "Social", "Científico");
    public static final List<String> POSITIONS = List.of("1er Lugar", "2do Lugar", "3er Lugar", "Participante");
}
