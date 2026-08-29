package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 405. */
public class MethodNotAllowedException extends IveBusinessException {
    private static final long serialVersionUID = 1L;
    /** La clave del catalogo para esta familia. Ver IveBusinessException. */
    public static final String REF = "405_MethodNotAllowed";

    private final String errorRef;

    public MethodNotAllowedException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    /** Con el ref canonico de la familia. */
    public MethodNotAllowedException(String message) {
        this(REF, message);
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
