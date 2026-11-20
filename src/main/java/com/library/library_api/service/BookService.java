package com.library.library_api.service;

import com.library.library_api.Book;
import com.library.library_api.BookRepository;
import com.library.library_api.dto.BookRequest;
import com.library.library_api.dto.BookResponse;
import com.library.library_api.exception.BookNotFoundException;
import com.library.library_api.exception.DuplicateIsbnException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Application service managing library book operations and transaction boundaries.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Returns a paginated list of books, optionally filtered by title, author, and/or ISBN.
     */
    public Page<BookResponse> getAllBooks(String title, String author, String isbn, Pageable pageable) {
        String titleFilter = normalizeFilter(title);
        String authorFilter = normalizeFilter(author);
        String isbnFilter = normalizeIsbnFilter(isbn);

        if (titleFilter.isEmpty() && authorFilter.isEmpty() && isbnFilter.isEmpty()) {
            return bookRepository.findAll(pageable).map(this::toResponse);
        }

        return bookRepository
                .findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndIsbnContainingIgnoreCase(
                        titleFilter, authorFilter, isbnFilter, pageable)
                .map(this::toResponse);
    }

    /**
     * Finds a single book by its identifier.
     */
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        return toResponse(book);
    }

    /**
     * Creates a new book after normalizing input fields and verifying ISBN uniqueness.
     */
    @Transactional
    public BookResponse addBook(BookRequest request) {
        String title = request.title().trim();
        String author = request.author().trim();
        String isbn = normalizeIsbn(request.isbn());

        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }

        Book newBook = new Book(title, author, isbn);
        Book savedBook = bookRepository.save(newBook);
        return toResponse(savedBook);
    }

    /**
     * Updates an existing book by ID while enforcing ISBN uniqueness across other books.
     */
    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        String title = request.title().trim();
        String author = request.author().trim();
        String isbn = normalizeIsbn(request.isbn());

        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new DuplicateIsbnException(isbn);
        }

        existing.setTitle(title);
        existing.setAuthor(author);
        existing.setIsbn(isbn);

        Book updated = bookRepository.save(existing);
        return toResponse(updated);
    }

    /**
     * Deletes an existing book atomically inside a transaction.
     */
    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        bookRepository.delete(book);
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn());
    }

    private String normalizeIsbn(String isbn) {
        return isbn.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeIsbnFilter(String isbn) {
        if (isbn == null) {
            return "";
        }
        return isbn.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
    }
}
