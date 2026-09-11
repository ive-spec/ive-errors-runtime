package ar.ive.spec.security;

/**
 * La credencial no vale, y renovarla no lleva a ningún lado.
 *
 * <p>Mal formada, firma que no verifica, emisor desconocido, revocada. Se
 * traduce a `401_Unauthorized` con la causa `credentialInvalid`, cuyo
 * `expects` es autenticarse de cero.</p>
 */
public class CredentialInvalidException extends CredentialException {

    private static final long serialVersionUID = 1L;

    public CredentialInvalidException(String message) {
        super(message);
    }

    public CredentialInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
