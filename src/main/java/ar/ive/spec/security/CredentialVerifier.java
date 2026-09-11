package ar.ive.spec.security;

import java.util.Map;

/**
 * COMO SE VERIFICA UNA CREDENCIAL. Lo único que el generador NO escribe.
 *
 * <p>Qué proveedor de identidad, cómo se lee un token, cuándo está vencido
 * y a qué permisos corresponde es del DESPLIEGUE. El backend generado sabe
 * QUÉ exige cada operación —lo declara el Envelope— y deja este puerto
 * para el CÓMO.</p>
 *
 * <p>Es el mismo lugar que ocupa {@code DecisionTable} en la librería de
 * protección: un puerto, no una decisión.</p>
 *
 * <p><b>Sin ninguna implementación, ninguna credencial es válida.</b> La
 * ausencia no es un permiso.</p>
 *
 * <p>Con un resource server de Spring ya montado son dos líneas: leer el
 * {@code Jwt} del {@code SecurityContext} y devolver sus claims con sus
 * scopes.</p>
 */
@FunctionalInterface
public interface CredentialVerifier {

    /**
     * Lo que se sepa de quien llama, o una de las dos negativas.
     *
     * <p>No se le exige forma al mapa: lo único que el código generado lee
     * es {@code scopes}, y sólo cuando la especificación declara permisos.
     * Todo lo demás queda disponible para quien lo necesite.</p>
     *
     * @throws CredentialExpiredException si la credencial ERA válida y
     *         venció — es la única de las tres causas cuyo `expects`
     *         implica renovar y reintentar el mismo mensaje.
     * @throws CredentialInvalidException si no vale, y renovarla no lleva
     *         a ningún lado.
     */
    Map<String, Object> verify(String credential)
            throws CredentialExpiredException, CredentialInvalidException;

    /**
     * Igual que el anterior, pero además con los encabezados de la
     * petición (en minúsculas).
     *
     * <p><b>POR QUÉ EXISTE.</b> El puerto recibía sólo la credencial, y
     * eso alcanza para todo SSO que tenga forma de token en un
     * encabezado. NO alcanza para el patrón más común de integración
     * corporativa: un proxy que autentica adelante —el gateway del propio
     * proveedor, oauth2-proxy— y le pasa al servicio la identidad ya
     * resuelta en OTRO encabezado. Con eso, SAML, Kerberos y mTLS —que no
     * tienen forma de encabezado— entran igual: el proxy los convierte en
     * algo que sí la tiene.</p>
     *
     * <p><b>ES UN MÉTODO POR OMISIÓN A PROPÓSITO.</b> Quien ya escribió un
     * verificador que sólo mira la credencial sigue andando sin tocar
     * nada, y quien necesita los encabezados sobreescribe éste. Cambiar
     * la firma del abstracto habría roto a todos para servir a algunos.</p>
     *
     * <p><b>SE PASAN LOS ENCABEZADOS Y NO LA PETICIÓN ENTERA.</b> Un
     * verificador que recibe el cuerpo o la ruta termina decidiendo la
     * identidad según QUÉ se está pidiendo, y eso es autorización
     * disfrazada de autenticación.</p>
     *
     * <p>La credencial puede venir <b>vacía</b>: con un proxy adelante no
     * hay encabezado de credencial, y eso no es un error por sí mismo.</p>
     */
    default Map<String, Object> verify(String credential, Map<String, String> headers)
            throws CredentialExpiredException, CredentialInvalidException {
        return verify(credential);
    }
}
