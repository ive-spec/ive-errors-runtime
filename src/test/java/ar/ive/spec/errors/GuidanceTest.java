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
        assertEquals(33, Guidances.CATALOG.size());
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

    private static final Guidance INVALID = new Guidance("422_UnprocessableContent", 422, "loadRejected", "The catalog is not valid", "Fix it.");
    private static final Map<String, List<FieldProblem>> FIELDS =
        Map.of("steps[0]", List.of(new FieldProblem("notOfThisStep", null, Map.of("step", "topology"))));

    @Test
    void a_cause_of_a_detailed_family_carries_its_field_errors() {
        GuidanceTable table = ownTable();
        table.register(INVALID);
        for (IveBusinessException e : List.of(table.errorFor("loadRejected", FIELDS), table.errorFor("loadRejected", 422, FIELDS))) {
            UnprocessableException u = assertInstanceOf(UnprocessableException.class, e);
            assertEquals("The catalog is not valid", u.getMessage());
            assertEquals("loadRejected", u.condition());
            assertEquals(List.of(new UnprocessableException.FieldError("notOfThisStep", null, Map.of("step", "topology"))),
                u.fieldErrors().get("steps[0]"));
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> body = (Map<String, List<Map<String, Object>>>) ErrorBody.of(u).get("fieldErrors");
            assertEquals("notOfThisStep", body.get("steps[0]").get(0).get("key"));
            assertEquals("notOfThisStep", body.get("steps[0]").get(0).get("message"), "a problem without a text carries its key as its message");
        }
    }

    @Test
    void field_errors_for_a_family_without_them_are_not_lost_in_silence() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ownTable().errorFor("seatTaken", FIELDS));
        assertTrue(e.getMessage().contains("seatTaken") && e.getMessage().contains("DetailedErrorView"), e.getMessage());
    }

    @Test
    void no_field_errors_is_the_plain_error() {
        assertInstanceOf(ConflictException.class, ownTable().errorFor("seatTaken", Map.of()));
    }

    // --- the body --------------------------------------------------------------

    @Test
    void with_a_cause_the_body_carries_its_key_and_expects() {
        Guidance row = catalogRowWithExpects();
        IveBusinessException e = errorOf(row, "said");
        Map<String, Object> body = ErrorBody.of(e);
        assertEquals(List.of("key", "message", "expects"), List.copyOf(body.keySet()), "ErrorView's order");
        assertEquals(row.condition(), body.get("key"));
        assertEquals("said", body.get("message"));
        assertEquals(row.expects(), body.get("expects"));
    }

    @Test
    void the_body_says_nothing_of_which_error_it_is() {
        Map<String, Object> body = ErrorBody.of(errorOf(catalogRowWithExpects(), "said"));
        assertFalse(body.containsKey("errorRef"), body.toString());
        assertFalse(body.containsKey("messages"), body.toString());
    }

    @Test
    void without_a_cause_only_the_message() {
        assertEquals(Map.of("message", "Pet does not exist"),
            ErrorBody.of(new NotFoundException("Pet does not exist")));
    }

    @Test
    void with_a_guide_of_its_own() {
        GuidanceTable table = new GuidanceTable();
        table.register(OWN);
        Map<String, Object> body = ErrorBody.of(new ConflictException("m").withCondition("seatTaken"), table);
        assertEquals("seatTaken", body.get("key"));
        assertEquals("Pick another seat.", body.get("expects"));
    }

    @Test
    void field_errors() {
        BadRequestException e = new BadRequestException(BadRequestException.REF, "bad",
            Map.of("name", List.of(new BadRequestException.FieldError("MIN", "short", Map.of("min", 8)))));
        Map<String, Object> body = ErrorBody.of(e);
        assertEquals(Map.of("name", List.of(Map.of("key", "MIN", "message", "short", "params", Map.of("min", 8)))),
            body.get("fieldErrors"));
        assertEquals(List.of("message", "fieldErrors"), List.copyOf(body.keySet()), "DetailedErrorView's order");
    }

    @Test
    void transport() {
        IveTransportException down = new IveTransportException(0, "down");
        assertEquals(502, ErrorBody.statusOf(down));
        assertEquals(Map.of("message", "down"), ErrorBody.of(down));
        assertEquals(504, ErrorBody.statusOf(new IveTransportException(504, "late")));
    }

    @Test
    void a_failure_that_is_not_a_business_error() {
        assertEquals(Map.of("message", "it broke"), ErrorBody.of(null, "it broke", null));
        assertEquals(List.of("key", "message", "expects"),
            List.copyOf(ErrorBody.of("seatTaken", "taken", "Pick another seat.").keySet()));
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
        assertEquals("seatTaken", ErrorBody.keyOf(raw));
        assertEquals("Pick another seat.", ErrorBody.expectsOf(raw));
    }

    @Test
    void without_expects_in_the_body_the_guide_answers() {
        Guidance row = catalogRowWithExpects();
        String raw = "{\"key\": \"" + row.condition() + "\", \"message\": \"x\"}";
        IveBusinessException e = IveErrorFactory.fromResponse(row.code(), null, raw);
        assertEquals(row.condition(), e.condition());
        assertNull(e.carriedExpects());
        assertEquals(row.expects(), e.expects());
    }

    @Test
    void the_error_comes_from_the_status_not_from_the_body() {
        Guidance row = catalogRowWithExpects();
        String raw = "{\"errorRef\": \"409_Conflict\", \"key\": \"" + row.condition() + "\", \"message\": \"x\"}";
        IveBusinessException e = assertInstanceOf(IveBusinessException.class, IveErrorFactory.errorFromResponse(row.code(), raw));
        assertEquals(row.code(), e.code());
        assertEquals(row.errorRef(), e.errorRef(), "the guide names the error of a known cause of that code");
    }

    @Test
    void the_rich_form_keeps_its_detail() {
        String raw = "{\"key\": \"tooShort\", \"message\": \"bad\", \"fieldErrors\": {\"name\": [{\"key\": \"MIN\", \"message\": \"short\", \"params\": {\"min\": 8}}]}}";
        BadRequestException e = assertInstanceOf(BadRequestException.class, IveErrorFactory.fromResponse(400, null, raw));
        assertEquals(BadRequestException.REF, e.errorRef());
        assertEquals("tooShort", e.condition());
        assertEquals(List.of(new BadRequestException.FieldError("MIN", "short", Map.of("min", 8))), e.fieldErrors().get("name"));
    }

    @Test
    void an_empty_body_is_the_error_of_the_status() {
        NotFoundException e = assertInstanceOf(NotFoundException.class, IveErrorFactory.fromResponse(404, null, ""));
        assertEquals("Not Found", e.getMessage());
        assertNull(e.condition());
        UnprocessableException u = assertInstanceOf(UnprocessableException.class, IveErrorFactory.errorFromResponse(422, null));
        assertEquals("Unprocessable Content", u.getMessage());
        assertTrue(u.fieldErrors().isEmpty());
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
