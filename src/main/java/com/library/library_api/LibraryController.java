package com.library.library_api;

import com.library.library_api.dto.BookRequest;
import com.library.library_api.dto.BookResponse;
import com.library.library_api.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller exposing CRUD, search, and pagination endpoints for library books.
 */
@Validated
@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "CRUD and search endpoints for library books")
public class LibraryController {

    private final BookService bookService;

    public LibraryController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(
            summary = "List books",
            description = "Returns a paginated list of books with optional case-insensitive filtering by title, author, or ISBN."
    )
    @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @Parameter(description = "Optional title substring filter")
            @RequestParam(required = false) String title,
            @Parameter(description = "Optional author substring filter")
            @RequestParam(required = false) String author,
            @Parameter(description = "Optional ISBN substring filter")
            @RequestParam(required = false) String isbn,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(title, author, isbn, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a book by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(responseCode = "400", description = "Invalid book ID"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookResponse> getBookById(
            @Parameter(description = "Positive book identifier", example = "1")
            @PathVariable @Positive(message = "Book id must be positive.") Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new book")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created"),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request"),
            @ApiResponse(responseCode = "409", description = "A book with the same ISBN already exists")
    })
    public ResponseEntity<BookResponse> addBook(@Valid @RequestBody BookRequest request) {
        BookResponse created = bookService.addBook(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing book")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed, malformed request, or invalid book ID"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "409", description = "A different book with the same ISBN already exists")
    })
    public ResponseEntity<BookResponse> updateBook(
            @Parameter(description = "Positive book identifier", example = "1")
            @PathVariable @Positive(message = "Book id must be positive.") Long id,
            @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid book ID"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "Positive book identifier", example = "1")
            @PathVariable @Positive(message = "Book id must be positive.") Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
