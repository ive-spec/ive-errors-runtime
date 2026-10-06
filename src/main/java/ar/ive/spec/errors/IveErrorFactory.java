package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;
import ar.ive.spec.core.IveTransportException;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * THE FAMILY OF AN HTTP CODE, built from what is known of the error: a ref
 * and a message, or a whole error response.
 *
 * <p>{@code Map<Integer, BiFunction>} and not {@code Map<Integer, Class>}:
 * the families do not share one constructor. 400 and 422 (DetailedErrorView)
 * carry {@code fieldErrors} besides the ref and the message; the rest
 * (ErrorView) only the ref and the message. {@link #create(int, String, String)}
 * covers the simple path for all of them; the field errors of 400/422 go
 * through {@link #create(int, String, String, Map)}.</p>
 */
public final class IveErrorFactory {

    private static final Map<Integer, BiFunction<String, String, IveBusinessException>> FAMILY_BY_CODE = Map.ofEntries(
        Map.entry(400, (ref, msg) -> new BadRequestException(ref, msg, Map.of())),
        Map.entry(401, UnauthorizedException::new),
        Map.entry(403, ForbiddenException::new),
        Map.entry(404, NotFoundException::new),
        Map.entry(405, MethodNotAllowedException::new),
        Map.entry(406, NotAcceptableException::new),
        Map.entry(408, RequestTimeoutException::new),
        Map.entry(409, ConflictException::new),
        Map.entry(410, GoneException::new),
        Map.entry(412, PreconditionFailedException::new),
        Map.entry(415, UnsupportedMediaTypeException::new),
        Map.entry(422, (ref, msg) -> new UnprocessableException(ref, msg, Map.of())),
        Map.entry(429, TooManyRequestsException::new),
        Map.entry(451, UnavailableForLegalReasonsException::new),
        Map.entry(500, InternalServerErrorException::new),
        Map.entry(502, BadGatewayException::new),
        Map.entry(503, ServiceUnavailableException::new),
        Map.entry(504, GatewayTimeoutException::new)
    );

    /**
     * The same families, built from a RAW RESPONSE BODY: the detailed ones
     * (DetailedErrorView) through their `fromBody`, the rest with the body's
     * `message` (the status text when the body is empty). Without a ref, the
     * family's canonical one.
     */
    private static final Map<Integer, BiFunction<String, String, IveBusinessException>> FROM_BODY_BY_CODE = Map.ofEntries(
        Map.entry(400, (ref, raw) -> BadRequestException.fromBody(ref != null ? ref : BadRequestException.REF, raw)),
        Map.entry(401, (ref, raw) -> new UnauthorizedException(ref != null ? ref : UnauthorizedException.REF, DetailedErrorBody.messageOf(raw, UnauthorizedException.REF))),
        Map.entry(403, (ref, raw) -> new ForbiddenException(ref != null ? ref : ForbiddenException.REF, DetailedErrorBody.messageOf(raw, ForbiddenException.REF))),
        Map.entry(404, (ref, raw) -> new NotFoundException(ref != null ? ref : NotFoundException.REF, DetailedErrorBody.messageOf(raw, NotFoundException.REF))),
        Map.entry(405, (ref, raw) -> new MethodNotAllowedException(ref != null ? ref : MethodNotAllowedException.REF, DetailedErrorBody.messageOf(raw, MethodNotAllowedException.REF))),
        Map.entry(406, (ref, raw) -> new NotAcceptableException(ref != null ? ref : NotAcceptableException.REF, DetailedErrorBody.messageOf(raw, NotAcceptableException.REF))),
        Map.entry(408, (ref, raw) -> new RequestTimeoutException(ref != null ? ref : RequestTimeoutException.REF, DetailedErrorBody.messageOf(raw, RequestTimeoutException.REF))),
        Map.entry(409, (ref, raw) -> new ConflictException(ref != null ? ref : ConflictException.REF, DetailedErrorBody.messageOf(raw, ConflictException.REF))),
        Map.entry(410, (ref, raw) -> new GoneException(ref != null ? ref : GoneException.REF, DetailedErrorBody.messageOf(raw, GoneException.REF))),
        Map.entry(412, (ref, raw) -> new PreconditionFailedException(ref != null ? ref : PreconditionFailedException.REF, DetailedErrorBody.messageOf(raw, PreconditionFailedException.REF))),
        Map.entry(415, (ref, raw) -> new UnsupportedMediaTypeException(ref != null ? ref : UnsupportedMediaTypeException.REF, DetailedErrorBody.messageOf(raw, UnsupportedMediaTypeException.REF))),
        Map.entry(422, (ref, raw) -> UnprocessableException.fromBody(ref != null ? ref : UnprocessableException.REF, raw)),
        Map.entry(429, (ref, raw) -> new TooManyRequestsException(ref != null ? ref : TooManyRequestsException.REF, DetailedErrorBody.messageOf(raw, TooManyRequestsException.REF))),
        Map.entry(451, (ref, raw) -> new UnavailableForLegalReasonsException(ref != null ? ref : UnavailableForLegalReasonsException.REF, DetailedErrorBody.messageOf(raw, UnavailableForLegalReasonsException.REF))),
        Map.entry(500, (ref, raw) -> new InternalServerErrorException(ref != null ? ref : InternalServerErrorException.REF, DetailedErrorBody.messageOf(raw, InternalServerErrorException.REF))),
        Map.entry(502, (ref, raw) -> new BadGatewayException(ref != null ? ref : BadGatewayException.REF, DetailedErrorBody.messageOf(raw, BadGatewayException.REF))),
        Map.entry(503, (ref, raw) -> new ServiceUnavailableException(ref != null ? ref : ServiceUnavailableException.REF, DetailedErrorBody.messageOf(raw, ServiceUnavailableException.REF))),
        Map.entry(504, (ref, raw) -> new GatewayTimeoutException(ref != null ? ref : GatewayTimeoutException.REF, DetailedErrorBody.messageOf(raw, GatewayTimeoutException.REF)))
    );

    private IveErrorFactory() { }

    public static IveBusinessException create(int code, String errorRef, String message) {
        var factory = FAMILY_BY_CODE.get(code);
        if (factory == null) {
            throw new IllegalArgumentException("HTTP code with no exception registered: " + code);
        }
        return factory.apply(errorRef, message);
    }

    /**
     * Whether the family of that code carries per-field detail: the ones
     * the catalog declares with DetailedErrorView.
     */
    public static boolean carriesFieldErrors(int code) {
        switch (code) {
            case 400: return true;
            case 422: return true;
            default: return false;
        }
    }

    /**
     * The exception of that code WITH ITS FIELD ERRORS, each one turned
     * into the family's own {@code FieldError}. Without field errors it is
     * {@link #create(int, String, String)}.
     *
     * @throws IllegalArgumentException when the code has no family, or its
     *         family carries no per-field detail (the detail would be lost)
     */
    public static IveBusinessException create(int code, String errorRef, String message,
                                              Map<String, List<FieldProblem>> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) return create(code, errorRef, message);
        switch (code) {
            case 400: return new BadRequestException(errorRef, message,
                fieldsAs(fieldErrors, p -> new BadRequestException.FieldError(p.key(), p.message(), p.params())));
            case 422: return new UnprocessableException(errorRef, message,
                fieldsAs(fieldErrors, p -> new UnprocessableException.FieldError(p.key(), p.message(), p.params())));
            default:
                if (!FAMILY_BY_CODE.containsKey(code)) {
                    throw new IllegalArgumentException("HTTP code with no exception registered: " + code);
                }
                throw new IllegalArgumentException("The family of " + code + " carries no per-field detail"
                    + " (the catalog does not declare it with DetailedErrorView): the detail would be lost.");
        }
    }

    private static <T> Map<String, List<T>> fieldsAs(Map<String, List<FieldProblem>> fields,
                                                     java.util.function.Function<FieldProblem, T> as) {
        Map<String, List<T>> out = new java.util.LinkedHashMap<>();
        fields.forEach((field, problems) -> out.put(field, problems.stream().map(as).toList()));
        return out;
    }

    /**
     * The exception of a catalog error, its code taken from the ref itself
     * (every catalog ref starts with it: {@code 404_NotFound} -> 404).
     *
     * @throws IllegalArgumentException when the ref carries no code, or the
     *         code has no family
     */
    public static IveBusinessException create(String errorRef, String message) {
        Integer code = codeOf(errorRef);
        if (code == null) {
            throw new IllegalArgumentException("The errorRef '" + errorRef + "' does not start with an HTTP code.");
        }
        return create(code, errorRef, message);
    }

    /**
     * The HTTP code a catalog ref starts with, or {@code null} when it does
     * not start with one (a flow's conventional {@code escalated}, a ref
     * from an unknown source). Null, not a default: whoever calls decides
     * what an unknown code means for it.
     */
    public static Integer codeOf(String errorRef) {
        if (errorRef == null) return null;
        String prefix = errorRef.split("_", 2)[0];
        try {
            return Integer.valueOf(prefix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * The exception of a complete error response, WITH ITS CAUSE and what
     * the cause expects, both read from the body ({@code key} and
     * {@code expects}): whoever catches it decides without parsing the body
     * again. WHICH ERROR it is comes from the status ({@code code}), never
     * from the body. The {@code expects} of the body wins (the other side
     * declared it); without one, {@link IveBusinessException#expects()}
     * falls back to the guide.
     *
     * <p>Picks the rich form by itself when the family declares it (400 and
     * 422), so a generated client does not need to know which ones they
     * are. An empty body, or one that is not JSON, is still the error of
     * the status: its message is the raw text or the status text.</p>
     *
     * @param errorRef the ref the caller expects; null takes the family's canonical one
     * @throws IllegalArgumentException when the code has no family
     */
    public static IveBusinessException fromResponse(int code, String errorRef, String rawBody) {
        var factory = FROM_BODY_BY_CODE.get(code);
        if (factory == null) {
            throw new IllegalArgumentException("HTTP code with no exception registered: " + code);
        }
        return factory.apply(errorRef, rawBody)
            .withCondition(DetailedErrorBody.keyOf(rawBody))
            .withExpects(DetailedErrorBody.expectsOf(rawBody));
    }

    /**
     * The exception for any error response, by its STATUS. The ref is the
     * one the guide gives the body's cause ({@code key}) for that code --
     * a cause the guide knows names its error -- or else the family's
     * canonical one. A code the catalog does not declare is NOT turned into
     * a business error: it comes out as {@link IveTransportException} --
     * something answered on the other side and the contract does not
     * describe it.
     *
     * @return an {@link IveBusinessException} or an {@link IveTransportException}
     */
    public static Exception errorFromResponse(int status, String rawBody) {
        if (!FROM_BODY_BY_CODE.containsKey(status)) {
            return new IveTransportException(status, "Response " + status + " that the contract does not declare");
        }
        String key = DetailedErrorBody.keyOf(rawBody);
        Guidance row = key == null ? null : Guidances.GUIDANCE.byCondition(key, status);
        return fromResponse(status, row == null ? null : row.errorRef(), rawBody);
    }
}
