package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Family HTTP 422 - the RICH form (DetailedErrorView), like 400: the
 * catalog declares both with that View, and they are the only two. The
 * message meets the contract and the business rejects it anyway; its
 * {@code fieldErrors} say which field the rejection is about.
 */
public class UnprocessableException extends IveBusinessException {
    private static final long serialVersionUID = 2L;
    /** The catalog key of this family. See IveBusinessException. */
    public static final String REF = "422_UnprocessableContent";

    private final String errorRef;
    private final Map<String, List<FieldError>> fieldErrors;

    public UnprocessableException(String errorRef, String message, Map<String, List<FieldError>> fieldErrors) {
        super(message);
        this.errorRef = errorRef;
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors;
    }

    /** With the family's canonical ref and no per-field detail. */
    public UnprocessableException(String message) {
        this(REF, message, Map.of());
    }

    /**
     * From the raw body, as in 400: the two families the catalog declares
     * with DetailedErrorView both have per-field detail to lose if it is
     * not parsed.
     *
     * <p>This class's {@code FieldError} and {@code BadRequestException}'s
     * are DIFFERENT types with the same shape, so the mapping is explicit:
     * converting them is this class's job, not the parser's.</p>
     */
    public static UnprocessableException fromBody(String errorRef, String rawBody) {
        return new UnprocessableException(
            errorRef,
            DetailedErrorBody.messageOf(rawBody, REF),
            DetailedErrorBody.fieldErrorsOf(rawBody).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream()
                        .map(f -> new FieldError(f.key(), f.message(), f.params()))
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
