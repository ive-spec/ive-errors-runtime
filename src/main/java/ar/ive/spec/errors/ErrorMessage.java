package ar.ive.spec.errors;

import java.util.Map;

/**
 * The catalog View `ErrorMessageView`, as data: the shape of that part of an
 * error body. Generated from the platform catalog.
 */
public record ErrorMessage(
    String key,
    String message,
    /** The values a field problem was checked against (a limit, a pattern). */
    Map<String, Object> params
) {}
