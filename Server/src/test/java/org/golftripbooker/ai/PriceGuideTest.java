package org.golftripbooker.ai;

import org.golftripbooker.data.PastTrip;
import org.golftripbooker.models.Destination;
import org.golftripbooker.models.TripRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The real Myrtle Beach seed data, so every expected number below can be checked by hand
 * against initial-data.sql rather than taken on trust.
 *
 * Per player, per round:
 *   req  7   4p 2n 2r  $824    $206/player   / 2 = $103.00
 *   req 14   4p 2n 3r  $1,328  $332/player   / 3 = $110.67
 *   req 26   8p 3n 4r  $4,056  $507/player   / 4 = $126.75
 *   req 38   4p 3n 3r  $3,000  $750/player   / 3 = $250.00
 *
 * Median of those four is (110.67 + 126.75) / 2 = $118.71.
 */
class PriceGuideTest {

    private static final PastTrip REQ_7 = new PastTrip(
            "Man O'War\nBlackmoor", "Sand Dunes Resort",
            4, 2, 2, new BigDecimal("824.00"), LocalDate.of(2026, 5, 12),
            "2 rounds over 3 days. Blackmoor last.");

    private static final PastTrip REQ_14 = new PastTrip(
            "Man O'War\nLitchfield CC\nBlackmoor", "Ocean Reef Resort",
            4, 2, 3, new BigDecimal("1328.00"), LocalDate.of(2026, 6, 2),
            "3 rounds over 3 days. Blackmoor last.");

    private static final PastTrip REQ_26 = new PastTrip(
            "Arrowhead\nTradition Club\nBlackmoor\nMan O'War", "Beach Cove Resort",
            8, 3, 4, new BigDecimal("4056.00"), LocalDate.of(2026, 6, 20),
            "4 rounds over 4 days. Man O'War last.");

    private static final PastTrip REQ_38 = new PastTrip(
            "Barefoot Dye\nBarefoot Love\nTPC Myrtle Beach", "Barefoot Resort Villas",
            4, 3, 3, new BigDecimal("3000.00"), LocalDate.of(2026, 7, 8),
            "3 rounds over 4 days. TPC Myrtle Beach last.");

    private static final List<PastTrip> MYRTLE_BEACH = List.of(REQ_7, REQ_14, REQ_26, REQ_38);

    /** Request 53: 8 players, 3 rounds, 2 nights, $400 per player. */
    private TripRequest request53() {
        TripRequest request = new TripRequest();
        request.setDestination(new Destination(7, "Myrtle Beach", "South Carolina", null, null, null));
        request.setPlayerCount(8);
        request.setRoundsRequested(3);
        request.setNights(2);
        request.setBudgetPerPlayer(new BigDecimal("400.00"));
        return request;
    }

    @Test
    void pricesFromTheMedianCostPerPlayerPerRound() {
        PriceGuide guide = PriceGuide.from(MYRTLE_BEACH, request53());

        // $118.71 median x 3 rounds = $356.13, rounded to the dollar.
        assertEquals(new BigDecimal("356"), guide.suggestedPerPlayer());
        assertEquals(0, new BigDecimal("2848.00").compareTo(guide.suggestedTotal()));
        assertEquals(4, guide.sampleSize());
    }

    @Test
    void theRangeSpansTheCheapestAndDearestPastTrips() {
        PriceGuide guide = PriceGuide.from(MYRTLE_BEACH, request53());

        assertEquals(new BigDecimal("309"), guide.lowPerPlayer());   // 103.00 x 3
        assertEquals(new BigDecimal("750"), guide.highPerPlayer());  // 250.00 x 3
    }

    /**
     * The regression this class exists for. Asked to price request 53 itself, the model
     * returned $3,200, exactly 8 x the $400 budget, citing req 7 as its basis. req 14 is
     * the same shape as the request and prices $2,656. The suggestion now comes from the
     * data, so the budget can no longer be the answer by coincidence.
     */
    @Test
    void theSuggestionIsNotTheClientsBudget() {
        PriceGuide guide = PriceGuide.from(MYRTLE_BEACH, request53());

        BigDecimal budgetTotal = new BigDecimal("3200.00");
        assertEquals(-1, guide.suggestedTotal().compareTo(budgetTotal),
                "the suggested total must come from past trips, not from the budget");
    }

    /**
     * req 14 is 3 rounds and 2 nights, exactly what request 53 asks for. req 7 is cheaper
     * and req 38 is newer, and neither is the honest comparable.
     */
    @Test
    void theClosestMatchIsTheOneWithTheSameRoundsAndNights() {
        PriceGuide guide = PriceGuide.from(MYRTLE_BEACH, request53());

        assertSame(REQ_14, guide.closestMatch());
        assertEquals(0, PriceGuide.shapeDistance(REQ_14, request53()));
        assertEquals(1, PriceGuide.shapeDistance(REQ_7, request53()));
        // 8p 3n 4r against a request for 3 rounds and 2 nights: |4-3| + |3-2| = 2.
        assertEquals(2, PriceGuide.shapeDistance(REQ_26, request53()));
    }

    @Test
    void copesWithASingleComparable() {
        PriceGuide guide = PriceGuide.from(List.of(REQ_14), request53());

        // $110.67 x 3 = $332.01, to the dollar.
        assertEquals(new BigDecimal("332"), guide.suggestedPerPlayer());
        assertEquals(1, guide.sampleSize());
        assertSame(REQ_14, guide.closestMatch());
    }
}
