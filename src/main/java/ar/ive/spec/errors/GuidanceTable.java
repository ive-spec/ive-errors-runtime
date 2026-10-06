package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A guide of causes: the BASE rows (the catalog's, see {@link Guidances})
 * plus the REGISTERED ones (a project's own).
 *
 * <p>A registered row wins over a base row with the same error and cause,
 * and is looked at first when searching by cause alone: it is the most
 * specific declaration.</p>
 *
 * <p>Registration normally happens once, when the generated code of a
 * project is loaded; the methods are synchronized so a registration racing
 * a lookup is still safe.</p>
 */
public final class GuidanceTable implements Iterable<Guidance> {

    private final Map<String, Guidance> base = new LinkedHashMap<>();
    private final Map<String, Guidance> own = new LinkedHashMap<>();

    /** An empty guide. */
    public GuidanceTable() {
    }

    /** A guide whose base rows are {@code rows}. */
    public GuidanceTable(Collection<Guidance> rows) {
        for (Guidance row : rows) {
            base.put(key(row.errorRef(), row.condition()), row);
        }
    }

    /** Adds causes (a project's own). Same error and cause: the last wins. */
    public synchronized void register(Guidance... rows) {
        for (Guidance row : rows) {
            own.put(key(row.errorRef(), row.condition()), row);
        }
    }

    /**
     * The guide of one cause, or {@code null} when it is not declared: an
     * invented action would be followed.
     */
    public synchronized Guidance lookup(String errorRef, String condition) {
        String key = key(errorRef, condition);
        Guidance row = own.get(key);
        return row != null ? row : base.get(key);
    }

    /**
     * The first cause with that key (of that code, when {@code code} is not
     * null). Two errors may declare a cause with the same key; then the
     * caller has to say the code too.
     */
    public synchronized Guidance byCondition(String condition, Integer code) {
        for (Map<String, Guidance> table : List.of(own, base)) {
            for (Guidance row : table.values()) {
                if (row.condition().equals(condition) && (code == null || code.equals(row.code()))) {
                    return row;
                }
            }
        }
        return null;
    }

    /** {@link #byCondition(String, Integer)} with any code. */
    public Guidance byCondition(String condition) {
        return byCondition(condition, null);
    }

    /**
     * What a cause expects, from this guide: by error and cause first, then
     * by cause (and code) alone. {@code null} when no cause is given, when
     * it is not declared, or when its {@code expects} is empty.
     */
    public Guidance guidanceOf(String errorRef, String condition, Integer code) {
        if (condition == null || condition.isBlank()) return null;
        Guidance row = errorRef == null ? null : lookup(errorRef, condition);
        return row != null ? row : byCondition(condition, code);
    }

    /** What the cause of {@code error} expects, from this guide (see {@link #guidanceOf}). */
    public String expectsOf(IveBusinessException error) {
        String ref = error.errorRef();
        Guidance row = guidanceOf(ref, error.condition(), IveErrorFactory.codeOf(ref));
        return row == null || row.expects() == null || row.expects().isBlank() ? null : row.expects();
    }

    /** Every cause key known, sorted. */
    public synchronized SortedSet<String> conditions() {
        SortedSet<String> keys = new TreeSet<>();
        for (Guidance row : this) keys.add(row.condition());
        return keys;
    }

    /**
     * The error of a DECLARED cause, with its code, message and cause, ready
     * to be thrown.
     *
     * <p>A cause nobody declared cannot be raised, and no 500 is invented to
     * cover it: it is a programming error -- a misspelt key, or a cause
     * removed from the spec -- and it shows as such
     * ({@link IllegalArgumentException}), with the list of the ones that
     * exist.</p>
     */
    public IveBusinessException errorFor(String condition, Integer code) {
        Guidance row = byCondition(condition, code);
        if (row == null) {
            SortedSet<String> known = conditions();
            throw new IllegalArgumentException("The cause '" + condition + "' is not declared. The declared ones are: "
                + (known.isEmpty() ? "(none)" : String.join(", ", known)) + ".");
        }
        int status = row.code() != null ? row.code() : 500;
        return IveErrorFactory.create(status, row.errorRef(), row.message()).withCondition(row.condition());
    }

    /** {@link #errorFor(String, Integer)} with any code. */
    public IveBusinessException errorFor(String condition) {
        return errorFor(condition, (Integer) null);
    }

    /**
     * The error of a declared cause WITH ITS FIELD ERRORS: a 422 that says
     * the input is not valid has to say WHICH field and why.
     *
     * <p>Only a family with {@code DetailedErrorView} (400 and 422) carries
     * them. Field errors for any other cause would be dropped without a
     * trace, so that is a programming error too
     * ({@link IllegalArgumentException}). Without field errors it is
     * {@link #errorFor(String, Integer)}.</p>
     */
    public IveBusinessException errorFor(String condition, Integer code, Map<String, List<FieldProblem>> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) return errorFor(condition, code);
        Guidance row = byCondition(condition, code);
        if (row == null) return errorFor(condition, code);   // the "not declared" error, with the list
        int status = row.code() != null ? row.code() : 500;
        if (!IveErrorFactory.carriesFieldErrors(status)) {
            throw new IllegalArgumentException("The cause '" + condition + "' is a " + row.errorRef()
                + ", whose family carries no per-field detail (the catalog does not declare it with DetailedErrorView):"
                + " the detail would be lost.");
        }
        return IveErrorFactory.create(status, row.errorRef(), row.message(), fieldErrors).withCondition(row.condition());
    }

    /** {@link #errorFor(String, Integer, Map)} with any code. */
    public IveBusinessException errorFor(String condition, Map<String, List<FieldProblem>> fieldErrors) {
        return errorFor(condition, null, fieldErrors);
    }

    /** Whether the cause is declared for that error. */
    public boolean contains(String errorRef, String condition) {
        return lookup(errorRef, condition) != null;
    }

    /** How many rows: the registered ones plus the base rows they do not replace. */
    public int size() {
        return rows().size();
    }

    /** The registered rows first, then the base rows they do not replace. */
    @Override
    public Iterator<Guidance> iterator() {
        return rows().iterator();
    }

    private synchronized List<Guidance> rows() {
        List<Guidance> result = new ArrayList<>(own.values());
        for (Map.Entry<String, Guidance> entry : base.entrySet()) {
            if (!own.containsKey(entry.getKey())) result.add(entry.getValue());
        }
        return List.copyOf(result);
    }

    private static String key(String errorRef, String condition) {
        return errorRef + "|" + condition;
    }
}
