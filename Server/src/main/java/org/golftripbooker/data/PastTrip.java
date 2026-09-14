package org.golftripbooker.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A confirmed trip somebody already took at a destination, flattened for one job:
 * grounding a drafted proposal in what this place has actually cost and which courses
 * have actually been played there.
 *
 * A record rather than a model class because nothing ever writes one. It is a read shape
 * assembled by a join, and it never becomes a row.
 */
public record PastTrip(String courses,
                       String lodging,
                       int playerCount,
                       int nights,
                       int roundsRequested,
                       BigDecimal totalCost,
                       LocalDate startDate) {

    /**
     * What one person paid. This, not the total, is the number that compares across trips
     * of different party sizes, and it is the number the client's budget is expressed in.
     */
    public BigDecimal costPerPlayer() {
        if (playerCount <= 0 || totalCost == null) {
            return totalCost;
        }
        return totalCost.divide(BigDecimal.valueOf(playerCount), 2, RoundingMode.HALF_UP);
    }
}
