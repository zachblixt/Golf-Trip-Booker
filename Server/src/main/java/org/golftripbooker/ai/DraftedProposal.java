package org.golftripbooker.ai;

import org.golftripbooker.models.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What the model came back with, before anyone has agreed to any of it.
 *
 * Note what is missing: no bookingId, no bookedBy, no bookedAt, no status. A draft is not
 * a proposal and never becomes one on its own. A host reads this, edits whatever they
 * like, and the thing they send is an ordinary BookingForm going through the ordinary
 * endpoint.
 */
public record DraftedProposal(List<String> courses,
                              String lodging,
                              LocalDate startDate,
                              LocalDate endDate,
                              BigDecimal totalCost,
                              String itinerary,
                              String rationale) {

    /** The shape the existing validation rules expect. Courses are one per line. */
    public Booking toBooking(int requestId) {
        Booking booking = new Booking();
        booking.setRequestId(requestId);
        booking.setCourses(courses == null ? "" : String.join("\n", courses));
        booking.setLodging(lodging);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setTotalCost(totalCost);
        booking.setItinerary(itinerary);
        return booking;
    }
}
