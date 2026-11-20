package com.library.library_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload representing a persisted book resource.
 */
@Schema(description = "Book resource returned by the API")
public record BookResponse(
        @Schema(description = "Unique identifier of the book", example = "1")
        Long id,

        @Schema(description = "Book title", example = "Spring Architecture Guide")
        String title,

        @Schema(description = "Book author", example = "Jane Doe")
        String author,

        @Schema(description = "Normalized ISBN-10 or ISBN-13", example = "9780000000001")
        String isbn
) {
}
