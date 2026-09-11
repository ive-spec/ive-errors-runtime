package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;
import ar.ive.spec.core.IveTransportException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * THE BODY OF AN ERROR RESPONSE: writing it, and reading back what the
 * exceptions carry.
 *
 * <p>The shape is the catalog's error View -- {@code message},
 * {@code errorRef}, {@code messages[]{code, message, severity}},
 * {@code fieldErrors} -- plus {@code expects}, what the cause of the error
 * expects from whoever receives it. The cause travels in
 * {@code messages[0].code}, which is exactly where the generated clients
 * read it, and {@code expects} travels ALWAYS when the cause has one: any
 * client, or an AI agent, gets it without a table of its own.</p>
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
        Map<String, Object> body = of(error.errorRef(), error.getMessage(), error.condition(), expects);
        Map<String, ?> fieldErrors = fieldErrorsOf(error);
        if (!fieldErrors.isEmpty()) {
            // Same place as in DetailedErrorView: after `messages`, before `expects`.
            Object carried = body.remove("expects");
            body.put("fieldErrors", fieldErrors);
            if (carried != null) body.put("expects", carried);
        }
        return body;
    }

    /**
     * The body for a failure that is not an {@link IveBusinessException} but
     * does carry a catalog {@code errorRef} (e.g. a flow's
     * {@code FlowFailureException}). {@code condition} and {@code expects}
     * may be null: then neither {@code messages} nor {@code expects} is
     * written -- guessing a cause by its code would pick one of several that
     * expect different things.
     */
    public static Map<String, Object> of(String errorRef, String message, String condition, String expects) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("errorRef", errorRef);
        if (condition != null && !condition.isBlank()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", condition);
            entry.put("message", message);
            entry.put("severity", "ERROR");
            body.put("messages", List.of(entry));
        }
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

    /** The body of a technical failure: {@code {message, errorRef: "<status>_Upstream"}}. */
    public static Map<String, Object> of(IveTransportException error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", error.getMessage());
        body.put("errorRef", statusOf(error) + "_Upstream");
        return body;
    }

    // --- reading -----------------------------------------------------------

    /** The {@code message} of a raw error body; the raw text when it is not one. */
    public static String messageOf(String rawBody) {
        return DetailedErrorBody.messageOf(rawBody);
    }

    /** The {@code errorRef} of a raw error body, or {@code null}. */
    public static String errorRefOf(String rawBody) {
        return DetailedErrorBody.errorRefOf(rawBody);
    }

    /** The cause of a raw error body ({@code messages[0].code}), or {@code null}. */
    public static String conditionOf(String rawBody) {
        return DetailedErrorBody.conditionOf(rawBody);
    }

    /** The {@code expects} of a raw error body, or {@code null}. */
    public static String expectsOf(String rawBody) {
        return DetailedErrorBody.expectsOf(rawBody);
    }

    private static Map<String, ?> fieldErrorsOf(IveBusinessException error) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (error instanceof BadRequestException bad) {
            bad.fieldErrors().forEach((field, errors) -> result.put(field,
                errors.stream().map(e -> fieldError(e.code(), e.message(), e.params())).toList()));
        } else if (error instanceof UnprocessableException unprocessable) {
            unprocessable.fieldErrors().forEach((field, errors) -> result.put(field,
                errors.stream().map(e -> fieldError(e.code(), e.message(), e.params())).toList()));
        }
        return result;
    }

    private static Map<String, Object> fieldError(String code, String message, Map<String, Object> params) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("code", code);
        entry.put("message", message);
        entry.put("params", params == null ? Map.of() : params);
        return entry;
    }
}
