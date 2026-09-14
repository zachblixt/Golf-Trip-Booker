package org.golftripbooker.ai;

import org.golftripbooker.data.PastTrip;
import org.golftripbooker.models.TripRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * The price, worked out in Java rather than asked for in a prompt.
 *
 * The first version of this feature let the model price the trip. It returned a total of
 * exactly the client's budget to the dollar, cited the least similar past trip as its
 * basis, and described a calculation that had not happened. That is the ordinary failure:
 * given a budget and a request for a number, a language model tends to return the budget
 * and narrate backwards from it.
 *
 * So arithmetic moved here and the model keeps the jobs it is good at, choosing courses
 * and explaining the choice. The budget is no longer the only number in the prompt that
 * looks like an answer.
 *
 * The method is deliberately simple enough to state in one sentence, because it goes into
 * the prompt and a host may read it: the median cost per player per round across confirmed
 * trips at this destination, multiplied by the rounds requested.
 *
 * Known limitation: it prices on rounds and ignores nights, because with a handful of
 * trips per destination there is not enough data to separate a lodging rate from a green
 * fee. A destination whose trips vary a lot in nights will estimate worse.
 */
public record PriceGuide(BigDecimal suggestedPerPlayer,
                         BigDecimal lowPerPlayer,
                         BigDecimal highPerPlayer,
                         BigDecimal suggestedTotal,
                         PastTrip closestMatch,
                         int sampleSize) {

    public static PriceGuide from(List<PastTrip> trips, TripRequest request) {
        List<BigDecimal> perRound = trips.stream()
                .filter(t -> t.roundsRequested() > 0 && t.playerCount() > 0)
                .map(t -> t.costPerPlayer()
                        .divide(BigDecimal.valueOf(t.roundsRequested()), 2, RoundingMode.HALF_UP))
                .sorted()
                .toList();

        BigDecimal rounds = BigDecimal.valueOf(request.getRoundsRequested());
        BigDecimal players = BigDecimal.valueOf(request.getPlayerCount());

        BigDecimal median = median(perRound);
        BigDecimal suggested = median.multiply(rounds).setScale(0, RoundingMode.HALF_UP);

        return new PriceGuide(
                suggested,
                perRound.get(0).multiply(rounds).setScale(0, RoundingMode.HALF_UP),
                perRound.get(perRound.size() - 1).multiply(rounds).setScale(0, RoundingMode.HALF_UP),
                suggested.multiply(players).setScale(2, RoundingMode.HALF_UP),
                closestInShape(trips, request),
                perRound.size());
    }

    private static BigDecimal median(List<BigDecimal> sorted) {
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1)
                .add(sorted.get(n / 2))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    /**
     * Closest by shape, not by date or price. A trip of the same rounds and nights is the
     * honest comparable even if it is older or had a different party size, and pointing at
     * it by name is what stops the model reaching for whichever one suits its answer.
     */
    static PastTrip closestInShape(List<PastTrip> trips, TripRequest request) {
        return trips.stream()
                .min(Comparator.comparingInt(t -> shapeDistance(t, request)))
                .orElse(null);
    }

    static int shapeDistance(PastTrip trip, TripRequest request) {
        return Math.abs(trip.roundsRequested() - request.getRoundsRequested())
                + Math.abs(trip.nights() - request.getNights());
    }
}
