package org.golftripbooker.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * What the host arranged. Courses are one blob of text with one name per line --
 * the post-MVP story "pick each course and round date individually" is what turns
 * this column into its own table.
 */
public class Booking {

    private int bookingId;
    private int requestId;
    private String courses;
    private String lodging;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalCost;
    private String itinerary;
    private int bookedBy;
    private LocalDateTime bookedAt;

    /**
     * Null until the client accepts. Every host submission starts as a proposal, so this
     * is the only thing separating a trip that was arranged from one that was agreed.
     */
    private LocalDateTime confirmedAt;

    /**
     * How this proposal died, if it did. countered_at is the client refusing it, with a
     * reason and a note; withdrawnAt is the host pulling it back before anyone answered.
     * A row carries at most one of confirmedAt, counteredAt and withdrawnAt.
     */
    private LocalDateTime counteredAt;
    private CounterReason counterReason;
    private String counterNote;
    private LocalDateTime withdrawnAt;
    private String withdrawNote;

    public Booking() {
    }

    /** A live proposal: nobody has accepted it, refused it, or pulled it back. */
    public Booking(int bookingId, int requestId, String courses, String lodging, LocalDate startDate,
                   LocalDate endDate, BigDecimal totalCost, String itinerary, int bookedBy,
                   LocalDateTime bookedAt) {
        this(bookingId, requestId, courses, lodging, startDate, endDate, totalCost, itinerary,
                bookedBy, bookedAt, null);
    }

    public Booking(int bookingId, int requestId, String courses, String lodging, LocalDate startDate,
                   LocalDate endDate, BigDecimal totalCost, String itinerary, int bookedBy,
                   LocalDateTime bookedAt, LocalDateTime confirmedAt) {
        this(bookingId, requestId, courses, lodging, startDate, endDate, totalCost, itinerary,
                bookedBy, bookedAt, confirmedAt, null, null, null, null, null);
    }

    /** The whole row, which is what the mapper needs and nothing else does. */
    public Booking(int bookingId, int requestId, String courses, String lodging, LocalDate startDate,
                   LocalDate endDate, BigDecimal totalCost, String itinerary, int bookedBy,
                   LocalDateTime bookedAt, LocalDateTime confirmedAt, LocalDateTime counteredAt,
                   CounterReason counterReason, String counterNote, LocalDateTime withdrawnAt,
                   String withdrawNote) {
        this.confirmedAt = confirmedAt;
        this.counteredAt = counteredAt;
        this.counterReason = counterReason;
        this.counterNote = counterNote;
        this.withdrawnAt = withdrawnAt;
        this.withdrawNote = withdrawNote;
        this.bookingId = bookingId;
        this.requestId = requestId;
        this.courses = courses;
        this.lodging = lodging;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCost = totalCost;
        this.itinerary = itinerary;
        this.bookedBy = bookedBy;
        this.bookedAt = bookedAt;
    }

    /** True once the client has agreed to it. Nothing else sets this. */
    public boolean isConfirmed() {
        return confirmedAt != null;
    }

    /** The client refused it. The row survives so the host can read why. */
    public boolean isCountered() {
        return counteredAt != null;
    }

    /** The host pulled it back before the client answered either way. */
    public boolean isWithdrawn() {
        return withdrawnAt != null;
    }

    /**
     * Still waiting on the client. This is the predicate findLiveProposal encodes in SQL;
     * having it here too means a test can assert the rule without going near a database.
     */
    public boolean isLive() {
        return confirmedAt == null && counteredAt == null && withdrawnAt == null;
    }

    /** Blank lines are dropped so a stray newline in the host's form does not become a course. */
    public List<String> courseList() {
        if (courses == null || courses.isBlank()) {
            return List.of();
        }
        return Arrays.stream(courses.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getCourses() {
        return courses;
    }

    public void setCourses(String courses) {
        this.courses = courses;
    }

    public String getLodging() {
        return lodging;
    }

    public void setLodging(String lodging) {
        this.lodging = lodging;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getItinerary() {
        return itinerary;
    }

    public void setItinerary(String itinerary) {
        this.itinerary = itinerary;
    }

    public int getBookedBy() {
        return bookedBy;
    }

    public void setBookedBy(int bookedBy) {
        this.bookedBy = bookedBy;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(LocalDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCounteredAt() {
        return counteredAt;
    }

    public void setCounteredAt(LocalDateTime counteredAt) {
        this.counteredAt = counteredAt;
    }

    public CounterReason getCounterReason() {
        return counterReason;
    }

    public void setCounterReason(CounterReason counterReason) {
        this.counterReason = counterReason;
    }

    public String getCounterNote() {
        return counterNote;
    }

    public void setCounterNote(String counterNote) {
        this.counterNote = counterNote;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public String getWithdrawNote() {
        return withdrawNote;
    }

    public void setWithdrawNote(String withdrawNote) {
        this.withdrawNote = withdrawNote;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return bookingId == booking.bookingId
                && requestId == booking.requestId
                && bookedBy == booking.bookedBy
                && Objects.equals(courses, booking.courses)
                && Objects.equals(lodging, booking.lodging)
                && Objects.equals(startDate, booking.startDate)
                && Objects.equals(endDate, booking.endDate)
                && Objects.equals(totalCost, booking.totalCost)
                && Objects.equals(itinerary, booking.itinerary)
                && Objects.equals(bookedAt, booking.bookedAt)
                && Objects.equals(confirmedAt, booking.confirmedAt)
                && Objects.equals(counteredAt, booking.counteredAt)
                && counterReason == booking.counterReason
                && Objects.equals(counterNote, booking.counterNote)
                && Objects.equals(withdrawnAt, booking.withdrawnAt)
                && Objects.equals(withdrawNote, booking.withdrawNote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, requestId, courses, lodging, startDate, endDate,
                totalCost, itinerary, bookedBy, bookedAt, confirmedAt, counteredAt,
                counterReason, counterNote, withdrawnAt, withdrawNote);
    }
}
