package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Category;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.AppException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.JdbcHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Business rules for the resource inventory and the loan system. */
public class ResourceController {

    private final ResourceDao resourceDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ResourceController(ResourceDao resourceDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.resourceDao = resourceDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    public List<Category> listCategories() {
        try {
            return resourceDao.findCategories();
        } catch (SQLException e) {
            AppLogger.error("Error listing resource categories", e);
            throw new AppException("No se pudo consultar las categorias.", e);
        }
    }

    public List<Resource> listResources(String statusOrNull) {
        try {
            return resourceDao.findResources(statusOrNull);
        } catch (SQLException e) {
            AppLogger.error("Error listing resources", e);
            throw new AppException("No se pudo consultar el inventario.", e);
        }
    }

    public int registerResource(String code, String name, int categoryId, String description, String location,
                                LocalDate acquisitionDate) {
        try {
            if (resourceDao.codeExists(code)) {
                throw new AppException("Ya existe un recurso con ese codigo.");
            }
            if (listCategories().stream().noneMatch(c -> c.id() == categoryId)) {
                throw new AppException("No existe una categoria con ID " + categoryId + ".");
            }
            return resourceDao.insertResource(code, name, categoryId, description, location, acquisitionDate);
        } catch (SQLException e) {
            AppLogger.error("Error registering resource " + code, e);
            throw new AppException("No se pudo registrar el recurso.", e);
        }
    }

    public void updateResource(int resourceId, String name, String location, String description) {
        try {
            if (!resourceDao.updateResource(resourceId, name, location, description)) {
                throw new AppException("No existe un recurso con ID " + resourceId + ".");
            }
        } catch (SQLException e) {
            AppLogger.error("Error updating resource " + resourceId, e);
            throw new AppException("No se pudo actualizar el recurso.", e);
        }
    }

    /** Maintenance, retirement or reactivation. A borrowed resource must be returned first. */
    public void changeResourceStatus(int resourceId, String newStatus) {
        try {
            JdbcHelper.inTransaction(conn -> {
                String current = resourceDao.lockResourceStatus(conn, resourceId).orElseThrow(
                        () -> new AppException("No existe un recurso con ID " + resourceId + "."));
                if ("Prestado".equals(current)) {
                    throw new AppException("El recurso esta prestado. Registre primero su devolucion.");
                }
                resourceDao.setResourceStatus(conn, resourceId, newStatus);
                return null;
            });
        } catch (SQLException e) {
            AppLogger.error("Error changing status of resource " + resourceId, e);
            throw new AppException("No se pudo cambiar el estado del recurso.", e);
        }
    }

    /** Lends a resource to a student ("Estudiante") or a teacher ("Profesor"). */
    public void lendResource(String borrowerType, int borrowerId, int resourceId, int days) {
        try {
            boolean student = "Estudiante".equals(borrowerType);
            if (student) {
                Student s = studentDao.findById(borrowerId).orElseThrow(
                        () -> new AppException("No existe un estudiante con ID " + borrowerId + "."));
                if (!"Activo".equals(s.getStatus())) {
                    throw new AppException("El estudiante no esta activo.");
                }
            } else {
                Teacher t = teacherDao.findById(borrowerId).orElseThrow(
                        () -> new AppException("No existe un profesor con ID " + borrowerId + "."));
                if (!"Activo".equals(t.status())) {
                    throw new AppException("El profesor no esta activo.");
                }
            }
            LocalDate today = LocalDate.now();
            JdbcHelper.inTransaction(conn -> {
                String status = resourceDao.lockResourceStatus(conn, resourceId).orElseThrow(
                        () -> new AppException("No existe un recurso con ID " + resourceId + "."));
                if (!"Disponible".equals(status)) {
                    throw new AppException("El recurso no esta disponible (estado: " + status + ").");
                }
                if (student) {
                    resourceDao.insertStudentLoan(conn, resourceId, borrowerId, today, today.plusDays(days));
                } else {
                    resourceDao.insertTeacherLoan(conn, resourceId, borrowerId, today, today.plusDays(days));
                }
                resourceDao.setResourceStatus(conn, resourceId, "Prestado");
                return null;
            });
        } catch (SQLException e) {
            AppLogger.error("Error lending resource " + resourceId, e);
            throw new AppException("No se pudo registrar el prestamo.", e);
        }
    }

    /** Registers the return: closes the loan with today's date and frees the resource. */
    public void returnResource(String borrowerType, int loanId) {
        try {
            boolean student = "Estudiante".equals(borrowerType);
            JdbcHelper.inTransaction(conn -> {
                int resourceId = resourceDao.closeLoan(conn, student, loanId).orElseThrow(
                        () -> new AppException("No existe un prestamo abierto con ese ID."));
                resourceDao.setResourceStatus(conn, resourceId, "Disponible");
                return null;
            });
        } catch (SQLException e) {
            AppLogger.error("Error returning loan " + loanId, e);
            throw new AppException("No se pudo registrar la devolucion.", e);
        }
    }

    public List<Loan> openLoans() {
        try {
            resourceDao.markOverdueLoans();
            return resourceDao.findOpenLoans();
        } catch (SQLException e) {
            AppLogger.error("Error listing open loans", e);
            throw new AppException("No se pudo consultar los prestamos activos.", e);
        }
    }

    public List<Loan> overdueLoans() {
        try {
            resourceDao.markOverdueLoans();
            return resourceDao.findOverdueLoans();
        } catch (SQLException e) {
            AppLogger.error("Error listing overdue loans", e);
            throw new AppException("No se pudo consultar los prestamos vencidos.", e);
        }
    }
}
