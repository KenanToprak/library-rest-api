package com.library.library_api.exception;

/**
 * Thrown when attempting to create or update a book with an ISBN that already belongs to another book.
 */
public class DuplicateIsbnException extends RuntimeException {

    public DuplicateIsbnException(String isbn) {
        super("A book with ISBN '" + isbn + "' already exists.");
    }
}
