package com.library.library_api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Structured error payload returned by {@link GlobalExceptionHandler}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured API error response")
public record ApiError(
        @Schema(description = "UTC timestamp when the error occurred", example = "2026-11-20T13:30:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable error summary", example = "Validation failed for one or more fields.")
        String message,

        @Schema(description = "Request path that triggered the error", example = "/api/books")
        String path,

        @Schema(description = "Validation error messages grouped by field name")
        Map<String, List<String>> fieldErrors
) {

    public ApiError(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}
