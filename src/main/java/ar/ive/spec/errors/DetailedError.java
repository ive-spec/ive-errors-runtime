package ar.ive.spec.errors;

import java.util.List;
import java.util.Map;

/**
 * The catalog View `DetailedErrorView`, as data: the shape of that part of an
 * error body. Generated from the platform catalog.
 */
public record DetailedError(
    /** The key of the cause. */
    String key,
    /** The message of the cause. */
    String message,
    /** What whoever receives the error has to do. */
    String expects,
    /** Each field with the list of its problems. */
    Map<String, List<ErrorMessage>> fieldErrors
) {}
