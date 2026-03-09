package com.danil.library.controller;

import com.danil.library.dto.BookDto;
import com.danil.library.dto.CreateBookRequest;
import com.danil.library.dto.UpdateBookRequest;
import com.danil.library.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/books") // итоговый URL: /api/books/
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    /* ==== CRUD ==== */

    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody CreateBookRequest req) {
        BookDto saved = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/books/" + saved.getId()))
                .body(saved);
    }

    @GetMapping
    public List<BookDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/page")
    public Page<BookDto> getPage(Pageable pageable) {
        return service.getPage(pageable);
    }

    @GetMapping("/{id}")
    public BookDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public BookDto update(@PathVariable Long id, @Valid @RequestBody UpdateBookRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /* ==== Бизнес-операции над книгами () ==== */

    // найти все доступные
    @GetMapping("/available")
    public List<BookDto> listAvailable() {
        return service.listAvailable();
    }

    // отдать книгу читателю
    @PatchMapping("/{id}/borrow")
    public BookDto borrow(@PathVariable Long id) {
        return service.borrow(id);
    }

    // вернуть книгу
    @PatchMapping("/{id}/return")
    public BookDto giveBack(@PathVariable Long id) {
        return service.giveBack(id);
    }

    // поиск по названию
    @GetMapping("/search")
    public List<BookDto> search(@RequestParam("q") String q) {
        return service.searchByTitle(q);
    }
}
