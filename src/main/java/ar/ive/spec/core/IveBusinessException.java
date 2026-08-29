package ar.ive.spec.core;

/**
 * La base de las 16 familias de error, una por clave del catalogo de
 * plataforma.
 *
 * SON DIECISEIS Y NO MAS. Un error no se inventa por caso de dominio:
 * "asiento no disponible" y "vuelo cerrado" no son dos errores nuevos,
 * son el MISMO 404 con dos `conditions` distintas. La condicion es lo
 * que dice por que se lanzo — y es, a la vez, el mensaje con el que se
 * lanza. Por eso la clase es de familia y `REF` alcanza: cubre todo lo
 * que el modelo permite declarar.
 *
 * DOS FORMAS DE CONSTRUIR:
 *
 *  · `(message)` — usa `REF`, la clave canonica de la familia. Es la
 *    forma normal, y la que el codigo generado necesita: un
 *    `case NotFoundException.REF ->` exige una CONSTANTE DE COMPILACION,
 *    y una constante estatica solo puede tener un valor. Con las claves
 *    catalogadas eso no es una restriccion, es exactamente el modelo.
 *  · `(errorRef, message)` — el ref explicito. La usan `IveErrorFactory`
 *    y los clientes de SDK, que reciben el ref del otro lado y no lo
 *    deducen.
 *
 * Los valores de `REF` salen de `ive-catalog/platform-errors-and-views.yaml`
 * y de ningun otro lado: son la MISMA clave que la especificacion usa en
 * sus `$ref`, y es lo que hace que `errorRef()` y el catalogo hablen del
 * mismo error.
 */
public abstract class IveBusinessException extends Exception {
    private static final long serialVersionUID = 3897490023122755116L;
	public abstract String errorRef();

    // Código HTTP, derivado del propio errorRef (siempre empieza con
    // el código, ej. "404_NotFound" -> 404) - agregado para que quien
    // maneje la excepción (ej. un @ControllerAdvice) no tenga que
    // reparsear errorRef() a mano, mismo truco que ya usa Service
    // internamente para construir la excepción en primer lugar.
    public int code() {
        return Integer.parseInt(errorRef().split("_")[0]);
    }

    protected IveBusinessException() {
        super();
    }

    protected IveBusinessException(String message) {
        super(message);
    }
}
