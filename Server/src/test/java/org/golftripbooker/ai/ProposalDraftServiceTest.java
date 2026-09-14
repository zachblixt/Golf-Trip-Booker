package org.golftripbooker.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.golftripbooker.data.BookingRepository;
import org.golftripbooker.data.DestinationRepository;
import org.golftripbooker.data.PastTrip;
import org.golftripbooker.data.PastTripRepository;
import org.golftripbooker.data.TripRequestRepository;
import org.golftripbooker.domain.BookingService;
import org.golftripbooker.domain.Result;
import org.golftripbooker.domain.ResultType;
import org.golftripbooker.models.Destination;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * No Spring, no database, no network, no API key, no cost. The whole point of the
 * LlmClient seam is that these run on every push like any other test.
 *
 * BookingService is real rather than mocked, built over mock repositories. Its validation
 * touches no repository, so it costs nothing to use the genuine rules here -- and mocking
 * validateProposal would mean asserting against a stub instead of the thing that will
 * actually judge the draft.
 */
class ProposalDraftServiceTest {

    private static final int REQUEST_ID = 42;
    private static final int DESTINATION_ID = 7;

    private static final String GOOD_ANSWER = """
            {
              "courses": ["Grayhawk", "TPC Scottsdale"],
              "lodging": "The Phoenician",
              "startDate": "2027-03-04",
              "endDate": "2027-03-07",
              "totalCost": 3200.00,
              "itinerary": "Arrive Thursday, play Friday and Saturday.",
              "rationale": "Priced from the two March trips below, scaled to four players."
            }
            """;

    private QueuedLlmClient llm;
    private TripRequestRepository requestRepository;
    private PastTripRepository pastTripRepository;
    private ProposalDraftService service;

    private final User host = new User(3, "h@h.com", "tourpro", null, Role.HOST);
    private final User client = new User(1, "a@a.com", "zach", null, Role.CLIENT);

    @BeforeEach
    void setup() {
        llm = new QueuedLlmClient();
        requestRepository = Mockito.mock(TripRequestRepository.class);
        pastTripRepository = Mockito.mock(PastTripRepository.class);

        BookingService bookingService = new BookingService(
                Mockito.mock(BookingRepository.class),
                Mockito.mock(TripRequestRepository.class),
                Mockito.mock(DestinationRepository.class));

        service = new ProposalDraftService(
                requestRepository, pastTripRepository, bookingService, llm);

        when(requestRepository.findById(REQUEST_ID)).thenReturn(pendingRequest());
        when(pastTripRepository.findByDestination(anyInt(), anyInt())).thenReturn(pastTrips());
    }

    @Test
    void draftsFromThePastTripsAtThatDestination() {
        llm.queue(GOOD_ANSWER);

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertTrue(result.isSuccess(), String.valueOf(result.getErrorMessages()));
        DraftedProposal drafted = result.getPayload();
        assertEquals(List.of("Grayhawk", "TPC Scottsdale"), drafted.courses());
        assertEquals(LocalDate.of(2027, 3, 4), drafted.startDate());
        assertEquals(0, new BigDecimal("3200.00").compareTo(drafted.totalCost()));
        assertFalse(drafted.rationale().isBlank());
    }

    /**
     * The past trips are the menu. If the prompt did not say so the model would invent
     * course names, so this asserts the courses actually reach the prompt.
     */
    @Test
    void thePromptCarriesTheCoursesTheModelMayChooseFrom() {
        llm.queue(GOOD_ANSWER);

        service.draftFor(REQUEST_ID, host);

        assertTrue(llm.lastUserPrompt.contains("Grayhawk"));
        assertTrue(llm.lastUserPrompt.contains("TPC Scottsdale"));
        assertTrue(llm.lastUserPrompt.contains("only courses you may choose from"));
    }

    /**
     * Four rounds were requested but the request below asks for two, so a five-course
     * answer breaks the same rule a host's own typing would. The drafter feeds the error
     * back and takes the second answer.
     */
    /**
     * The price reaches the model as a finished number rather than a thing to work out,
     * and the best comparable is labelled so it does not have to pick one.
     */
    @Test
    void thePromptCarriesAPriceAlreadyCalculatedAndNamesTheClosestTrip() {
        llm.queue(GOOD_ANSWER);

        service.draftFor(REQUEST_ID, host);

        assertTrue(llm.lastUserPrompt.contains("THE PRICE, ALREADY CALCULATED"));
        assertTrue(llm.lastUserPrompt.contains("Use this total: $"));
        assertTrue(llm.lastUserPrompt.contains("CLOSEST MATCH to this request"));
        assertTrue(llm.lastUserPrompt.contains("context only"),
                "the budget must be framed as context, not as a target");
    }

    @Test
    void retriesOnceWhenTheFirstAnswerBreaksARule() {
        llm.queue(GOOD_ANSWER.replace(
                "[\"Grayhawk\", \"TPC Scottsdale\"]",
                "[\"Grayhawk\", \"TPC Scottsdale\", \"We-Ko-Pa\", \"Troon North\"]"));
        llm.queue(GOOD_ANSWER);

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertTrue(result.isSuccess(), String.valueOf(result.getErrorMessages()));
        assertEquals(2, llm.calls);
        assertTrue(llm.lastUserPrompt.contains("broke these rules"));
    }

