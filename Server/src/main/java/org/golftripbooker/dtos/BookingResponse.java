package org.golftripbooker.dtos;

import org.golftripbooker.models.Booking;
import org.golftripbooker.models.CounterReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Courses go out as an array rather than the raw newline-delimited column, so the
 * client renders a list without parsing anything. When courses become their own
 * table post-MVP, this shape does not change.
 */
public class BookingResponse {

    private final int bookingId;
    private final List<String> courses;
    private final String lodging;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal totalCost;
    private final String itinerary;
    private final LocalDateTime bookedAt;

    /**
     * False while this is a proposal the client has not answered. The client renders the
     * same block either way; this is what decides whether it comes with two buttons.
     */
    private final boolean confirmed;

    /**
     * How the proposal ended, for the host's history. All null on a live one, and on the
     * confirmed booking a client sees -- there is nothing to explain about a yes.
     */
    private final LocalDateTime counteredAt;
    private final CounterReason counterReason;
    private final String counterNote;
    private final LocalDateTime withdrawnAt;
    private final String withdrawNote;

    public static BookingResponse from(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingResponse(
                booking.getBookingId(),
                booking.courseList(),
                booking.getLodging(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getTotalCost(),
                booking.getItinerary(),
                booking.getBookedAt(),
                booking.isConfirmed(),
                booking.getCounteredAt(),
                booking.getCounterReason(),
                booking.getCounterNote(),
                booking.getWithdrawnAt(),
                booking.getWithdrawNote());
    }

    public BookingResponse(int bookingId, List<String> courses, String lodging, LocalDate startDate,
                           LocalDate endDate, BigDecimal totalCost, String itinerary,
                           LocalDateTime bookedAt, boolean confirmed, LocalDateTime counteredAt,
                           CounterReason counterReason, String counterNote,
                           LocalDateTime withdrawnAt, String withdrawNote) {
        this.bookingId = bookingId;
        this.courses = courses;
        this.lodging = lodging;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCost = totalCost;
        this.itinerary = itinerary;
        this.bookedAt = bookedAt;
        this.confirmed = confirmed;
        this.counteredAt = counteredAt;
        this.counterReason = counterReason;
        this.counterNote = counterNote;
        this.withdrawnAt = withdrawnAt;
        this.withdrawNote = withdrawNote;
    }

    public int getBookingId() {
        return bookingId;
    }

    public List<String> getCourses() {
        return courses;
    }

    public String getLodging() {
        return lodging;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public String getItinerary() {
        return itinerary;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public LocalDateTime getCounteredAt() {
        return counteredAt;
    }

    public CounterReason getCounterReason() {
        return counterReason;
    }

    public String getCounterNote() {
        return counterNote;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public String getWithdrawNote() {
        return withdrawNote;
    }
}
