package ar.ive.spec.errors;

/**
 * The cause keys the catalog declares, as constants.
 *
 * <p>A cause key is what travels in {@code messages[0].code} and what
 * {@link ar.ive.spec.core.IveBusinessException#condition()} holds. Writing
 * the string by hand in an {@code if} fails silently when the catalog
 * renames it; a constant does not compile. Among them are the platform
 * causes of the 401/403 guards (credential missing, expired or invalid;
 * permission missing).</p>
 */
public final class Conditions {

    public static final String CONTRACTED_QUOTA_EXHAUSTED = "contractedQuotaExhausted";
    public static final String CREDENTIAL_EXPIRED = "credentialExpired";
    public static final String CREDENTIAL_INVALID = "credentialInvalid";
    public static final String CREDENTIAL_MISSING = "credentialMissing";
    public static final String DEPENDENCY_UNAVAILABLE = "dependencyUnavailable";
    public static final String IDEMPOTENCY_KEY_EXPIRED = "idempotencyKeyExpired";
    public static final String IDEMPOTENCY_KEY_REUSED = "idempotencyKeyReused";
    public static final String INVALID_UPSTREAM_RESPONSE = "invalidUpstreamResponse";
    public static final String LEGALLY_RESTRICTED = "legallyRestricted";
    public static final String METHOD_NOT_SUPPORTED = "methodNotSupported";
    public static final String NO_ACCEPTABLE_REPRESENTATION = "noAcceptableRepresentation";
    public static final String PATH_NOT_FOUND = "pathNotFound";
    public static final String PERIOD_QUOTA_EXHAUSTED = "periodQuotaExhausted";
    public static final String PERMISSION_MISSING = "permissionMissing";
    public static final String RATE_LIMIT_EXCEEDED = "rateLimitExceeded";
    public static final String REQUEST_NOT_COMPLETED = "requestNotCompleted";
    public static final String REQUEST_STILL_IN_PROGRESS = "requestStillInProgress";
    public static final String RESOURCE_ALREADY_EXISTS = "resourceAlreadyExists";
    public static final String RESOURCE_ALREADY_PRESENT = "resourceAlreadyPresent";
    public static final String RESOURCE_GONE = "resourceGone";
    public static final String RESOURCE_IMMUTABLE = "resourceImmutable";
    public static final String RESOURCE_IN_USE = "resourceInUse";
    public static final String RESOURCE_NOT_FOUND = "resourceNotFound";
    public static final String RESOURCE_NOT_PERMITTED = "resourceNotPermitted";
    public static final String RESOURCE_STATE_CHANGED = "resourceStateChanged";
    public static final String SERVICE_OVERLOADED = "serviceOverloaded";
    public static final String TRACE_CONTEXT_MISSING = "traceContextMissing";
    public static final String UNDER_MAINTENANCE = "underMaintenance";
    public static final String UNEXPECTED_SERVER_ERROR = "unexpectedServerError";
    public static final String UNSUPPORTED_CONTENT_TYPE = "unsupportedContentType";
    public static final String UPSTREAM_TIMEOUT = "upstreamTimeout";
    public static final String VERSION_CONFLICT = "versionConflict";

    private Conditions() { }
}
