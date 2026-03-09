package com.danil.library.service;

import com.danil.library.dto.BookDto;
import com.danil.library.dto.CreateBookRequest;
import com.danil.library.dto.UpdateBookRequest;
import com.danil.library.exception.NotFoundException;
import com.danil.library.model.Author;
import com.danil.library.model.Book;
import com.danil.library.repository.AuthorRepository;
import com.danil.library.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepo;
    private final AuthorRepository authorRepo;

    public BookService(BookRepository bookRepo, AuthorRepository authorRepo) {
        this.bookRepo = bookRepo; this.authorRepo = authorRepo;
    }

    public BookDto create(CreateBookRequest req) {
        Author author = authorRepo.findById(req.getAuthorId())
                .orElseThrow(() -> new NotFoundException("Автор id=" + req.getAuthorId() + " не найден"));
        Book b = new Book(null, req.getTitle(), req.getPublishedYear(),
                req.getAvailable() == null ? true : req.getAvailable(), author);
        return toDto(bookRepo.save(b));
    }

    public List<BookDto> getAll() { return bookRepo.findAll().stream().map(this::toDto).toList(); }
    public Page<BookDto> getPage(Pageable pageable) { return bookRepo.findAll(pageable).map(this::toDto); }

    public BookDto getById(Long id) {
        Book b = bookRepo.findById(id).orElseThrow(() -> new NotFoundException("Книга id=" + id + " не найдена"));
        return toDto(b);
    }

    public BookDto update(Long id, UpdateBookRequest req) {
        Book b = bookRepo.findById(id).orElseThrow(() -> new NotFoundException("Книга id=" + id + " не найдена"));
        Author author = authorRepo.findById(req.getAuthorId())
                .orElseThrow(() -> new NotFoundException("Автор id=" + req.getAuthorId() + " не найден"));
        b.setTitle(req.getTitle());
        b.setPublishedYear(req.getPublishedYear());
        b.setAuthor(author);
        b.setAvailable(Boolean.TRUE.equals(req.getAvailable()));
        return toDto(bookRepo.save(b));
    }

    public void delete(Long id) {
        Book b = bookRepo.findById(id).orElseThrow(() -> new NotFoundException("Книга id=" + id + " не найдена"));
        bookRepo.delete(b);
    }

    public List<BookDto> findByAuthor(Long authorId) {
        return bookRepo.findByAuthorId(authorId).stream().map(this::toDto).toList();
    }

    public List<BookDto> searchByTitle(String q) {
        return bookRepo.findByTitleContainingIgnoreCase(q).stream().map(this::toDto).toList();
    }

    public List<BookDto> listAvailable() {
        return bookRepo.findByAvailableTrue().stream().map(this::toDto).toList();
    }

    public BookDto borrow(Long id) {
        Book b = bookRepo.findById(id).orElseThrow(() -> new NotFoundException("Книга id=" + id + " не найдена"));
        if (!b.isAvailable()) return toDto(b);
        b.setAvailable(false);
        return toDto(bookRepo.save(b));
    }

    public BookDto giveBack(Long id) {
        Book b = bookRepo.findById(id).orElseThrow(() -> new NotFoundException("Книга id=" + id + " не найдена"));
        if (b.isAvailable()) return toDto(b);
        b.setAvailable(true);
        return toDto(bookRepo.save(b));
    }

    private BookDto toDto(Book b) {
        return new BookDto(b.getId(), b.getTitle(), b.getPublishedYear(), b.isAvailable(),
                b.getAuthor() != null ? b.getAuthor().getId() : null);
    }
}
