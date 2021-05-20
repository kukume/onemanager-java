package me.kuku.onemanager.exception;

public class VerifyFailedException extends RuntimeException{
	public VerifyFailedException() {
		super();
	}

	public VerifyFailedException(String message) {
		super(message);
	}
}
