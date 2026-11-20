package com.library.library_api.exception;

/**
 * Thrown when a requested book cannot be found in the repository.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }

    public BookNotFoundException(String message) {
        super(message);
    }
}
