package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

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
        Map.entry(409, ConflictException::new),
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

    private IveErrorFactory() { }

    public static IveBusinessException create(int code, String errorRef, String message) {
        var factory = FAMILY_BY_CODE.get(code);
        if (factory == null) {
            throw new IllegalArgumentException("Código HTTP sin excepción registrada: " + code);
        }
        return factory.apply(errorRef, message);
    }
}
