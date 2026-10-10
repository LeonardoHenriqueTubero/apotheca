package br.dev.leonardo.apotheca.exception;

public class InvalidFieldException extends RuntimeException {

	private final String field;

	public InvalidFieldException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
