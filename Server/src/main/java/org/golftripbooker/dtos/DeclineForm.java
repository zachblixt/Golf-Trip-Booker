package org.golftripbooker.dtos;

/** "Decline with a reason." The reason is required, so the client is never left guessing. */
public class DeclineForm {

    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
