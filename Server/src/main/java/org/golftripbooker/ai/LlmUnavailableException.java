package org.golftripbooker.ai;

/**
 * The model could not be reached, refused the request, or answered with something that
 * was not the structured object asked for.
 *
 * Checked on purpose. Drafting is an optional convenience laid on top of a form the host
 * can always fill in by hand, so every caller has to say out loud what it does when the
 * convenience is not there. An unchecked exception would let somebody forget, and the
 * first symptom would be a host unable to work because a third party was down.
 */
public class LlmUnavailableException extends Exception {

    public LlmUnavailableException(String message) {
        super(message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
