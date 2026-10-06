package ar.ive.spec.errors;

/**
 * What is declared about ONE cause of ONE error: the text the error is
 * raised with ({@code message}) and what whoever receives it has to do
 * ({@code expects}).
 *
 * <p>The error carries only its cause ({@code condition}); the texts live
 * in a {@link GuidanceTable}. Two causes of the same code expect different
 * things -- a 401 for an expired credential is solved by renewing it, one
 * for an invalid credential is not -- so the guide is per cause, not per
 * code.</p>
 *
 * @param errorRef  the catalog error the cause belongs to (e.g. {@code 401_Unauthorized})
 * @param code      the HTTP code it answers with; {@code null} when nobody declared it
 * @param condition the cause key, the stable identifier that travels as the body's {@code key}
 * @param message   the text the error is raised with
 * @param expects   what whoever receives it has to do; part of the contract
 */
public record Guidance(String errorRef, Integer code, String condition, String message, String expects) {
}
