package ar.ive.spec.core;

public abstract class IveBusinessException extends Exception {
    private static final long serialVersionUID = 3897490023122755116L;
	public abstract String errorRef();

    protected IveBusinessException() {
        super();
    }

    protected IveBusinessException(String message) {
        super(message);
    }
}