package com.jonschang.audio;

public class UnsupportedAudioFormatException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -746600329771092044L;

	public UnsupportedAudioFormatException() {
	}

	public UnsupportedAudioFormatException(String message) {
		super(message);
	}

	public UnsupportedAudioFormatException(Throwable cause) {
		super(cause);
	}

	public UnsupportedAudioFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedAudioFormatException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
