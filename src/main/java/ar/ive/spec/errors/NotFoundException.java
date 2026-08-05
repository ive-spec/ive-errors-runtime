package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/** Familia HTTP 404. Ej.: "404_NotFound", "404_AsientoNoDisponible", etc. */
public class NotFoundException extends IveBusinessException {
    private static final long serialVersionUID = -5549326345177517718L;
	private final String errorRef;

    public NotFoundException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
