package ar.ive.spec.errors;

import ar.ive.spec.core.IveBusinessException;
import ar.ive.spec.core.IveTransportException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The cause of an error, its guide, and the body that carries both.
 *
 * <p>What was decided: the error carries only its cause
 * ({@code condition}); the guide of the catalog's causes comes with the
 * library and a project adds only its own; the body always carries
 * {@code expects} when the cause has one, and a client reads it back from
 * there. The same cases as the Python library's test_guidance.py.</p>
 */
class GuidanceTest {

    /** A catalog row whose cause expects something. */
    private static Guidance catalogRowWithExpects() {
        return Guidances.CATALOG.stream()
            .filter(row -> row.expects() != null && !row.expects().isBlank() && row.code() != null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("The catalog declares no cause with `expects`."));
    }

    private static IveBusinessException errorOf(Guidance row, String message) {
        return IveErrorFactory.create(row.code(), row.errorRef(), message).withCondition(row.condition());
    }

    // --- the cause ------------------------------------------------------------

    @Test
    void no_cause_unless_someone_says() {
        NotFoundException e = new NotFoundException("missing");
        assertNull(e.condition());
        assertNull(e.expects());
    }

    @Test
    void the_cause_is_set_fluently() {
        ConflictException e = new ConflictException("x");
        assertSame(e, e.withCondition("versionConflict"));
        assertEquals("versionConflict", e.condition());
    }

    @Test
    void expects_comes_from_the_catalog_guide() {
        Guidance row = catalogRowWithExpects();
        assertEquals(row.expects(), errorOf(row, "x").expects());
    }

    @Test
    void an_explicit_expects_wins() {
        IveBusinessException e = errorOf(catalogRowWithExpects(), "x").withExpects("what the other side said");
        assertEquals("what the other side said", e.expects());
    }

    @Test
    void an_undeclared_cause_has_no_invented_action() {
        assertNull(new NotFoundException("x").withCondition("nobodySaidThis").expects());
    }

    // --- the catalog's guide -------------------------------------------------

    @Test
    void every_cause_of_the_catalog() {
        assertEquals(29, Guidances.CATALOG.size());
    }

    @Test
    void every_row_belongs_to_a_family() {
        for (Guidance row : Guidances.CATALOG) {
            assertNotNull(row.code(), row.toString());
            assertTrue(row.errorRef().startsWith(row.code() + "_"), row.toString());
            assertFalse(row.message() == null || row.message().isBlank(), row.toString());
            assertEquals(row.errorRef(), IveErrorFactory.create(row.code(), row.errorRef(), row.message()).errorRef());
        }
    }

    @Test
    void every_condition_constant_is_a_catalog_cause() throws IllegalAccessException {
        int constants = 0;
        for (Field field : Conditions.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            constants++;
            String key = (String) field.get(null);
            assertTrue(Guidances.GUIDANCE.conditions().contains(key), key);
        }
        assertEquals(Guidances.GUIDANCE.conditions().size(), constants);
    }

    // --- a project's own causes ------------------------------------------------

    private static final Guidance OWN = new Guidance("409_Conflict", 409, "seatTaken", "The seat is taken", "Pick another seat.");

    private static GuidanceTable ownTable() {
        // A table of its own: the process-wide GUIDANCE is not touched.
        GuidanceTable table = new GuidanceTable(Guidances.CATALOG);
        table.register(OWN);
        return table;
    }

    @Test
    void registered_on_top_of_the_catalog() {
        GuidanceTable table = ownTable();
        assertSame(OWN, table.lookup("409_Conflict", "seatTaken"));
        Guidance catalog = catalogRowWithExpects();
        assertEquals(catalog, table.lookup(catalog.errorRef(), catalog.condition()));
        assertEquals(Guidances.CATALOG.size() + 1, table.size());
        assertNull(Guidances.GUIDANCE.lookup("409_Conflict", "seatTaken"));
    }

    @Test
    void a_rewording_wins_over_the_catalog() {
        GuidanceTable table = ownTable();
        Guidance catalog = catalogRowWithExpects();
        Guidance reworded = new Guidance(catalog.errorRef(), catalog.code(), catalog.condition(), "Reworded", "Do this instead.");
        table.register(reworded);
        assertSame(reworded, table.lookup(catalog.errorRef(), catalog.condition()));
        assertSame(reworded, table.byCondition(catalog.condition()));
        assertEquals(Guidances.CATALOG.size() + 1, table.size());
    }

    @Test
    void error_for_a_declared_cause() {
        IveBusinessException e = ownTable().errorFor("seatTaken");
        assertInstanceOf(ConflictException.class, e);
        assertEquals("The seat is taken", e.getMessage());
        assertEquals("seatTaken", e.condition());
    }

