package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;
import ar.ive.spec.core.IveTransportException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * THE BODY OF AN ERROR RESPONSE: writing it, and reading back what it says.
 *
 * <p>The shape is the catalog's error Views: {@code ErrorView}
 * ({@code key}, {@code message}, {@code expects}) and, for the errors about
 * the data received (400 and 422), {@code DetailedErrorView}, which adds
 * {@code fieldErrors} ({@code {<field>: [{key, message, params}]}}).</p>
 *
 * <ul>
 *   <li>{@code key} is the cause of the error. Absent when the cause is
 *       not known: guessing it by the code would pick one of several causes
 *       that expect different things.</li>
 *   <li>{@code expects} is what the cause expects from whoever receives the
 *       error. It travels ALWAYS when the cause has one: any client, or an
 *       AI agent, gets it without a table of its own.</li>
 * </ul>
 *
 * <p>WHICH ERROR it is does not travel in the body: the transport says it
 * (the HTTP status). And an error whose entry declares no View answers
 * with no body at all -- that is the writer's decision, which knows the
 * specification; this class writes the body of one that has a View.</p>
 *
 * <p>Framework-agnostic on purpose: it returns a {@code Map} (any JSON
 * writer serializes it) and a status; the adapter to a web framework
 * (Spring's exception handler, ...) is the generated code's.</p>
 */
public final class ErrorBody {

    private ErrorBody() { }

    // --- writing -----------------------------------------------------------

    /** The body of a business error; {@code expects} from {@link Guidances#GUIDANCE}. */
    public static Map<String, Object> of(IveBusinessException error) {
        return of(error, Guidances.GUIDANCE);
    }

    /**
     * The body of a business error.
     *
     * <p>{@code expects} comes from the error when it carries one explicitly
     * (read from the body it came in), otherwise from {@code guidance}.</p>
     */
    public static Map<String, Object> of(IveBusinessException error, GuidanceTable guidance) {
        String expects = error.carriedExpects();
        if (expects == null) {
            expects = (guidance != null ? guidance : Guidances.GUIDANCE).expectsOf(error);
        }
        Map<String, Object> body = of(error.condition(), error.getMessage(), expects);
        Map<String, ?> fieldErrors = fieldErrorsOf(error);
        if (!fieldErrors.isEmpty()) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }

    /**
     * The body of a failure that is not an {@link IveBusinessException} (e.g.
     * a flow's {@code FlowFailureException}). {@code key} and {@code expects}
     * may be null: then they are not written.
     */
    public static Map<String, Object> of(String key, String message, String expects) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (key != null && !key.isBlank()) {
            body.put("key", key);
        }
        body.put("message", message);
        if (expects != null && !expects.isBlank()) {
            body.put("expects", expects);
        }
        return body;
    }

    /**
     * The status of a technical failure: its own code, or 502 when it has
     * none. NOT 500: a 500 says "I broke", and this is the opposite --
     * something this service depends on did not answer --. Whoever receives
     * it does different things with each.
     */
    public static int statusOf(IveTransportException error) {
        return error.code() > 0 ? error.code() : 502;
    }

    /** The body of a technical failure: {@code {message}} only -- it has no cause. */
    public static Map<String, Object> of(IveTransportException error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", error.getMessage());
        return body;
    }

    // --- reading -----------------------------------------------------------

    /** The {@code message} of a raw error body; the raw text when it is not one. */
    public static String messageOf(String rawBody) {
        return DetailedErrorBody.messageOf(rawBody);
    }

    /** The cause of a raw error body ({@code key}), or {@code null}: it is not guessed. */
    public static String keyOf(String rawBody) {
        return DetailedErrorBody.keyOf(rawBody);
    }

    /** The {@code expects} of a raw error body, or {@code null}. */
    public static String expectsOf(String rawBody) {
        return DetailedErrorBody.expectsOf(rawBody);
    }

    private static Map<String, ?> fieldErrorsOf(IveBusinessException error) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (error instanceof BadRequestException bad) {
            bad.fieldErrors().forEach((field, errors) -> result.put(field,
                errors.stream().map(e -> fieldError(e.key(), e.message(), e.params())).toList()));
        } else if (error instanceof UnprocessableException unprocessable) {
            unprocessable.fieldErrors().forEach((field, errors) -> result.put(field,
                errors.stream().map(e -> fieldError(e.key(), e.message(), e.params())).toList()));
        }
        return result;
    }

    /**
     * One problem of one field ({@code ErrorMessageView}): its key, its text,
     * its params. The View requires a message: a problem raised without one
     * (its key says it all, see {@link FieldProblem}) carries its key there.
     */
    private static Map<String, Object> fieldError(String key, String message, Map<String, Object> params) {
        Map<String, Object> entry = new LinkedHashMap<>();
        if (key != null && !key.isBlank()) {
            entry.put("key", key);
        }
        entry.put("message", message != null ? message : key);
        entry.put("params", params == null ? Map.of() : params);
        return entry;
    }
}
