package com.library.library_api;

import com.library.library_api.dto.BookRequest;
import com.library.library_api.dto.BookResponse;
import com.library.library_api.exception.BookNotFoundException;
import com.library.library_api.exception.DuplicateIsbnException;
import com.library.library_api.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryController.class)
class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void getAllBooks_returnsPaginatedBooksWithIds() throws Exception {
        BookResponse item = new BookResponse(1L, "Test Book Title", "Test Author", "9780000000001");
        when(bookService.getAllBooks(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Test Book Title"))
                .andExpect(jsonPath("$.content[0].author").value("Test Author"))
                .andExpect(jsonPath("$.content[0].isbn").value("9780000000001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getBookById_whenFound_returns200() throws Exception {
        when(bookService.getBookById(1L))
                .thenReturn(new BookResponse(1L, "Test Book Title", "Test Author", "9780000000001"));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book Title"));
    }

    @Test
    void getBookById_whenMissing_returnsStructured404() throws Exception {
        when(bookService.getBookById(404L)).thenThrow(new BookNotFoundException(404L));

        mockMvc.perform(get("/api/books/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with id: 404"))
                .andExpect(jsonPath("$.path").value("/api/books/404"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getBookById_whenNonPositiveId_returns400() throws Exception {
        mockMvc.perform(get("/api/books/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.id", hasItem("Book id must be positive.")));
    }

    @Test
    void getBookById_whenTypeMismatch_returns400InsteadOf404() throws Exception {
        mockMvc.perform(get("/api/books/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'."));
    }

    @Test
    void addBook_whenValid_returns201WithLocationHeaderAndId() throws Exception {
        when(bookService.addBook(any(BookRequest.class)))
                .thenReturn(new BookResponse(12L, "Test Book Title", "Test Author", "9780000000001"));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test Book Title",
                                  "author": "Test Author",
                                  "isbn": "978-0-00-000000-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/books/12")))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.isbn").value("9780000000001"));
    }

    @Test
    void addBook_whenInvalidFields_returns400WithAllFieldErrorsCollected() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "author": "",
                                  "isbn": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.title", hasItem("Book title cannot be empty or null!")))
                .andExpect(jsonPath("$.fieldErrors.author", hasItem("Author name is required!")))
                .andExpect(jsonPath("$.fieldErrors.isbn", hasItem("ISBN cannot be empty!")))
                .andExpect(jsonPath("$.fieldErrors.isbn", hasItem(containsString("10-digit or 13-digit"))));
    }

    @Test
    void addBook_whenMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body."));
    }

    @Test
    void addBook_whenDuplicateIsbn_returns409Conflict() throws Exception {
        when(bookService.addBook(any(BookRequest.class)))
                .thenThrow(new DuplicateIsbnException("9780000000001"));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test Book Title",
                                  "author": "Test Author",
                                  "isbn": "9780000000001"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void updateBook_whenValid_returns200() throws Exception {
        when(bookService.updateBook(eq(1L), any(BookRequest.class)))
                .thenReturn(new BookResponse(1L, "Updated Title", "Updated Author", "9780000000001"));

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "author": "Updated Author",
                                  "isbn": "9780000000001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void deleteBook_whenExists_returns204NoContent() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBook_whenMissing_returns404() throws Exception {
        doThrow(new BookNotFoundException(999L)).when(bookService).deleteBook(999L);

        mockMvc.perform(delete("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unexpectedRuntimeException_returns500WithoutLeakingInternalDetails() throws Exception {
        when(bookService.getBookById(1L)).thenThrow(new RuntimeException("internal-sql-credential-leak"));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred."))
                .andExpect(jsonPath("$.message", not(containsString("internal-sql-credential-leak"))));
    }
}
