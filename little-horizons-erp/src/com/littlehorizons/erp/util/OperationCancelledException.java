package com.littlehorizons.erp.util;

/** Thrown when the user types "cancelar" in any input field to go back to the previous menu. */
public class OperationCancelledException extends AppException {

    private static final long serialVersionUID = 1L;

    public OperationCancelledException() {
        super("Operacion cancelada.");
    }
}
