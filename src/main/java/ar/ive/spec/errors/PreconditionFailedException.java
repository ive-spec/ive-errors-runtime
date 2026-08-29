package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 412. */
public class PreconditionFailedException extends IveBusinessException {
    private static final long serialVersionUID = 1L;
    /** La clave del catalogo para esta familia. Ver IveBusinessException. */
    public static final String REF = "412_PreconditionFailed";

    private final String errorRef;

    public PreconditionFailedException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    /** Con el ref canonico de la familia. */
    public PreconditionFailedException(String message) {
        this(REF, message);
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
