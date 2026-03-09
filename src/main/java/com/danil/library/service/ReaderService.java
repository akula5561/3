package com.danil.library.service;

import com.danil.library.dto.CreateReaderRequest;
import com.danil.library.dto.ReaderDto;
import com.danil.library.dto.UpdateReaderRequest;
import com.danil.library.exception.NotFoundException;
import com.danil.library.model.Reader;
import com.danil.library.repository.ReaderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReaderService {
    private final ReaderRepository repo;

    public ReaderService(ReaderRepository repo) { this.repo = repo; }

    public ReaderDto create(CreateReaderRequest req) {
        Reader r = new Reader(null, req.getName(), req.getEmail(), req.getPhone());
        return toDto(repo.save(r));
    }

    public List<ReaderDto> getAll() { return repo.findAll().stream().map(this::toDto).toList(); }
    public Page<ReaderDto> getPage(Pageable pageable) { return repo.findAll(pageable).map(this::toDto); }

    public ReaderDto getById(Long id) {
        Reader r = repo.findById(id).orElseThrow(() -> new NotFoundException("Читатель id=" + id + " не найден"));
        return toDto(r);
    }

    public ReaderDto update(Long id, UpdateReaderRequest req) {
        Reader r = repo.findById(id).orElseThrow(() -> new NotFoundException("Читатель id=" + id + " не найден"));
        r.setName(req.getName());
        r.setEmail(req.getEmail());
        r.setPhone(req.getPhone());
        return toDto(repo.save(r));
    }

    public void delete(Long id) {
        Reader r = repo.findById(id).orElseThrow(() -> new NotFoundException("Читатель id=" + id + " не найден"));
        repo.delete(r);
    }

    private ReaderDto toDto(Reader r) {
        return new ReaderDto(r.getId(), r.getName(), r.getEmail(), r.getPhone());
    }
}
