package com.littlehorizons.erp.util;

import java.time.Year;

/** Application-wide constants (school identity used on invoices, current academic year). */
public final class AppConfig {

    public static final int CURRENT_YEAR = Year.now().getValue();

    public static final String SCHOOL_NAME = "Colegio Internacional Little Horizons S.A.S.";
    public static final String SCHOOL_NIT = "901.234.567-8";
    public static final String SCHOOL_ADDRESS = "Carrera 27 #45-12, Bucaramanga, Santander";
    public static final String SCHOOL_PHONE = "(607) 555-0123";
    public static final String SCHOOL_EMAIL = "facturacion@littlehorizons.edu.co";

    /** Simulated DIAN enabling resolution (not a real authorization). */
    public static final String DIAN_RESOLUTION =
            "Resolucion DIAN No. 18760000001 del 2026-01-02 - Rango FE-000001 a FE-999999 (SIMULADA)";

    public static final int MAX_ABSENCES_PER_WEEK = 3;
    public static final double SUPPORT_THRESHOLD = 3.0;

    private AppConfig() {
    }
}
