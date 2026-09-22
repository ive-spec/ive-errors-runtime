package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

import java.util.List;

/**
 * THE GUIDE OF THE CATALOG'S CAUSES, and the guide of this process.
 *
 * <p>The catalog's causes come with the library ({@link #CATALOG},
 * generated from the catalog's {@code errors[*].conditions}). A project
 * registers on top ONLY its own causes -- the ones its spec declares and
 * the catalog does not, or a catalog cause it rewords -- with
 * {@link #register}. Its generated code does that when it is loaded.</p>
 *
 * <p>{@link #GUIDANCE} is what {@link IveBusinessException#expects()},
 * {@link ErrorBody} and {@link #errorFor} read.</p>
 */
public final class Guidances {

    /** The causes of the catalog, one per failure error and cause. */
    public static final List<Guidance> CATALOG = List.of(
        new Guidance("400_BadRequest", 400, "traceContextMissing", "The request carries no trace identifier", "Send the trace you are already inside of. If there is none, the caller is the start of the chain and this operation is not: it declares `traceability` on its input, so the value has to come from whoever calls."),
        new Guidance("401_Unauthorized", 401, "credentialExpired", "The authentication credential has expired", "Renew the credential and retry the same message."),
        new Guidance("401_Unauthorized", 401, "credentialInvalid", "The authentication credential is not valid", "Authenticate from scratch. The credential is not recoverable, so renewing it leads nowhere."),
        new Guidance("401_Unauthorized", 401, "credentialMissing", "The request carries no authentication credential", "Authenticate and retry the same message."),
        new Guidance("403_Forbidden", 403, "contractedQuotaExhausted", "The contracted quota has been used up", "Extend what is contracted, which is a matter outside this interaction. This quota renews by agreement rather than by time."),
        new Guidance("403_Forbidden", 403, "permissionMissing", "The caller is authenticated but lacks permission for this operation", "Obtain the permission, which is a matter outside this interaction. The same message will get the same answer until then."),
        new Guidance("403_Forbidden", 403, "resourceNotPermitted", "The caller's permission does not reach this particular resource", "Another resource of the same kind may be permitted. Obtaining access to this one is a matter outside this interaction."),
        new Guidance("404_NotFound", 404, "pathNotFound", "The requested path does not correspond to any operation", "Check the call against the contract: this is an integration mistake and the same message will keep failing."),
        new Guidance("404_NotFound", 404, "resourceNotFound", "The requested resource does not exist", "Use another identifier. This one may become valid later, so a retry makes sense only once something changed."),
        new Guidance("405_MethodNotAllowed", 405, "methodNotSupported", "The method used is not supported for this resource", "Check the call against the contract: this is an integration mistake and the same message will keep failing."),
        new Guidance("406_NotAcceptable", 406, "noAcceptableRepresentation", "The service cannot produce a response matching the content types the caller accepts", "Accept one of the content types the contract declares."),
        new Guidance("408_RequestTimeout", 408, "requestNotCompleted", "The request did not complete within the time the service waits", "Send it again. It may not have been processed: with an idempotency key, sending it again is safe."),
        new Guidance("409_Conflict", 409, "idempotencyKeyExpired", "The idempotency key has expired", "Retry with a new idempotency key."),
        new Guidance("409_Conflict", 409, "idempotencyKeyReused", "The idempotency key was already used with a different payload", "Correct the call: either the payload changed while the key stayed the same, or the key belongs to another request."),
        new Guidance("409_Conflict", 409, "requestStillInProgress", "The original request is still being processed", "Retry the same message after the indicated time."),
        new Guidance("409_Conflict", 409, "resourceAlreadyExists", "A resource with the same natural key already exists", "Work with the existing resource, or send a different natural key."),
        new Guidance("409_Conflict", 409, "resourceImmutable", "The resource is final and can no longer be changed or removed", "Do not retry: compensate it with a reversing operation instead (for example, a reversing entry for a posted accounting entry)."),
        new Guidance("409_Conflict", 409, "resourceInUse", "The resource is in use and cannot be changed or removed right now", "Retry later with growing waits, once whatever holds it (a dependent resource, a running process) releases it."),
        new Guidance("409_Conflict", 409, "versionConflict", "The resource changed since it was read", "Read the resource again, resolve the difference and retry. The same message would overwrite someone else's change."),
        new Guidance("410_Gone", 410, "resourceGone", "The resource existed and is no longer available", "Do not retry: it will not come back. Remove whatever still refers to it."),
        new Guidance("412_PreconditionFailed", 412, "resourceAlreadyPresent", "The caller required the resource to be absent, and it already exists", "Decide against the existing resource: retrying with the same precondition leads to the same answer."),
        new Guidance("412_PreconditionFailed", 412, "resourceStateChanged", "The resource no longer matches the state the caller stated as a precondition", "Read the resource again, resolve the difference against its current state and retry with the precondition rebuilt from it."),
        new Guidance("415_UnsupportedMediaType", 415, "unsupportedContentType", "The content type of the request body is not supported by this operation", "Send the body in one of the content types the contract declares."),
        new Guidance("429_TooManyRequests", 429, "periodQuotaExhausted", "The caller has used up the quota for the current period", "Wait for the period to renew. The indicated time is when the quota comes back."),
        new Guidance("429_TooManyRequests", 429, "rateLimitExceeded", "The caller has exceeded the allowed rate of requests", "Slow down and retry after the indicated time."),
        new Guidance("451_UnavailableForLegalReasons", 451, "legallyRestricted", "The resource is unavailable due to a legal restriction", "Lifting the restriction is a matter outside this interaction. Retrying leads to the same answer."),
        new Guidance("500_InternalServerError", 500, "unexpectedServerError", "An unexpected error occurred while processing the request", "Report the trace identifier: resolving it happens on the service side. A retry may work if the cause was transient."),
        new Guidance("502_BadGateway", 502, "invalidUpstreamResponse", "An upstream service returned an invalid response", "Retry with growing waits. Resolving it happens between this service and its upstream."),
        new Guidance("503_ServiceUnavailable", 503, "dependencyUnavailable", "A dependency of this service is unavailable", "Retry with growing waits. The time depends on a third party, so this service can only estimate it loosely."),
        new Guidance("503_ServiceUnavailable", 503, "serviceOverloaded", "The service is temporarily unable to handle the request", "Retry after the indicated time."),
        new Guidance("503_ServiceUnavailable", 503, "underMaintenance", "The service is under maintenance", "Retry after the indicated time."),
        new Guidance("504_GatewayTimeout", 504, "upstreamTimeout", "An upstream service did not respond in time", "Retry with growing waits. The original request may have gone through, so use an idempotency key where the operation supports one.")
    );

    /** The guide of this process: the catalog's causes plus the registered ones. */
    public static final GuidanceTable GUIDANCE = new GuidanceTable(CATALOG);

    private Guidances() { }

    /** {@code GUIDANCE.lookup}: the guide of one cause, or {@code null}. */
    public static Guidance of(String errorRef, String condition) {
        return GUIDANCE.lookup(errorRef, condition);
    }

    /** Registers a project's own causes on {@link #GUIDANCE}. */
    public static void register(Guidance... rows) {
        GUIDANCE.register(rows);
    }

    /** The error of a declared cause of {@link #GUIDANCE} (see {@link GuidanceTable#errorFor}). */
    public static IveBusinessException errorFor(String condition) {
        return GUIDANCE.errorFor(condition);
    }

    /** The error of a declared cause of {@link #GUIDANCE}, of that code. */
    public static IveBusinessException errorFor(String condition, Integer code) {
        return GUIDANCE.errorFor(condition, code);
    }
}
