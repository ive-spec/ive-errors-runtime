package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Familia HTTP 422 - forma RICA (DetailedErrorView), no la simple que
 * tenía antes - confirmado contra el catálogo real
 * (platform-errors-and-views.yaml): 422_UnprocessableContent apunta a
 * DetailedErrorView, igual que 400 - son los DOS únicos códigos con
 * esta forma, no solo 400 como decía un comentario desactualizado de
 * ValidationException.
 */
public class UnprocessableException extends IveBusinessException {
    private static final long serialVersionUID = 1L;
    /** La clave del catalogo para esta familia. Ver IveBusinessException. */
    public static final String REF = "422_UnprocessableContent";

    private final String errorRef;
    private final List<Message> messages;
    private final Map<String, List<FieldError>> fieldErrors;

    public UnprocessableException(String errorRef, String message, List<Message> messages, Map<String, List<FieldError>> fieldErrors) {
        super(message);
        this.errorRef = errorRef;
        this.messages = messages;
        this.fieldErrors = fieldErrors;
    }

    /** Con el ref canonico de la familia y sin detalle por campo. */
    public UnprocessableException(String message) {
        this(REF, message, List.of(), Map.of());
    }

    /**
     * Desde el cuerpo crudo, igual que en 400: son las DOS familias que
     * el catalogo declara con DetailedErrorView, asi que las dos tienen
     * detalle por campo que perder si no se parsea.
     *
     * Los records `Message`/`FieldError` de esta clase y los de
     * `BadRequestException` son tipos DISTINTOS aunque tengan la misma
     * forma, asi que el mapeo va explicito: convertirlos es de acá y no
     * del que parsea.
     */
    public static UnprocessableException fromBody(String errorRef, String rawBody) {
        return new UnprocessableException(
            errorRef,
            DetailedErrorBody.messageOf(rawBody),
            DetailedErrorBody.messagesOf(rawBody).stream()
                .map(m -> new Message(m.code(), m.message(), m.severity()))
                .toList(),
            DetailedErrorBody.fieldErrorsOf(rawBody).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream()
                        .map(f -> new FieldError(f.code(), f.message(), f.params()))
                        .toList(),
                    (a, b) -> a,
                    LinkedHashMap::new
                ))
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
