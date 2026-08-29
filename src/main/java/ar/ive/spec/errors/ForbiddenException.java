package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 403. */
public class ForbiddenException extends IveBusinessException {
    private static final long serialVersionUID = 1L;
    /** La clave del catalogo para esta familia. Ver IveBusinessException. */
    public static final String REF = "403_Forbidden";

    private final String errorRef;

    public ForbiddenException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    /** Con el ref canonico de la familia. */
    public ForbiddenException(String message) {
        this(REF, message);
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
