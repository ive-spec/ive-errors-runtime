package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 409. Ej.: "409_AlreadyProcessed". */
public class ConflictException extends IveBusinessException {
    private static final long serialVersionUID = 9133782503748442103L;
	private final String errorRef;

    public ConflictException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
