package org.ivdnt.openconvert.filehandling;

public class SimpleProcessException extends Exception {
	public SimpleProcessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SimpleProcessException(String message, Throwable cause) {
		super(message, cause);
	}

	public SimpleProcessException(String message) {
		super(message);
	}

	public SimpleProcessException(Throwable cause) {
		super(cause);
	}
}
