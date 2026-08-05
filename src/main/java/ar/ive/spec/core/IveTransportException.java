package ar.ive.spec.core;

/**
 * Condición técnica de transporte (`{$error.code}` en la spec), NO declarada
 * en el `errors:` de ningún Intent particular. Es unchecked porque cualquier
 * Intent puede sufrirla (timeouts, 503, conexión rechazada, etc.),
 * independientemente de lo que haya catalogado en su contrato de negocio.
 *
 * El adapter (HTTP/gRPC/AsyncAPI) es quien la lanza al traducir el fallo de
 * transporte real; el Intent nunca la declara en su `throws`.
 */
public class IveTransportException extends RuntimeException {
    private static final long serialVersionUID = -625699363483399911L;
	private final int code;

    public IveTransportException(int code, String message) {
        super(message);
        this.code = code;
    }

    public IveTransportException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
