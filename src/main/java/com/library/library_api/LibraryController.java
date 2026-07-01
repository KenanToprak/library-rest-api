package com.library.library_api;

import com.library.library_api.dto.BookDto;
import com.library.library_api.service.BookService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class LibraryController {

    // Artık Repository değil, Service ile konuşuyor
    private final BookService bookService;

    public LibraryController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookDto> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public BookDto getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @PostMapping
    public BookDto addBook(@Valid @RequestBody BookDto newBookDto) {
        return bookService.addBook(newBookDto);
    }

    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return "Book with ID " + id + " has been deleted successfully.";
    }
}