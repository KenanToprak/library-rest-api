package com.library.library_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Request payload for creating or updating a book.
 * Trims title/author and strips spaces/hyphens from ISBN prior to validation.
 */
@Schema(description = "Request payload for creating or updating a book")
public record BookRequest(

        @Schema(description = "Book title", example = "Test Book Title", maxLength = 255)
        @NotBlank(message = "Book title cannot be empty or null!")
        @Size(max = 255, message = "Book title must not exceed 255 characters!")
        String title,

        @Schema(description = "Book author", example = "Test Author", maxLength = 255)
        @NotBlank(message = "Author name is required!")
        @Size(max = 255, message = "Author name must not exceed 255 characters!")
        String author,

        @Schema(
                description = "10-digit or 13-digit ISBN (hyphens and spaces are stripped; ISBN-10 may end with X)",
                example = "9780000000001"
        )
        @NotBlank(message = "ISBN cannot be empty!")
        @Pattern(
                regexp = "^(?:\\d{9}[\\dX]|\\d{13})$",
                message = "ISBN must be a valid 10-digit or 13-digit ISBN (ISBN-10 may end with X)!"
        )
        String isbn
) {

    public BookRequest {
        title = title == null ? null : title.trim();
        author = author == null ? null : author.trim();
        isbn = isbn == null ? null : isbn.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
    }
}
