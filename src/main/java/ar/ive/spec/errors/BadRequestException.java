package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

import java.util.List;
import java.util.Map;

/**
 * Family HTTP 400 - the RICH form (DetailedErrorView): besides its cause
 * ({@code condition}, the body's {@code key}) it carries
 * {@code fieldErrors}, what was wrong in each field. Renamed from
 * ValidationException: the other classes of this hierarchy are named
 * after the HTTP code (Unauthorized/NotFound/Conflict...), not after a
 * meaning, and this one follows the same rule.
 */
public class BadRequestException extends IveBusinessException {
    private static final long serialVersionUID = 2L;
    /** The catalog key of this family. See IveBusinessException. */
    public static final String REF = "400_BadRequest";

    private final String errorRef;
    private final Map<String, List<FieldError>> fieldErrors;

    public BadRequestException(String errorRef, String message, Map<String, List<FieldError>> fieldErrors) {
        super(message);
        this.errorRef = errorRef;
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors;
    }

    /** With the family's canonical ref and no per-field detail. */
    public BadRequestException(String message) {
        this(REF, message, Map.of());
    }

    /**
     * From the raw body of an error response, as a generated SDK client
     * receives it: the per-field detail the server sent arrives typed
     * instead of as a loose String. A body that is not understood does not
     * fail: it stays as the {@code message}, with no field errors.
     */
    public static BadRequestException fromBody(String errorRef, String rawBody) {
        return new BadRequestException(
            errorRef,
            DetailedErrorBody.messageOf(rawBody, REF),
            DetailedErrorBody.fieldErrorsOf(rawBody)
        );
    }

    @Override
    public String errorRef() {
        return errorRef;
    }

    /** What was wrong in each field; empty when nothing was said per field. */
    public Map<String, List<FieldError>> fieldErrors() {
        return fieldErrors;
    }

    /**
     * One problem of one field (ErrorMessageView): its cause ({@code key}),
     * its text and the values it was checked against ({@code params}).
     */
    public record FieldError(String key, String message, Map<String, Object> params) {}
}
