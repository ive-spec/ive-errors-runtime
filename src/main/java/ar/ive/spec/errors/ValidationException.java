package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

import java.util.List;
import java.util.Map;

/**
 * Familia HTTP 400 - completa el hueco que faltaba en FAMILY_BY_CODE.
 * A diferencia de las demás excepciones de esta familia (mensaje
 * simple), esta lleva la forma RICA de DetailedErrorView -
 * fieldErrors (validación por campo) y messages (errores generales,
 * sin campo asociado) - ya que 400_BadRequest es, en el catálogo, el
 * único código asociado a esa vista más detallada, no a
 * StandardErrorView.
 */
public class ValidationException extends IveBusinessException {
    private static final long serialVersionUID = 1L;
    private final String errorRef;
    private final List<Message> messages;
    private final Map<String, List<FieldError>> fieldErrors;

    public ValidationException(String errorRef, String message, List<Message> messages, Map<String, List<FieldError>> fieldErrors) {
        super(message);
        this.errorRef = errorRef;
        this.messages = messages;
        this.fieldErrors = fieldErrors;
    }

    @Override
    public String errorRef() {
        return errorRef;
    }

    public List<Message> messages() {
        return messages;
    }

    public Map<String, List<FieldError>> fieldErrors() {
        return fieldErrors;
    }

    public record Message(String code, String message, String severity) {}

    public record FieldError(String code, String message, Map<String, Object> params) {}
}
