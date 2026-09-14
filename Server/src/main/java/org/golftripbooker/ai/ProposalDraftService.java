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
import java.util.Comparator;
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
              "required": ["courses", "lodging", "startDate", "endDate", "totalCost", "itinerary", "rationale"]
            }
            """;

    private static final String SYSTEM_PROMPT = """
            You draft golf trip proposals for a host to review. You never book anything.
            A person reads everything you produce, edits it, and decides whether to send it.

            Rules:

            1. Choose courses ONLY from the courses that appear in the past trips provided.
               Never invent a course name. Choose lodging the same way.
            2. Offer one course per round requested. Repeating a course is allowed when the
               choice is deliberate, and then say why in the rationale. Never offer more
               courses than rounds.
            3. The start date must fall on or between the earliest and latest start dates
               given. The end date must be the start date plus the number of nights.
            4. THE PRICE IS GIVEN TO YOU. A suggested total has already been calculated
               from the past trips. Use it. Do not recalculate it, do not adjust it to fit
               the client's budget, and do not derive a number of your own. If you have a
               specific reason to depart from it -- premium courses, for instance -- you may,
               but say so explicitly and stay inside the stated range.
            5. The client's budget is context, not a target and not a ceiling. After using
               the suggested price, compare it to their budget and state plainly whether it
               comes in under, over, or about level. A trip that honestly costs more than
               they hoped is useful information. A price bent to match their budget is not.
            6. The itinerary is one or two short sentences in the same shape as the past
               trips below, naming how many rounds over how many days and which course
               plays last. It is written for the client.
            7. The rationale is written for the host, not the client. Two or three
               sentences. Name the past trip the courses came from, and say how the price
               compares to the budget.

            Two things that are easy to get wrong:

            - Cost per player does NOT rise with party size. Eight players do not each pay
              more than four players would. The suggested total already accounts for the
              headcount.
            - The closest comparable is the one with the same number of rounds and nights,
              not the cheapest one and not the most recent one. It is labelled for you.

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
        PriceGuide price = PriceGuide.from(comparables, request);
        PastTrip closest = price.closestMatch();
        StringBuilder prompt = new StringBuilder();

        prompt.append("THE REQUEST\n")
                .append("Destination: ").append(request.getDestination().getName()).append("\n")
                .append("Players: ").append(request.getPlayerCount()).append("\n")
                .append("Rounds requested: ").append(request.getRoundsRequested()).append("\n")
                .append("Nights: ").append(request.getNights()).append("\n")
                .append("Travel window: ").append(request.getEarliestStart())
                .append(" to ").append(request.getLatestStart()).append("\n")
                .append("Budget per player: $").append(request.getBudgetPerPlayer())
                .append("  (context only -- see rule 5)\n");

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            prompt.append("Client's notes: ").append(request.getNotes()).append("\n");
        }

        prompt.append("\nTHE PRICE, ALREADY CALCULATED\n")
                .append("Use this total: $").append(price.suggestedTotal()).append("\n")
                .append("That is $").append(price.suggestedPerPlayer()).append(" per player")
                .append(" for ").append(request.getPlayerCount()).append(" players.\n")
                .append("Method: the median cost per player per round across the ")
                .append(price.sampleSize())
                .append(" confirmed trips below, times the ")
                .append(request.getRoundsRequested()).append(" rounds requested.\n")
                .append("Defensible range for this many rounds: $")
                .append(price.lowPerPlayer()).append(" to $")
                .append(price.highPerPlayer()).append(" per player.\n");

        prompt.append("\nPAST TRIPS AT THIS DESTINATION\n")
                .append("These are the only courses you may choose from.\n")
                .append("Ordered by how closely each matches this request's rounds and nights.\n\n");

        /*
         * Sorted so the best comparable is first and labelled. Left unsorted, the model
         * picked whichever trip suited the number it had already decided on.
         */
        List<PastTrip> ranked = comparables.stream()
                .sorted(Comparator.comparingInt(t -> PriceGuide.shapeDistance(t, request)))
                .toList();

        for (PastTrip trip : ranked) {
            prompt.append("- ").append(trip.startDate()).append(": ")
                    .append(trip.playerCount()).append(" players, ")
                    .append(trip.nights()).append(" nights, ")
                    .append(trip.roundsRequested()).append(" rounds. ")
                    .append("$").append(trip.totalCost())
                    .append(" total, $").append(trip.costPerPlayer()).append(" per player.");
            if (trip == closest) {
                prompt.append("   <-- CLOSEST MATCH to this request");
            }
            prompt.append("\n  Courses: ").append(trip.courses().replace("\n", ", ")).append("\n");
            if (trip.lodging() != null && !trip.lodging().isBlank()) {
                prompt.append("  Lodging: ").append(trip.lodging()).append("\n");
            }
            if (trip.itinerary() != null && !trip.itinerary().isBlank()) {
                prompt.append("  Itinerary: ").append(trip.itinerary()).append("\n");
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
