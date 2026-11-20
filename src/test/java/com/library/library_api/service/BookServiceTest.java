package com.library.library_api.service;

import com.library.library_api.Book;
import com.library.library_api.BookRepository;
import com.library.library_api.dto.BookRequest;
import com.library.library_api.dto.BookResponse;
import com.library.library_api.exception.BookNotFoundException;
import com.library.library_api.exception.DuplicateIsbnException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void getAllBooks_withoutFilters_returnsPagedResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        Book book = persistedBook(1L, "Test Book Title", "Test Author", "9780000000001");
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(book), pageable, 1));

        Page<BookResponse> result = bookService.getAllBooks(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst())
                .isEqualTo(new BookResponse(1L, "Test Book Title", "Test Author", "9780000000001"));
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void getAllBooks_withFilters_normalizesFiltersAndQueriesRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book = persistedBook(2L, "Test Book Two", "Second Author", "9780000000002");
        when(bookRepository.findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndIsbnContainingIgnoreCase(
                "Book", "Second", "9780000000002", pageable))
                .thenReturn(new PageImpl<>(List.of(book), pageable, 1));

        Page<BookResponse> result = bookService.getAllBooks("  Book  ", " Second ", "978-0-00-000000-2", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(2L);
    }

    @Test
    void getBookById_whenPresent_returnsBookResponse() {
        Book book = persistedBook(10L, "Test Book Title", "Test Author", "9780000000001");
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        BookResponse response = bookService.getBookById(10L);

        assertThat(response).isEqualTo(new BookResponse(10L, "Test Book Title", "Test Author", "9780000000001"));
    }

    @Test
    void getBookById_whenMissing_throwsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void addBook_whenIsbnIsUnique_persistsNormalizedBookAndReturnsId() {
        BookRequest request = new BookRequest("  Test Book Title  ", "  Test Author  ", "000000000x");
        when(bookRepository.existsByIsbn("000000000X")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book toSave = invocation.getArgument(0);
            toSave.setId(5L);
            return toSave;
        });

        BookResponse response = bookService.addBook(request);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book savedArg = captor.getValue();
        assertThat(savedArg.getTitle()).isEqualTo("Test Book Title");
        assertThat(savedArg.getAuthor()).isEqualTo("Test Author");
        assertThat(savedArg.getIsbn()).isEqualTo("000000000X");
        assertThat(response).isEqualTo(new BookResponse(5L, "Test Book Title", "Test Author", "000000000X"));
    }

    @Test
    void addBook_whenDuplicateIsbn_throwsDuplicateIsbnExceptionWithoutSaving() {
        BookRequest request = new BookRequest("Test Book Title", "Test Author", "9780000000001");
        when(bookRepository.existsByIsbn("9780000000001")).thenReturn(true);

        assertThatThrownBy(() -> bookService.addBook(request))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining("9780000000001");

        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBook_whenSameBookKeepsIsbn_updatesSuccessfully() {
        Book existing = persistedBook(3L, "Old Title", "Old Author", "9780000000003");
        BookRequest request = new BookRequest("Updated Title", "Updated Author", "9780000000003");
        when(bookRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(bookRepository.existsByIsbnAndIdNot("9780000000003", 3L)).thenReturn(false);
        when(bookRepository.save(existing)).thenReturn(existing);

        BookResponse response = bookService.updateBook(3L, request);

        assertThat(response).isEqualTo(new BookResponse(3L, "Updated Title", "Updated Author", "9780000000003"));
    }

    @Test
    void updateBook_whenIsbnBelongsToAnotherBook_throwsDuplicateIsbnException() {
        Book existing = persistedBook(3L, "Old Title", "Old Author", "9780000000003");
        BookRequest request = new BookRequest("Updated Title", "Updated Author", "9780000000004");
        when(bookRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(bookRepository.existsByIsbnAndIdNot("9780000000004", 3L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.updateBook(3L, request))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining("9780000000004");

        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBook_whenBookMissing_throwsBookNotFoundException() {
        BookRequest request = new BookRequest("Updated Title", "Updated Author", "9780000000003");
        when(bookRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(77L, request))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void deleteBook_whenPresent_deletesEntity() {
        Book existing = persistedBook(4L, "Test Book Title", "Test Author", "9780000000004");
        when(bookRepository.findById(4L)).thenReturn(Optional.of(existing));

        bookService.deleteBook(4L);

        verify(bookRepository).delete(existing);
    }

    @Test
    void deleteBook_whenMissing_throwsBookNotFoundException() {
        when(bookRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(88L))
                .isInstanceOf(BookNotFoundException.class);

        verify(bookRepository, never()).delete(any());
    }

    private static Book persistedBook(Long id, String title, String author, String isbn) {
        Book book = new Book(title, author, isbn);
        book.setId(id);
        return book;
    }
}
