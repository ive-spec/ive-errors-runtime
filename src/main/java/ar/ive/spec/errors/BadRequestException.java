package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

import java.util.List;
import java.util.Map;

/**
 * Familia HTTP 400 - forma RICA (DetailedErrorView) - fieldErrors
 * (validación por campo) y messages (errores generales, sin campo
 * asociado). Renombrada desde ValidationException - las demás 15
 * clases de esta jerarquía llevan el nombre del código HTTP
 * (Unauthorized/NotFound/Conflict/etc.), no del significado semántico -
 * esta se alinea al mismo criterio.
 */
public class BadRequestException extends IveBusinessException {
    private static final long serialVersionUID = 1L;
    /** La clave del catalogo para esta familia. Ver IveBusinessException. */
    public static final String REF = "400_BadRequest";

    private final String errorRef;
    private final List<Message> messages;
    private final Map<String, List<FieldError>> fieldErrors;

    public BadRequestException(String errorRef, String message, List<Message> messages, Map<String, List<FieldError>> fieldErrors) {
        super(message);
        this.errorRef = errorRef;
        this.messages = messages;
        this.fieldErrors = fieldErrors;
    }

    /** Con el ref canonico de la familia y sin detalle por campo. */
    public BadRequestException(String message) {
        this(REF, message, List.of(), Map.of());
    }

    /**
     * Desde el cuerpo crudo de una respuesta de error, como lo recibe un
     * cliente de SDK generado. Es lo que hace que el detalle por campo
     * que el servidor mando llegue tipado en vez de quedar como un String
     * suelto. Un cuerpo que no se entiende no falla: queda como
     * `message`, con las dos colecciones vacias.
     */
    public static BadRequestException fromBody(String errorRef, String rawBody) {
        return new BadRequestException(
            errorRef,
            DetailedErrorBody.messageOf(rawBody),
            DetailedErrorBody.messagesOf(rawBody),
            DetailedErrorBody.fieldErrorsOf(rawBody)
        );
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