    @Test
    void an_undeclared_cause_cannot_be_raised() {
        assertThrows(IllegalArgumentException.class, () -> ownTable().errorFor("nobodySaidThis"));
    }

    // --- the body --------------------------------------------------------------

    @Test
    void with_a_cause_the_body_carries_messages_and_expects() {
        Guidance row = catalogRowWithExpects();
        IveBusinessException e = errorOf(row, "said");
        Map<String, Object> body = ErrorBody.of(e);
        assertEquals(row.errorRef(), body.get("errorRef"));
        assertEquals("said", body.get("message"));
        assertEquals(List.of(Map.of("code", row.condition(), "message", "said", "severity", "ERROR")), body.get("messages"));
        assertEquals(row.expects(), body.get("expects"));
    }

    @Test
    void without_a_cause_neither() {
        assertEquals(Map.of("message", "Pet does not exist", "errorRef", "404_NotFound"),
            ErrorBody.of(new NotFoundException("Pet does not exist")));
    }

    @Test
    void with_a_guide_of_its_own() {
        GuidanceTable table = new GuidanceTable();
        table.register(OWN);
        Map<String, Object> body = ErrorBody.of(new ConflictException("m").withCondition("seatTaken"), table);
        assertEquals("Pick another seat.", body.get("expects"));
    }

    @Test
    void field_errors() {
        BadRequestException e = new BadRequestException(BadRequestException.REF, "bad", List.of(),
            Map.of("key", List.of(new BadRequestException.FieldError("MIN", "short", Map.of("min", 8)))));
        assertEquals(Map.of("key", List.of(Map.of("code", "MIN", "message", "short", "params", Map.of("min", 8)))),
            ErrorBody.of(e).get("fieldErrors"));
    }

    @Test
    void transport() {
        IveTransportException down = new IveTransportException(0, "down");
        assertEquals(502, ErrorBody.statusOf(down));
        assertEquals(Map.of("message", "down", "errorRef", "502_Upstream"), ErrorBody.of(down));
        assertEquals(504, ErrorBody.statusOf(new IveTransportException(504, "late")));
    }

    // --- reading it back ---------------------------------------------------------

    @Test
    void the_client_reads_the_cause_and_its_expects_from_the_body() throws Exception {
        IveBusinessException sent = new ConflictException("taken").withCondition("seatTaken").withExpects("Pick another seat.");
        String raw = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(ErrorBody.of(sent));
        Exception e = IveErrorFactory.errorFromResponse(409, raw);
        ConflictException received = assertInstanceOf(ConflictException.class, e);
        assertEquals("taken", received.getMessage());
        assertEquals("seatTaken", received.condition());
        assertEquals("Pick another seat.", received.expects());
        assertEquals("Pick another seat.", ErrorBody.expectsOf(raw));
    }

    @Test
    void without_expects_in_the_body_the_guide_answers() {
        Guidance row = catalogRowWithExpects();
        String raw = "{\"message\": \"x\", \"errorRef\": \"" + row.errorRef() + "\", \"messages\": [{\"code\": \"" + row.condition() + "\"}]}";
        IveBusinessException e = IveErrorFactory.fromResponse(row.code(), null, raw);
        assertEquals(row.condition(), e.condition());
        assertNull(e.carriedExpects());
        assertEquals(row.expects(), e.expects());
    }

    @Test
    void the_rich_form_keeps_its_detail() {
        String raw = "{\"message\": \"bad\", \"messages\": [{\"code\": \"tooShort\"}], \"fieldErrors\": {\"key\": [{\"code\": \"MIN\", \"message\": \"short\"}]}}";
        BadRequestException e = assertInstanceOf(BadRequestException.class, IveErrorFactory.fromResponse(400, null, raw));
        assertEquals(BadRequestException.REF, e.errorRef());
        assertEquals("tooShort", e.condition());
        assertEquals(1, e.fieldErrors().get("key").size());
    }

    @Test
    void a_code_the_catalog_does_not_declare_is_transport() {
        IveTransportException e = assertInstanceOf(IveTransportException.class, IveErrorFactory.errorFromResponse(418, "{}"));
        assertEquals(418, e.code());
    }

    @Test
    void a_body_that_is_not_json_is_the_message() {
        NotFoundException e = assertInstanceOf(NotFoundException.class, IveErrorFactory.fromResponse(404, null, "plain text"));
        assertEquals("plain text", e.getMessage());
        assertNull(e.condition());
    }

    @Test
    void the_code_of_a_ref() {
        assertEquals(404, IveErrorFactory.codeOf("404_NotFound"));
        assertNull(IveErrorFactory.codeOf("escalated"));
        assertNull(IveErrorFactory.codeOf(null));
        assertInstanceOf(NotFoundException.class, IveErrorFactory.create("404_NotFound", "x"));
    }
}
