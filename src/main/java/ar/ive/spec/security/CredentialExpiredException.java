package ar.ive.spec.security;

/**
 * La credencial ERA válida y venció.
 *
 * <p>SE SEPARA DE LA OTRA A PROPÓSITO, y no es una distinción cosmética: es
 * la única de las tres causas del 401 cuyo `expects` implica un ciclo del
 * lado de quien llama —<b>renovar y reintentar el mismo mensaje</b>—.
 * Colapsarla con `credentialInvalid` le dice que se autentique de cero a
 * alguien a quien le alcanzaba con renovar.</p>
 */
public class CredentialExpiredException extends CredentialException {

    private static final long serialVersionUID = 1L;

    public CredentialExpiredException(String message) {
        super(message);
    }

    public CredentialExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
