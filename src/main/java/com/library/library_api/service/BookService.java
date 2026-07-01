package com.library.library_api.service;

import com.library.library_api.Book;
import com.library.library_api.BookRepository;
import com.library.library_api.dto.BookDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookDto> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(book -> new BookDto(book.getTitle(), book.getAuthor(), book.getIsbn()))
                .collect(Collectors.toList());
    }

    public BookDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        return new BookDto(book.getTitle(), book.getAuthor(), book.getIsbn());
    }

    public BookDto addBook(BookDto bookDto) {
        Book newBook = new Book(bookDto.title(), bookDto.author(), bookDto.isbn());
        Book savedBook = bookRepository.save(newBook);
        return new BookDto(savedBook.getTitle(), savedBook.getAuthor(), savedBook.getIsbn());
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }
}