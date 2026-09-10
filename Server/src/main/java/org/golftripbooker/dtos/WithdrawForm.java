package org.golftripbooker.dtos;

/**
 * "I am pulling this back, and here is why." The note is required, on the same terms as
 * the note a client owes when they counter -- BookingService decides how short is too
 * short, so both floors live beside the rules they belong to rather than in a form.
 */
public class WithdrawForm {

    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
