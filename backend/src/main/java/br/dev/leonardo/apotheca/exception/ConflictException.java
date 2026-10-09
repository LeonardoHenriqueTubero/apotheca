package br.dev.leonardo.apotheca.exception;

/** Thrown when a valid request clashes with the current data, e.g. a duplicate name. Answers 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
