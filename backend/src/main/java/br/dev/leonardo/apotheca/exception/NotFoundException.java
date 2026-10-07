package br.dev.leonardo.apotheca.exception;

/**
 * Thrown when a resource does not exist or the current user may not see it.
 * Both cases answer 404, so the API never reveals that another household's data exists.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

}