    @Test
    void givesUpAfterTwoUnusableAnswers() {
        String tooManyCourses = GOOD_ANSWER.replace(
                "[\"Grayhawk\", \"TPC Scottsdale\"]",
                "[\"Grayhawk\", \"TPC Scottsdale\", \"We-Ko-Pa\", \"Troon North\"]");
        llm.queue(tooManyCourses);
        llm.queue(tooManyCourses);

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertFalse(result.isSuccess());
        assertEquals(2, llm.calls);
        assertTrue(String.join(" ", result.getErrorMessages()).contains("by hand"));
    }

    /**
     * A start date outside the window the group gave. Same rule, different clause, and
     * worth its own test because dates are where a model most often drifts.
     */
    @Test
    void rejectsAStartDateOutsideTheClientsWindow() {
        String wrongDates = GOOD_ANSWER
                .replace("\"2027-03-04\"", "\"2027-06-01\"")
                .replace("\"2027-03-07\"", "\"2027-06-04\"");
        llm.queue(wrongDates);
        llm.queue(wrongDates);

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertFalse(result.isSuccess());
    }

    @Test
    void declinesWhenThereIsNoHistoryAtThatDestination() {
        when(pastTripRepository.findByDestination(anyInt(), anyInt())).thenReturn(List.of());

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertFalse(result.isSuccess());
        assertEquals(0, llm.calls, "the model must not be called with nothing to ground it");
    }

    @Test
    void declinesForAPlaceNobodyHasBookedBefore() {
        TripRequest freeText = pendingRequest();
        freeText.setDestination(null);
        freeText.setRequestedPlace("Bandon Dunes");
        when(requestRepository.findById(REQUEST_ID)).thenReturn(freeText);

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertFalse(result.isSuccess());
        assertEquals(0, llm.calls);
    }

    @Test
    void declinesQuietlyWhenNoApiKeyIsConfigured() {
        llm.configured = false;

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertFalse(result.isSuccess());
        assertEquals(ResultType.INVALID, result.getResultType());
        assertEquals(0, llm.calls);
    }

    @Test
    void isForbiddenForAClient() {
        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, client);

        assertEquals(ResultType.FORBIDDEN, result.getResultType());
        assertEquals(0, llm.calls);
    }

    @Test
    void refusesARequestThatIsNoLongerPending() {
        TripRequest alreadyProposed = pendingRequest();
        alreadyProposed.setStatus(RequestStatus.PROPOSED);
        when(requestRepository.findById(REQUEST_ID)).thenReturn(alreadyProposed);

        Result<DraftedProposal> result = service.draftFor(REQUEST_ID, host);

        assertFalse(result.isSuccess());
        assertEquals(0, llm.calls);
    }

    // ---------- fixtures ----------

    private TripRequest pendingRequest() {
        TripRequest request = new TripRequest();
        request.setRequestId(REQUEST_ID);
        request.setClient(client);
        request.setDestination(new Destination(DESTINATION_ID, "Scottsdale", "Arizona",
                null, null, null));
        request.setBudgetPerPlayer(new BigDecimal("900.00"));
        request.setPlayerCount(4);
        request.setRoundsRequested(2);
        request.setNights(3);
        request.setEarliestStart(LocalDate.of(2027, 3, 1));
        request.setLatestStart(LocalDate.of(2027, 3, 10));
        request.setStatus(RequestStatus.PENDING);
        return request;
    }

    private List<PastTrip> pastTrips() {
        return List.of(
                new PastTrip("Grayhawk\nTPC Scottsdale", "The Phoenician",
                        4, 3, 2, new BigDecimal("3100.00"), LocalDate.of(2026, 3, 5),
                        "2 rounds over 3 days. TPC Scottsdale last."),
                new PastTrip("TPC Scottsdale\nWe-Ko-Pa", "Hotel Valley Ho",
                        6, 3, 2, new BigDecimal("4800.00"), LocalDate.of(2026, 3, 18),
                        "2 rounds over 3 days. We-Ko-Pa last."));
    }

    /**
     * Hands back queued answers in order and remembers what it was asked. Everything the
     * real client does over HTTP, without the HTTP.
     */
    private static class QueuedLlmClient implements LlmClient {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final Deque<String> answers = new ArrayDeque<>();
        boolean configured = true;
        int calls = 0;
        String lastUserPrompt = "";

        void queue(String json) {
            answers.add(json);
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public JsonNode completeAsJson(String systemPrompt, String userPrompt, String schemaJson)
                throws LlmUnavailableException {
            calls++;
            lastUserPrompt = userPrompt;
            if (answers.isEmpty()) {
                throw new LlmUnavailableException("No answer queued for call " + calls + ".");
            }
            try {
                return MAPPER.readTree(answers.poll());
            } catch (Exception ex) {
                throw new LlmUnavailableException("Bad fixture JSON.", ex);
            }
        }
    }
}
