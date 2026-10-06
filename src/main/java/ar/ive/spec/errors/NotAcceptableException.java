package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

/**
 * Familia HTTP 406. Ej.: "406_NotAcceptable".
 *
 * Faltaba, y no era un hueco silencioso: `IveErrorFactory` ya la
 * registraba para el código 406, así que la librería entera no
 * compilaba. El catálogo de plataforma declara `406_NotAcceptable` con
 * ErrorView, o sea la forma SIMPLE — las ricas son solo 400 y
 * 422.
 */
public class NotAcceptableException extends IveBusinessException {
    private static final long serialVersionUID = 1L;

    /** La clave del catálogo para esta familia. Ver el encabezado de IveBusinessException. */
    public static final String REF = "406_NotAcceptable";

    private final String errorRef;

    public NotAcceptableException(String errorRef, String message) {
        super(message);
        this.errorRef = errorRef;
    }

    public NotAcceptableException(String message) {
        this(REF, message);
    }

    @Override
    public String errorRef() {
        return errorRef;
    }
}
