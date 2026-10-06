package ar.ive.spec.errors;

import java.util.Map;

/**
 * What was wrong in ONE field, said by whoever rejects -- the per-field
 * detail a cause of a family with {@code DetailedErrorView} (400 and 422)
 * carries.
 *
 * <p>It is the INPUT of {@link GuidanceTable#errorFor(String, Integer, Map)}
 * and not the type the error keeps: each detailed family declares its own
 * {@code FieldError} record (a generated backend builds them), and one
 * type for every family is what lets a service reject for a declared cause
 * without knowing which family that cause belongs to. The factory turns it
 * into the family's own record.</p>
 *
 * @param key     what is wrong, as a stable key (e.g. {@code notOfThisStep}); the body's {@code key}
 * @param message a text for a person; {@code null} when the key says it all
 * @param params  the values that explain it; never {@code null}
 */
public record FieldProblem(String key, String message, Map<String, Object> params) {

    public FieldProblem {
        params = params == null ? Map.of() : params;
    }

    /** A problem with no text and no params. */
    public static FieldProblem of(String key) {
        return new FieldProblem(key, null, Map.of());
    }
}
