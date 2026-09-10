package org.golftripbooker.dtos;

import org.golftripbooker.models.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The host's booking form. `courses` arrives as one string with a course per line,
 * exactly as the textarea holds it.
 *
 * bookedBy and bookedAt are absent on purpose: the host comes from the JWT and the
 * timestamp from the server clock.
 */
public class BookingForm {

    private String courses;
    private String lodging;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalCost;
    private String itinerary;

    public Booking toModel(int requestId, int bookingId) {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setRequestId(requestId);
        booking.setCourses(courses);
        booking.setLodging(lodging);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setTotalCost(totalCost);
        booking.setItinerary(itinerary);
        return booking;
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
}
