package ar.ive.spec.errors;

/**
 * The catalog View `ErrorMessageView`, as data: the shape of that part of an
 * error body. Generated from the platform catalog.
 */
public record ErrorMessage(
    String code,
    String message
) {}
