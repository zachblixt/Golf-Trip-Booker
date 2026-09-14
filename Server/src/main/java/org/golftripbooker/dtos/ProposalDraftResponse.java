package org.golftripbooker.dtos;

import org.golftripbooker.ai.DraftedProposal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A draft on its way to the host's screen.
 *
 * Shaped to drop straight into the fields of BookingForm, so the screen can prefill and
 * the host edits an ordinary form. `rationale` is the one field with no counterpart --
 * it explains the suggestion and is never submitted anywhere.
 */
public class ProposalDraftResponse {

    private List<String> courses;
    private String lodging;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalCost;
    private String itinerary;
    private String rationale;

    public static ProposalDraftResponse from(DraftedProposal drafted) {
        ProposalDraftResponse response = new ProposalDraftResponse();
        response.courses = drafted.courses();
        response.lodging = drafted.lodging();
        response.startDate = drafted.startDate();
        response.endDate = drafted.endDate();
        response.totalCost = drafted.totalCost();
        response.itinerary = drafted.itinerary();
        response.rationale = drafted.rationale();
        return response;
    }

    public List<String> getCourses() { return courses; }
    public String getLodging() { return lodging; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getTotalCost() { return totalCost; }
    public String getItinerary() { return itinerary; }
    public String getRationale() { return rationale; }
}
