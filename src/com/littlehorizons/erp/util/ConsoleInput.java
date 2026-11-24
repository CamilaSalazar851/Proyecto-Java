package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Validated console input. Every method keeps asking until the value is valid,
 * so views never receive bad data.
 */
public final class ConsoleInput {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String CANCEL_WORD = "cancelar";

    /** Reads one trimmed line; typing "cancelar" aborts the current operation. */
    private static String readLine() {
        String line = SCANNER.nextLine().trim();
        if (line.equalsIgnoreCase(CANCEL_WORD)) {
            throw new OperationCancelledException();
        }
        return line;
    }

    private ConsoleInput() {
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = readLine();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("  [!] Ingrese un numero entre %d y %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Entrada invalida. Ingrese un numero entero.");
            }
        }
    }

    /** Required text: cannot be blank and cannot exceed maxLength. */
    public static String readRequired(String prompt, int maxLength) {
        while (true) {
            System.out.print(prompt);
            String line = readLine();
            if (line.isEmpty()) {
                System.out.println("  [!] Este campo es obligatorio.");
            } else if (line.length() > maxLength) {
                System.out.printf("  [!] Maximo %d caracteres.%n", maxLength);
            } else {
                return line;
            }
        }
    }

    /** Optional text: an empty answer returns null. */
    public static String readOptional(String prompt, int maxLength) {
        while (true) {
            System.out.print(prompt + " (Enter para omitir): ");
            String line = readLine();
            if (line.isEmpty()) {
                return null;
            }
            if (line.length() <= maxLength) {
                return line;
            }
            System.out.printf("  [!] Maximo %d caracteres.%n", maxLength);
        }
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (AAAA-MM-DD): ");
            String line = readLine();
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Fecha invalida. Use el formato AAAA-MM-DD, por ejemplo 2015-03-24.");
            }
        }
    }

    /** Shows the options as a numbered list and returns the chosen one. */
    public static String readChoice(String prompt, List<String> options) {
        System.out.println(prompt);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, options.get(i));
        }
        int choice = readInt("  Seleccione una opcion: ", 1, options.size());
        return options.get(choice - 1);
    }

    public static boolean confirm(String prompt) {
        while (true) {
            System.out.print(prompt + " (s/n): ");
            String line = readLine().toLowerCase();
            if (line.equals("s") || line.equals("si")) {
                return true;
            }
            if (line.equals("n") || line.equals("no")) {
                return false;
            }
            System.out.println("  [!] Responda con 's' o 'n'.");
        }
    }

    public static void pause() {
        System.out.print("\nPresione Enter para continuar...");
        SCANNER.nextLine();
    }

    /** Waits for Enter without printing any prompt (the caller prints its own message). */
    public static void waitForEnter() {
        SCANNER.nextLine();
    }

    public static LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt + " (HH:mm): ");
            String line = readLine();
            try {
                return LocalTime.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Hora invalida. Use el formato HH:mm, por ejemplo 07:30.");
            }
        }
    }

    /** Reads a grade between 0.0 and 5.0 with at most one decimal (accepts "4.5" or "4,5"). */
    public static double readGrade(String prompt) {
        while (true) {
            System.out.print(prompt + " (0.0 a 5.0): ");
            String line = readLine().replace(',', '.');
            try {
                BigDecimal value = new BigDecimal(line);
                if (value.scale() > 1 || value.compareTo(BigDecimal.ZERO) < 0
                        || value.compareTo(new BigDecimal("5.0")) > 0) {
                    System.out.println("  [!] La nota debe estar entre 0.0 y 5.0 con maximo un decimal.");
                } else {
                    return value.doubleValue();
                }
            } catch (NumberFormatException e) {
                System.out.println("  [!] Entrada invalida. Ejemplo: 4.5");
            }
        }
    }

    /** Reads a positive money amount in pesos. An empty answer returns null. */
    public static BigDecimal readMoneyOrDefault(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = readLine().replace(".", "").replace(",", ".");
            if (line.isEmpty()) {
                return null;
            }
            try {
                BigDecimal value = new BigDecimal(line);
                if (value.signum() > 0) {
                    return value;
                }
                System.out.println("  [!] El monto debe ser mayor que cero.");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Monto invalido. Ejemplo: 350000");
            }
        }
    }

    /** Edit helper: shows the current value and keeps it when the user just presses Enter. */
    public static String readKeep(String prompt, String current, int maxLength) {
        while (true) {
            System.out.print(prompt + " [" + (current == null ? "-" : current) + "] (Enter para mantener): ");
            String line = readLine();
            if (line.isEmpty()) {
                return current;
            }
            if (line.length() <= maxLength) {
                return line;
            }
            System.out.printf("  [!] Maximo %d caracteres.%n", maxLength);
        }
    }

    /** Optional integer: an empty answer returns null (meaning "do not change"). */
    public static Integer readOptionalInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (Enter para mantener): ");
            String line = readLine();
            if (line.isEmpty()) {
                return null;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("  [!] Ingrese un numero entre %d y %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Entrada invalida. Ingrese un numero entero.");
            }
        }
    }
}
