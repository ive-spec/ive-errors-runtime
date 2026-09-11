package ar.ive.spec.security;

/**
 * La raíz de las dos señales del verificador. Existe para poder atajarlas
 * juntas.
 *
 * <p>NO SON ERRORES DEL CATÁLOGO —ésos son los 16 de
 * {@code ar.ive.spec.errors}— sino el contrato entre el código generado y
 * el despliegue. El backend las traduce a un 401 con la causa que
 * corresponda.</p>
 */
public abstract class CredentialException extends Exception {

    private static final long serialVersionUID = 1L;

    protected CredentialException(String message) {
        super(message);
    }

    protected CredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}
