package com.library.library_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void saveAndFindByIsbn_persistsAndFindsEntity() {
        Book saved = bookRepository.saveAndFlush(new Book("Test Book Title", "Test Author", "9780000000001"));

        assertThat(saved.getId()).isNotNull();
        assertThat(bookRepository.findByIsbn("9780000000001"))
                .isPresent()
                .get()
                .extracting(Book::getTitle)
                .isEqualTo("Test Book Title");
    }

    @Test
    void save_duplicateIsbn_throwsDataIntegrityViolationException() {
        bookRepository.saveAndFlush(new Book("Test Book One", "Author One", "9780000000002"));

        assertThatThrownBy(() ->
                bookRepository.saveAndFlush(new Book("Test Book Two", "Author Two", "9780000000002")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByIsbnAndIdNot_distinguishesSameRecordFromOtherRecords() {
        Book first = bookRepository.saveAndFlush(new Book("First Book", "First Author", "9780000000003"));
        Book second = bookRepository.saveAndFlush(new Book("Second Book", "Second Author", "9780000000004"));

        assertThat(bookRepository.existsByIsbn("9780000000003")).isTrue();
        assertThat(bookRepository.existsByIsbnAndIdNot("9780000000003", first.getId())).isFalse();
        assertThat(bookRepository.existsByIsbnAndIdNot("9780000000003", second.getId())).isTrue();
    }

    @Test
    void searchWithPagination_filtersCaseInsensitively() {
        bookRepository.saveAndFlush(new Book("Alpha Guide", "Alice Test", "9780000000010"));
        bookRepository.saveAndFlush(new Book("Beta Handbook", "Bob Test", "9780000000011"));
        bookRepository.saveAndFlush(new Book("Gamma Notes", "Alice Test", "9780000000012"));

        Page<Book> byAuthor = bookRepository
                .findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCaseAndIsbnContainingIgnoreCase(
                        "", "alice", "", PageRequest.of(0, 10));

        assertThat(byAuthor.getTotalElements()).isEqualTo(2);
        assertThat(byAuthor.getContent())
                .extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Alpha Guide", "Gamma Notes");
    }
}
