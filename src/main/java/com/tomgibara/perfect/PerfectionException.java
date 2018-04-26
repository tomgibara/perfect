package com.tomgibara.perfect;

/**
 * This exception is raised when an element of the API has failed to generate a suitable perfect hash.
 *
 * @author Tom Gibara
 */
public class PerfectionException extends RuntimeException {

	private static final long serialVersionUID = 2978793906449343420L;

	PerfectionException() { }

	PerfectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	PerfectionException(String message, Throwable cause) {
		super(message, cause);
	}

	PerfectionException(String message) {
		super(message);
	}

	PerfectionException(Throwable cause) {
		super(cause);
	}

}
