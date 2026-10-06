package ar.ive.spec.errors;

/**
 * The catalog View `ErrorView`, as data: the shape of that part of an
 * error body. Generated from the platform catalog.
 */
public record ErrorView(
    /** The key of the cause (`resourceNotFound`). */
    String key,
    /** The message of the cause. */
    String message,
    /** What whoever receives the error has to do. */
    String expects
) {}
