package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;
import ar.ive.spec.core.IveTransportException;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Reconstrucción de FAMILY_BY_CODE (mencionado en un comentario de
 * ValidationException/BadRequestException, de una sesión anterior no
 * recuperada) - NO es el archivo original, es un diseño nuevo sobre
 * el mismo nombre de concepto.
 *
 * Map<Integer, BiFunction> en vez de Map<Integer, Class> - las 16
 * excepciones no comparten un único constructor: 400/422 (forma
 * rica, DetailedErrorView) llevan messages/fieldErrors además de
 * errorRef/message; las otras 14 (forma simple, StandardErrorView)
 * solo llevan errorRef/message. create() cubre el camino simple para
 * las 16 - para reportar fieldErrors/messages reales en 400/422, se
 * sigue pudiendo instanciar BadRequestException/UnprocessableException
 * directo, sin pasar por esta fábrica.
 */
public final class IveErrorFactory {

    private static final Map<Integer, BiFunction<String, String, IveBusinessException>> FAMILY_BY_CODE = Map.ofEntries(
        Map.entry(400, (ref, msg) -> new BadRequestException(ref, msg, List.of(), Map.of())),
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
        Map.entry(422, (ref, msg) -> new UnprocessableException(ref, msg, List.of(), Map.of())),
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
     * `message`. Without a ref, the family's canonical one.
     */
    private static final Map<Integer, BiFunction<String, String, IveBusinessException>> FROM_BODY_BY_CODE = Map.ofEntries(
        Map.entry(400, (ref, raw) -> BadRequestException.fromBody(ref != null ? ref : BadRequestException.REF, raw)),
        Map.entry(401, (ref, raw) -> new UnauthorizedException(ref != null ? ref : UnauthorizedException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(403, (ref, raw) -> new ForbiddenException(ref != null ? ref : ForbiddenException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(404, (ref, raw) -> new NotFoundException(ref != null ? ref : NotFoundException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(405, (ref, raw) -> new MethodNotAllowedException(ref != null ? ref : MethodNotAllowedException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(406, (ref, raw) -> new NotAcceptableException(ref != null ? ref : NotAcceptableException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(408, (ref, raw) -> new RequestTimeoutException(ref != null ? ref : RequestTimeoutException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(409, (ref, raw) -> new ConflictException(ref != null ? ref : ConflictException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(410, (ref, raw) -> new GoneException(ref != null ? ref : GoneException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(412, (ref, raw) -> new PreconditionFailedException(ref != null ? ref : PreconditionFailedException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(415, (ref, raw) -> new UnsupportedMediaTypeException(ref != null ? ref : UnsupportedMediaTypeException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(422, (ref, raw) -> UnprocessableException.fromBody(ref != null ? ref : UnprocessableException.REF, raw)),
        Map.entry(429, (ref, raw) -> new TooManyRequestsException(ref != null ? ref : TooManyRequestsException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(451, (ref, raw) -> new UnavailableForLegalReasonsException(ref != null ? ref : UnavailableForLegalReasonsException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(500, (ref, raw) -> new InternalServerErrorException(ref != null ? ref : InternalServerErrorException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(502, (ref, raw) -> new BadGatewayException(ref != null ? ref : BadGatewayException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(503, (ref, raw) -> new ServiceUnavailableException(ref != null ? ref : ServiceUnavailableException.REF, DetailedErrorBody.messageOf(raw))),
        Map.entry(504, (ref, raw) -> new GatewayTimeoutException(ref != null ? ref : GatewayTimeoutException.REF, DetailedErrorBody.messageOf(raw)))
    );

    private IveErrorFactory() { }

    public static IveBusinessException create(int code, String errorRef, String message) {
        var factory = FAMILY_BY_CODE.get(code);
        if (factory == null) {
            throw new IllegalArgumentException("Código HTTP sin excepción registrada: " + code);
        }
        return factory.apply(errorRef, message);
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
     * the cause expects, both read from the body ({@code messages[0].code}
     * and {@code expects}): whoever catches it decides without parsing the
     * body again. The {@code expects} of the body wins (the other side
     * declared it); without one, {@link IveBusinessException#expects()}
     * falls back to the guide.
     *
     * <p>Picks the rich form by itself when the family declares it (400 and
     * 422), so a generated client does not need to know which ones they
     * are.</p>
     *
     * @param errorRef the ref the caller expects; null takes the family's canonical one
     * @throws IllegalArgumentException when the code has no family
     */
    public static IveBusinessException fromResponse(int code, String errorRef, String rawBody) {
        var factory = FROM_BODY_BY_CODE.get(code);
        if (factory == null) {
            throw new IllegalArgumentException("Código HTTP sin excepción registrada: " + code);
        }
        return factory.apply(errorRef, rawBody)
            .withCondition(DetailedErrorBody.conditionOf(rawBody))
            .withExpects(DetailedErrorBody.expectsOf(rawBody));
    }

    /**
     * The exception for any error response, its {@code errorRef} from the
     * body. A code the catalog does not declare is NOT turned into a
     * business error: it comes out as {@link IveTransportException} --
     * something answered on the other side and the contract does not
     * describe it.
     *
     * @return an {@link IveBusinessException} or an {@link IveTransportException}
     */
    public static Exception errorFromResponse(int status, String rawBody) {
        if (!FROM_BODY_BY_CODE.containsKey(status)) {
            return new IveTransportException(status, "Response " + status + " that the contract does not declare");
        }
        return fromResponse(status, DetailedErrorBody.errorRefOf(rawBody), rawBody);
    }
}
