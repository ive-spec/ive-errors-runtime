package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 504. */
public class GatewayTimeoutException extends IveBusinessException {
    private static final long serialVersionUID = 1L;
    /** La clave del catalogo para esta familia. Ver IveBusinessException. */
    public static final String REF = "504_GatewayTimeout";

    private final String errorRef;

    public GatewayTimeoutException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    /** Con el ref canonico de la familia. */
    public GatewayTimeoutException(String message) {
        this(REF, message);
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
