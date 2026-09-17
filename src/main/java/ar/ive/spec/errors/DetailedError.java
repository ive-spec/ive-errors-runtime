package ar.ive.spec.errors;

import java.util.List;
import java.util.Map;

/**
 * The catalog View `DetailedErrorView`, as data: the shape of that part of an
 * error body. Generated from the platform catalog.
 */
public record DetailedError(
    /** Errores que no corresponden a un campo en particular. */
    List<ErrorMessage> messages,
    /** Cada campo con la lista de sus errores. */
    Map<String, List<ErrorMessage>> fieldErrors
) {}
