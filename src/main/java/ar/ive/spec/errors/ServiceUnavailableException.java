package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 503. Ej.: "503_ServiceUnavailable". */
public class ServiceUnavailableException extends IveBusinessException {
    private static final long serialVersionUID = 407424173808546728L;
	private final String errorRef;

    public ServiceUnavailableException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
