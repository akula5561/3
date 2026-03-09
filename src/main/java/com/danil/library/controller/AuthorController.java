package com.danil.library.controller;

import com.danil.library.dto.AuthorDto;
import com.danil.library.dto.CreateAuthorRequest;
import com.danil.library.dto.UpdateAuthorRequest;
import com.danil.library.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    private final AuthorService service;

    public AuthorController(AuthorService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<AuthorDto> create(@RequestBody CreateAuthorRequest req) {
        AuthorDto saved = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/authors/" + saved.getId()))  // или "/authors/" при глобальном префиксе
                .body(saved);
    }

    // LIST
    @GetMapping
    public List<AuthorDto> list() {
        return service.getAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public AuthorDto get(@PathVariable Long id) {
        return service.getById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public AuthorDto update(@PathVariable Long id, @RequestBody UpdateAuthorRequest req) {
        return service.update(id, req);   // ВАЖНО: UpdateAuthorRequest
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
