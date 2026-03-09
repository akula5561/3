package com.danil.library.web;

import com.danil.library.dto.BookDto;
import com.danil.library.exception.NotFoundException;
import com.danil.library.model.Author;
import com.danil.library.model.Book;
import com.danil.library.repository.AuthorRepository;
import com.danil.library.repository.BookRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/books/query") // итоговый URL: /api/books/query/**
public class BookQueryController {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookQueryController(BookRepository bookRepository,
                               AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    /** Книги по автору */
    @GetMapping("/author/{authorId}")
    public List<BookDto> byAuthor(@PathVariable Long authorId) {
        Author a = authorRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));

        return bookRepository.findAll().stream()
                .filter(b -> b.getAuthor() != null && a.getId().equals(b.getAuthor().getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* маппер в твой BookDto */
    private BookDto toDto(Book b) {
        Long authorId = (b.getAuthor() != null) ? b.getAuthor().getId() : null;

        BookDto dto = new BookDto();
        dto.setId(b.getId());
        dto.setTitle(b.getTitle());
        dto.setPublishedYear(b.getPublishedYear()); // Integer
        dto.setAvailable(b.isAvailable());          // boolean
        dto.setAuthorId(authorId);
        return dto;
    }
}
