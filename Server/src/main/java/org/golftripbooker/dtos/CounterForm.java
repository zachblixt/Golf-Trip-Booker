package org.golftripbooker.dtos;

import org.golftripbooker.models.CounterReason;

/**
 * "Not this, and here is why." Both fields are required; TripRequestService decides how
 * short a note is too short, so the rule lives with every other rule rather than here.
 */
public class CounterForm {

    private CounterReason reason;
    private String note;

    public CounterReason getReason() {
        return reason;
    }

    public void setReason(CounterReason reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
