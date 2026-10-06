package ar.ive.spec.errors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * READING A RAW ERROR BODY and taking out of it what the exception carries.
 *
 * <p>The shape is the catalog's error Views ({@code ErrorView},
 * {@code DetailedErrorView}), not one invented here:
 * {@code {key, message, expects, fieldErrors: {<field>: [{key, message, params}]}}}.
 * The per-field detail is read by the two families the catalog declares
 * with {@code DetailedErrorView} (400 and 422) from their {@code fromBody};
 * it lives here and not in one of them because it is exactly the same
 * parsing, and a copy in each class would drift the day the View changes.</p>
 *
 * <p>WHO CALLS IT: the generated SDK client, through
 * {@link IveErrorFactory#fromResponse}. It receives the raw body of an
 * error response and needs the typed exception with its cause and its
 * per-field detail.</p>
 *
 * <p>IT NEVER FAILS. A body that is not JSON, or not of the expected shape,
 * cannot turn an error of the server into a parse error of the client:
 * what is not understood stays as the {@code message}, the cause is absent
 * and the collections come out empty. The exception being built is always
 * built.</p>
 */
final class DetailedErrorBody {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DetailedErrorBody() { }

    /** The body's {@code message}; the raw text when the body is not one. */
    static String messageOf(String rawBody) {
        JsonNode root = parse(rawBody);
        if (root == null) return rawBody;
        JsonNode message = root.get("message");
        return message != null && message.isTextual() ? message.asText() : rawBody;
    }

    /**
     * The message of an error response of the family {@code familyRef}: the
     * body's, or -- an empty body -- the status text, taken from the
     * family's catalog key ({@code 404_NotFound} -> {@code Not Found}).
     */
    static String messageOf(String rawBody, String familyRef) {
        if (rawBody == null || rawBody.isBlank()) return statusTextOf(familyRef);
        return messageOf(rawBody);
    }

    /** The cause: the body's {@code key}. Null when the body has none -- it is not guessed. */
    static String keyOf(String rawBody) {
        String key = text(parse(rawBody), "key");
        return key == null || key.isBlank() ? null : key;
    }

    /** What the cause expects, as the other side wrote it; null when absent or blank. */
    static String expectsOf(String rawBody) {
        String expects = text(parse(rawBody), "expects");
        return expects == null || expects.isBlank() ? null : expects;
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
                    text(entry, "key"),
                    text(entry, "message"),
                    paramsOf(entry.get("params"))
                ));
            }
            result.put(field.getKey(), List.copyOf(errors));
        });
        return Map.copyOf(result);
    }

    /** {@code 422_UnprocessableContent} -> {@code Unprocessable Content}; the ref itself otherwise. */
    static String statusTextOf(String familyRef) {
        if (familyRef == null) return null;
        String[] parts = familyRef.split("_", 2);
        if (parts.length < 2 || parts[1].isEmpty()) return familyRef;
        return parts[1].replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }

    private static Map<String, Object> paramsOf(JsonNode params) {
        if (params == null || !params.isObject()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        params.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            // `params` is free by schema (`additionalProperties: true`), so
            // the JSON type is kept instead of flattening everything to a
            // String: `{min: 8}` has to arrive as a number.
            if (value.isNumber()) result.put(entry.getKey(), value.numberValue());
            else if (value.isBoolean()) result.put(entry.getKey(), value.booleanValue());
            else if (value.isNull()) result.put(entry.getKey(), null);
            else result.put(entry.getKey(), value.asText());
        });
        return result.containsValue(null) ? java.util.Collections.unmodifiableMap(result) : Map.copyOf(result);
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
