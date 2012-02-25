package com.nostra13.socialsharing.twitter.extpack.winterwell.json;

/**
 * The JSONException is thrown by the JSON.org classes then things are amiss.
 * 
 * Tweaked to be a RuntimeException so it's slightly less annoying -- DBW
 * @author JSON.org
 * @version 2
 */
public class JSONException extends RuntimeException {
    private Throwable cause;

    /**
     * Constructs a JSONException with an explanatory message.
     * @param message Detail about the reason for the exception.
     */
    public JSONException(String message) {
        super(message);
    }

    public JSONException(Throwable t) {
        super(t.getMessage());
        this.cause = t;
    }

    @Override
	public Throwable getCause() {
        return this.cause;
    }
}
