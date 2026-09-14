package org.golftripbooker.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.golftripbooker.data.PastTrip;
import org.golftripbooker.data.PastTripRepository;
import org.golftripbooker.data.TripRequestRepository;
import org.golftripbooker.domain.BookingService;
import org.golftripbooker.domain.Result;
import org.golftripbooker.domain.ResultType;
import org.golftripbooker.models.Booking;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Drafts a proposal for a host to edit. Reads only.
 *
 * This service cannot book anything, and that is structural rather than a promise: it
 * holds no write path. The draft it returns goes to a screen, a human changes what they
 * want, and what they send travels through BookingService.propose exactly as a proposal
 * typed from scratch always has. The transaction, the atomic PENDING claim and the
 * validation rules are all untouched by this feature existing.
 */
@Service
public class ProposalDraftService {

    /** Enough trips to show a price range, few enough to keep the prompt cheap. */
    private static final int COMPARABLE_LIMIT = 8;

    /** One retry, with the validation errors handed back. Beyond that, give up. */
    private static final int MAX_ATTEMPTS = 2;

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "courses":   { "type": "array", "items": { "type": "string" } },
                "lodging":   { "type": "string" },
                "startDate": { "type": "string", "description": "yyyy-MM-dd" },
                "endDate":   { "type": "string", "description": "yyyy-MM-dd" },
                "totalCost": { "type": "number" },
                "itinerary": { "type": "string" },
                "rationale": { "type": "string" }
              },
              "required": ["courses", "startDate", "endDate", "totalCost", "rationale"]
            }
            """;

    private static final String SYSTEM_PROMPT = """
            You draft golf trip proposals for a host to review. You never book anything.
            A person reads everything you produce, edits it, and decides whether to send it.

            Rules:

            1. Choose courses ONLY from the list of courses that appear in the past trips
               provided. Never invent a course name. If the list is short, reuse from it.
            2. List at least one course and no more than the number of rounds requested.
            3. The start date must fall on or between the earliest and latest start dates
               given. The end date must be the start date plus the number of nights.
            4. Price from the past trips, scaled to this party size, this many rounds and
               this many nights. The client's budget is guidance, not a ceiling. If a
               realistic trip costs more than they hoped, price it honestly and say so in
               the rationale. Do not invent a cheaper trip to fit the number.
            5. The rationale is written for the host, not the client. Two or three
               sentences. Say which past trips you priced from and why these courses.

            You are a starting point, not an authority. Where the evidence is thin, say so.
            """;

    private final TripRequestRepository requestRepository;
    private final PastTripRepository pastTripRepository;
    private final BookingService bookingService;
    private final LlmClient llm;

    public ProposalDraftService(TripRequestRepository requestRepository,
                                PastTripRepository pastTripRepository,
                                BookingService bookingService,
                                LlmClient llm) {
        this.requestRepository = requestRepository;
        this.pastTripRepository = pastTripRepository;
        this.bookingService = bookingService;
        this.llm = llm;
    }

    public Result<DraftedProposal> draftFor(int requestId, User actingHost) {
        Result<DraftedProposal> result = new Result<>();

        if (actingHost == null || actingHost.getRole() != Role.HOST) {
            result.addErrorMessage("Only a host can draft a proposal.", ResultType.FORBIDDEN);
            return result;
        }

        if (!llm.isConfigured()) {
            /*
             * Not an error worth alarming anyone about. The host fills the form in by hand,
             * which is what they did before this feature existed and what they will do any
             * time it is unavailable.
             */
            result.addErrorMessage("Drafting is not configured on this server.",
                    ResultType.INVALID);
            return result;
        }

        TripRequest request = requestRepository.findById(requestId);
        if (request == null) {
            result.addErrorMessage("Request %s was not found.", ResultType.NOT_FOUND, requestId);
            return result;
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            result.addErrorMessage("Request %s is already %s.", ResultType.INVALID,
                    requestId, request.getStatus().name().toLowerCase());
            return result;
        }

        /*
         * No seeded destination means no history, and no history means the only place
         * course names could come from is the model's imagination. Refusing is the whole
         * design: this feature assembles trips out of evidence, and with no evidence it
         * declines rather than inventing a plausible golf course that does not exist.
         */
        if (request.isUnseededPlace() || request.getDestination() == null) {
            result.addErrorMessage(
                    "No past trips to draw on for a place we have not booked before.",
                    ResultType.INVALID);
            return result;
        }

        List<PastTrip> comparables = pastTripRepository.findByDestination(
                request.getDestination().getDestinationId(), COMPARABLE_LIMIT);

        if (comparables.isEmpty()) {
            result.addErrorMessage("No past trips at %s to draw on yet.", ResultType.INVALID,
                    request.getDestination().getName());
            return result;
        }

        return generate(request, comparables);
    }

    // ---------- the loop ----------

    private Result<DraftedProposal> generate(TripRequest request, List<PastTrip> comparables) {
        Result<DraftedProposal> result = new Result<>();
        String correction = "";

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DraftedProposal drafted;
            try {
                JsonNode answer = llm.completeAsJson(
                        SYSTEM_PROMPT, buildUserPrompt(request, comparables) + correction, SCHEMA);
                drafted = parse(answer);
            } catch (LlmUnavailableException ex) {
                result.addErrorMessage("Could not draft a proposal right now. %s",
                        ResultType.INVALID, ex.getMessage());
                return result;
            } catch (IllegalArgumentException | DateTimeParseException ex) {
                correction = "\n\nYour previous answer could not be read: " + ex.getMessage()
                        + " Answer again, matching the schema exactly.";
                continue;
            }

            /*
             * The same rules a host's own typing has to clear. A draft that fails them is
             * never shown, because a suggestion the server would reject on submit is worse
             * than no suggestion at all -- it looks authoritative and wastes the host's time.
             */
            Booking asBooking = drafted.toBooking(request.getRequestId());
            Result<Booking> validation = bookingService.validateProposal(asBooking, request);

            if (validation.isSuccess()) {
                result.setPayload(drafted);
                return result;
            }

            correction = "\n\nYour previous answer broke these rules: "
                    + String.join(" ", validation.getErrorMessages())
                    + " Answer again, fixing them.";
        }

        result.addErrorMessage(
                "Could not draft a usable proposal after %s attempts. Fill the form in by hand.",
                ResultType.INVALID, MAX_ATTEMPTS);
        return result;
    }

    // ---------- prompt ----------

    private String buildUserPrompt(TripRequest request, List<PastTrip> comparables) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("THE REQUEST\n")
                .append("Destination: ").append(request.getDestination().getName()).append("\n")
                .append("Players: ").append(request.getPlayerCount()).append("\n")
                .append("Rounds requested: ").append(request.getRoundsRequested()).append("\n")
                .append("Nights: ").append(request.getNights()).append("\n")
                .append("Budget per player: $").append(request.getBudgetPerPlayer()).append("\n")
                .append("Travel window: ").append(request.getEarliestStart())
                .append(" to ").append(request.getLatestStart()).append("\n");

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            prompt.append("Client's notes: ").append(request.getNotes()).append("\n");
        }

        prompt.append("\nPAST TRIPS AT THIS DESTINATION\n")
                .append("These are the only courses you may choose from.\n\n");

        for (PastTrip trip : comparables) {
            prompt.append("- ").append(trip.startDate()).append(": ")
                    .append(trip.playerCount()).append(" players, ")
                    .append(trip.nights()).append(" nights, ")
                    .append(trip.roundsRequested()).append(" rounds. ")
                    .append("$").append(trip.totalCost())
                    .append(" total, $").append(trip.costPerPlayer()).append(" per player.\n")
                    .append("  Courses: ").append(trip.courses().replace("\n", ", ")).append("\n");
            if (trip.lodging() != null && !trip.lodging().isBlank()) {
                prompt.append("  Lodging: ").append(trip.lodging()).append("\n");
            }
        }

        prompt.append("\nDraft a proposal for the request above.");
        return prompt.toString();
    }

    // ---------- parsing ----------

    private DraftedProposal parse(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isObject()) {
            throw new IllegalArgumentException("the answer was not a JSON object.");
        }

        List<String> courses = new ArrayList<>();
        for (JsonNode course : node.path("courses")) {
            String name = course.asText("").trim();
            if (!name.isEmpty()) {
                courses.add(name);
            }
        }

        LocalDate startDate = LocalDate.parse(requireText(node, "startDate"));
        LocalDate endDate = LocalDate.parse(requireText(node, "endDate"));

        JsonNode cost = node.path("totalCost");
        if (!cost.isNumber()) {
            throw new IllegalArgumentException("totalCost was not a number.");
        }

        return new DraftedProposal(
                courses,
                text(node, "lodging"),
                startDate,
                endDate,
                cost.decimalValue().setScale(2, RoundingMode.HALF_UP),
                text(node, "itinerary"),
                text(node, "rationale"));
    }

    private static String requireText(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " was missing.");
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }
}
