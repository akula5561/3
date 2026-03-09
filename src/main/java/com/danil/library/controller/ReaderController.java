package com.danil.library.controller;

import com.danil.library.dto.CreateReaderRequest;
import com.danil.library.dto.ReaderDto;
import com.danil.library.dto.UpdateReaderRequest;
import com.danil.library.service.ReaderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/readers")
public class ReaderController {

    private final ReaderService service;
    public ReaderController(ReaderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ReaderDto> create(@Valid @RequestBody CreateReaderRequest req) {
        ReaderDto saved = service.create(req);
        return ResponseEntity.created(URI.create("/api/readers/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<ReaderDto> getAll() { return service.getAll(); }

    @GetMapping("/page")
    public Page<ReaderDto> page(Pageable pageable) { return service.getPage(pageable); }

    @GetMapping("/{id}")
    public ReaderDto get(@PathVariable Long id) { return service.getById(id); }

    @PutMapping("/{id}")
    public ReaderDto update(@PathVariable Long id, @Valid @RequestBody UpdateReaderRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
