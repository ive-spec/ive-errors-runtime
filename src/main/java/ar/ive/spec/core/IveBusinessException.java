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

    /**
     * LA CAUSA, cuando quien lanza la sabe.
     *
     * <p>Es la clave de la `condition` declarada en la especificación
     * —`credentialExpired`, `stockInsuficiente`— y es lo que viaja en
     * `messages[0].code` del cuerpo, que es de donde el SDK generado la
     * lee para decidir sin parsear prosa.</p>
     *
     * <p>NO ES OBLIGATORIA, y por eso es un campo y no un parámetro del
     * constructor: hay errores que se lanzan sin saber por cuál de las
     * causas declaradas fue. Un error sin causa sale sin `messages`, que
     * es lo correcto — <b>adivinarla por el código elegiría una de varias
     * que esperan cosas distintas</b>.</p>
     */
    private String condition;

    public String condition() {
        return condition;
    }

    /** Devuelve `this` para poder escribir `throw new X(msg).withCondition(...)`. */
    @SuppressWarnings("unchecked")
    public <T extends IveBusinessException> T withCondition(String condition) {
        this.condition = condition;
        return (T) this;
    }

    /**
     * WHAT THE CAUSE EXPECTS, when this error carries it explicitly: the
     * client reads it from the body it came in (the other side declared
     * it, and that wins). Otherwise {@link #expects()} answers from the
     * guide.
     */
    private String expects;

    /**
     * What the cause of this error expects from whoever receives it: the one
     * it carries (read from a response body) or, without one, the guide's
     * ({@link ar.ive.spec.errors.Guidances#GUIDANCE}). {@code null} when the
     * error has no cause or its cause is not declared: an invented action
     * would be followed.
     */
    public String expects() {
        return expects != null ? expects : ar.ive.spec.errors.Guidances.GUIDANCE.expectsOf(this);
    }

    /** Only the {@code expects} this error carries explicitly, or {@code null}. */
    public String carriedExpects() {
        return expects;
    }

    /** Returns {@code this}, like {@link #withCondition}. Null or blank clears it. */
    @SuppressWarnings("unchecked")
    public <T extends IveBusinessException> T withExpects(String expects) {
        this.expects = expects == null || expects.isBlank() ? null : expects;
        return (T) this;
    }

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
