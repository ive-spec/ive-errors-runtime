package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 422. Ej.: "422_Unprocessable". */
public class UnprocessableException extends IveBusinessException {
    private static final long serialVersionUID = 3973587867576369721L;
	private final String errorRef;

    public UnprocessableException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
