package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/**
 * Familia HTTP 401. Cubre cualquier error catalogado de una spec cuyo
 * prefijo numérico sea 401 (ej. "401_NotAuthenticated"). El `errorRef`
 * puntual de cada spec no es un tipo distinto - es un dato de runtime que
 * el Intent generado conoce y expone vía `errorRef()`.
 */
public class UnauthorizedException extends IveBusinessException {
    private static final long serialVersionUID = -5552957716434596102L;
	private final String errorRef;

    public UnauthorizedException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
