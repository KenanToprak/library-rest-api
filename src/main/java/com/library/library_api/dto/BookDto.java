package com.library.library_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookDto(

        @NotBlank(message = "Book title cannot be empty or null!")
        String title,

        @NotBlank(message = "Author name is required!")
        String author,

        @NotBlank(message = "ISBN cannot be empty!")
        @Size(min = 10, max = 13, message = "ISBN must be between 10 and 13 characters!")
        String isbn
) {
}