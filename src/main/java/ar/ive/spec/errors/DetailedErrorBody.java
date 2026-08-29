package ar.ive.spec.errors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LEER UN CUERPO `DetailedErrorView` Y SACARLE LO QUE LA EXCEPCION LLEVA.
 *
 * Lo usan las dos familias que el catalogo declara con esa View --400 y
 * 422, y solo esas dos-- desde su `fromBody`. Vive aparte y no adentro
 * de una de ellas porque es exactamente el mismo parseo: una copia en
 * cada clase se divergiria el dia que la View cambie.
 *
 * QUIEN LO LLAMA: el cliente de SDK generado. Recibe el cuerpo crudo de
 * una respuesta de error y necesita la excepcion tipada con su detalle
 * por campo — sin esto, todo lo que el servidor dijo sobre QUE campo
 * estaba mal termina como un String suelto en `getMessage()`.
 *
 * NO FALLA NUNCA. Un cuerpo que no es JSON, o que no tiene la forma
 * esperada, no puede convertir un error del servidor en un error de
 * parseo del cliente: lo que no se entiende queda como `message` y las
 * dos colecciones salen vacias. La excepcion que se estaba construyendo
 * siempre se construye.
 *
 * La forma es la de `DetailedErrorView` en
 * `ive-catalog/platform-errors-and-views.yaml`, no una inventada acá:
 * `messages[] {code, message, severity}` y
 * `fieldErrors{<campo>: [{code, message, params}]}`.
 */
final class DetailedErrorBody {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DetailedErrorBody() { }

    static String messageOf(String rawBody) {
        JsonNode root = parse(rawBody);
        if (root == null) return rawBody;
        JsonNode message = root.get("message");
        return message != null && message.isTextual() ? message.asText() : rawBody;
    }

    static List<BadRequestException.Message> messagesOf(String rawBody) {
        List<BadRequestException.Message> result = new ArrayList<>();
        JsonNode root = parse(rawBody);
        if (root == null) return List.copyOf(result);
        JsonNode messages = root.get("messages");
        if (messages == null || !messages.isArray()) return List.copyOf(result);
        for (JsonNode entry : messages) {
            result.add(new BadRequestException.Message(
                text(entry, "code"),
                text(entry, "message"),
                // El catalogo le da default ERROR; un cuerpo que no lo
                // trae no es un cuerpo sin severidad.
                entry.hasNonNull("severity") ? entry.get("severity").asText() : "ERROR"
            ));
        }
        return List.copyOf(result);
    }

    static Map<String, List<BadRequestException.FieldError>> fieldErrorsOf(String rawBody) {
        Map<String, List<BadRequestException.FieldError>> result = new LinkedHashMap<>();
        JsonNode root = parse(rawBody);
        if (root == null) return Map.copyOf(result);
        JsonNode fieldErrors = root.get("fieldErrors");
        if (fieldErrors == null || !fieldErrors.isObject()) return Map.copyOf(result);

        fieldErrors.fields().forEachRemaining(field -> {
            if (!field.getValue().isArray()) return;
            List<BadRequestException.FieldError> errors = new ArrayList<>();
            for (JsonNode entry : field.getValue()) {
                errors.add(new BadRequestException.FieldError(
                    text(entry, "code"),
                    text(entry, "message"),
                    paramsOf(entry.get("params"))
                ));
            }
            result.put(field.getKey(), List.copyOf(errors));
        });
        return Map.copyOf(result);
    }

    private static Map<String, Object> paramsOf(JsonNode params) {
        if (params == null || !params.isObject()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        params.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            // `params` es libre por schema (`additionalProperties: true`),
            // asi que se conserva el tipo JSON en vez de aplastar todo a
            // String: `{min: 8}` tiene que llegar como numero.
            if (value.isNumber()) result.put(entry.getKey(), value.numberValue());
            else if (value.isBoolean()) result.put(entry.getKey(), value.booleanValue());
            else if (value.isNull()) result.put(entry.getKey(), null);
            else result.put(entry.getKey(), value.asText());
        });
        return Map.copyOf(result);
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static JsonNode parse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return null;
        try {
            JsonNode root = MAPPER.readTree(rawBody);
            return root != null && root.isObject() ? root : null;
        } catch (Exception e) {
            return null;
        }
    }
}
