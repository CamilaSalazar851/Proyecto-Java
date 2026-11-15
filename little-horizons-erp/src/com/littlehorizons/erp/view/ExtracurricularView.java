package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMember;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.OperationCancelledException;

import java.util.List;

/** Console screens for clubs and school events. */
public class ExtracurricularView {

    private final ExtracurricularController controller;

    public ExtracurricularView(ExtracurricularController controller) {
        this.controller = controller;
    }

    public void showMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ACTIVIDADES EXTRACURRICULARES ===");
            System.out.println("(escriba \'cancelar\' en cualquier campo para volver al menu)");
            System.out.println("1. Ver catalogo de clubes");
            System.out.println("2. Crear club");
            System.out.println("3. Inscribir estudiante en un club");
            System.out.println("4. Retirar estudiante de un club");
            System.out.println("5. Ver miembros de un club");
            System.out.println("6. Ver calendario de eventos");
            System.out.println("7. Crear evento");
            System.out.println("8. Registrar participante en un evento");
            System.out.println("9. Ver participantes de un evento");
            System.out.println("10. Editar un club");
            System.out.println("11. Desactivar un club");
            System.out.println("12. Desactivar un evento");
            System.out.println("13. Volver al menu principal");

            int option = ConsoleInput.readInt("Seleccione una opcion: ", 1, 13);
            try {
                switch (option) {
                    case 1 -> printClubs();
                    case 2 -> createClub();
                    case 3 -> joinClub();
                    case 4 -> leaveClub();
                    case 5 -> printMembers();
                    case 6 -> printEvents();
                    case 7 -> createEvent();
                    case 8 -> addParticipant();
                    case 9 -> printParticipants();
                    case 10 -> editClub();
                    case 11 -> deactivateClub();
                    case 12 -> deactivateEvent();
                    default -> back = true;
                }
            } catch (OperationCancelledException e) {
                System.out.println("\nOperacion cancelada. Volviendo al menu.");
            } catch (AppException e) {
                System.out.println("\n[ERROR] " + e.getMessage());
            }
            if (!back) {
                ConsoleInput.pause();
            }
        }
    }

    private void editClub() {
        printClubs();
        int clubId = ConsoleInput.readInt("\nID del club a editar: ", 1, Integer.MAX_VALUE);
        Club club = controller.requireClub(clubId);
        System.out.println("\nEditando " + club.name() + " (deje en blanco lo que no quiera cambiar).");
        String description = ConsoleInput.readOptional("Nueva descripcion", 500);
        Integer supervisorId = ConsoleInput.readOptionalInt("ID del nuevo profesor encargado", 1, Integer.MAX_VALUE);
        Integer capacity = ConsoleInput.readOptionalInt("Nuevo cupo maximo (1-200)", 1, 200);
        String schedule = ConsoleInput.readOptional("Nuevo horario de reunion", 100);
        controller.updateClub(clubId, description, supervisorId, capacity, schedule);
        System.out.println("\n[OK] Club actualizado.");
    }

    private void deactivateClub() {
        int clubId = ConsoleInput.readInt("ID del club a desactivar: ", 1, Integer.MAX_VALUE);
        if (ConsoleInput.confirm("Desactivar el club? Dejara de aparecer en el catalogo")) {
            controller.deactivateClub(clubId);
            System.out.println("\n[OK] Club desactivado.");
        }
    }

    private void deactivateEvent() {
        int eventId = ConsoleInput.readInt("ID del evento a desactivar: ", 1, Integer.MAX_VALUE);
        if (ConsoleInput.confirm("Desactivar el evento? Dejara de aparecer en el calendario")) {
            controller.deactivateEvent(eventId);
            System.out.println("\n[OK] Evento desactivado.");
        }
    }

    private void printClubs() {
        List<Club> clubs = controller.listClubs();
        System.out.printf("%n%-4s %-32s %-22s %-9s %s%n", "ID", "Club", "Profesor", "Cupo", "Horario");
        System.out.println("-".repeat(95));
        clubs.forEach(c -> System.out.printf("%-4d %-32s %-22s %-9s %s%n", c.id(), c.name(), c.supervisor(),
                c.activeMembers() + "/" + c.capacity() + (c.isFull() ? "*" : ""),
                c.schedule() == null ? "-" : c.schedule()));
        System.out.println("\n(*) club lleno");
    }

    private void createClub() {
        String name = ConsoleInput.readRequired("Nombre del club: ", 100);
        String description = ConsoleInput.readOptional("Descripcion", 500);
        int supervisorId = ConsoleInput.readInt("ID del profesor encargado: ", 1, Integer.MAX_VALUE);
        int capacity = ConsoleInput.readInt("Cupo maximo (1-200): ", 1, 200);
        String schedule = ConsoleInput.readOptional("Horario de reunion", 100);
        System.out.println("\n[OK] Club creado con ID "
                + controller.createClub(name, description, supervisorId, capacity, schedule) + ".");
    }

    private void joinClub() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        printClubs();
        int clubId = ConsoleInput.readInt("\nID del club: ", 1, Integer.MAX_VALUE);
        controller.joinClub(studentId, clubId);
        System.out.println("\n[OK] Estudiante inscrito en el club.");
    }

    private void leaveClub() {
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        int clubId = ConsoleInput.readInt("ID del club: ", 1, Integer.MAX_VALUE);
        String reason = ConsoleInput.readOptional("Motivo del retiro", 255);
        controller.leaveClub(studentId, clubId, reason);
        System.out.println("\n[OK] Estudiante retirado del club.");
    }

    private void printMembers() {
        int clubId = ConsoleInput.readInt("ID del club: ", 1, Integer.MAX_VALUE);
        Club club = controller.requireClub(clubId);
        List<ClubMember> members = controller.listMembers(clubId);
        System.out.println("\nMiembros de " + club.name() + ":");
        System.out.printf("%-5s %-32s %-12s %s%n", "ID", "Estudiante", "Ingreso", "Estado");
        members.forEach(m -> System.out.printf("%-5d %-32s %-12s %s%n", m.studentId(), m.studentName(),
                m.joinDate(), m.status()));
    }

    private void printEvents() {
        List<SchoolEvent> events = controller.listEvents();
        System.out.printf("%n%-4s %-30s %-12s %-12s %-22s %s%n", "ID", "Evento", "Tipo", "Fecha", "Lugar", "Competencia");
        System.out.println("-".repeat(98));
        events.forEach(e -> System.out.printf("%-4d %-30s %-12s %-12s %-22s %s%n", e.id(), e.name(), e.type(),
                e.date(), e.place() == null ? "-" : e.place(), e.competition() ? "Si" : "No"));
    }

    private void createEvent() {
        String name = ConsoleInput.readRequired("Nombre del evento: ", 150);
        String description = ConsoleInput.readOptional("Descripcion", 500);
        String type = ConsoleInput.readChoice("Tipo de evento:", SchoolEvent.TYPES);
        var date = ConsoleInput.readDate("Fecha del evento");
        String place = ConsoleInput.readOptional("Lugar", 150);
        boolean competition = ConsoleInput.confirm("El evento tiene competencia con posiciones?");
        int organizerId = ConsoleInput.readInt("ID del profesor organizador: ", 1, Integer.MAX_VALUE);
        System.out.println("\n[OK] Evento creado con ID "
                + controller.createEvent(name, description, type, date, place, competition, organizerId) + ".");
    }

    private void addParticipant() {
        printEvents();
        int eventId = ConsoleInput.readInt("\nID del evento: ", 1, Integer.MAX_VALUE);
        SchoolEvent event = controller.requireEvent(eventId);
        int studentId = ConsoleInput.readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
        String position = event.competition()
                ? ConsoleInput.readChoice("Posicion obtenida:", SchoolEvent.POSITIONS) : null;
        controller.addParticipant(eventId, studentId, position);
        System.out.println("\n[OK] Participante registrado.");
    }

    private void printParticipants() {
        int eventId = ConsoleInput.readInt("ID del evento: ", 1, Integer.MAX_VALUE);
        SchoolEvent event = controller.requireEvent(eventId);
        List<EventParticipant> participants = controller.listParticipants(eventId);
        System.out.println("\nParticipantes de " + event.name() + ":");
        System.out.printf("%-5s %-32s %-12s %s%n", "ID", "Estudiante", "Rol", "Posicion");
        participants.forEach(p -> System.out.printf("%-5d %-32s %-12s %s%n", p.studentId(), p.studentName(),
                p.role(), p.position() == null ? "-" : p.position()));
        System.out.println("\nTotal: " + participants.size());
    }
}
