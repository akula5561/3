package com.danil.library.service;

import com.danil.library.dto.AuthorDto;
import com.danil.library.dto.CreateAuthorRequest;
import com.danil.library.dto.UpdateAuthorRequest;
import com.danil.library.exception.NotFoundException;
import com.danil.library.model.Author;
import com.danil.library.repository.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {
    private final AuthorRepository authorRepo;

    public AuthorService(AuthorRepository authorRepo) { this.authorRepo = authorRepo; }

    public AuthorDto create(CreateAuthorRequest req) {
        Author a = new Author(null, req.getName(), req.getBio());
        a = authorRepo.save(a);
        return toDto(a);
    }

    public List<AuthorDto> getAll() { return authorRepo.findAll().stream().map(this::toDto).toList(); }

    public Page<AuthorDto> getPage(Pageable pageable) {
        return authorRepo.findAll(pageable).map(this::toDto);
    }

    public AuthorDto getById(Long id) {
        Author a = authorRepo.findById(id).orElseThrow(() -> new NotFoundException("Автор id=" + id + " не найден"));
        return toDto(a);
    }

    public AuthorDto update(Long id, UpdateAuthorRequest req) {
        Author a = authorRepo.findById(id).orElseThrow(() -> new NotFoundException("Автор id=" + id + " не найден"));
        a.setName(req.getName());
        a.setBio(req.getBio());
        return toDto(authorRepo.save(a));
    }

    public void delete(Long id) {
        Author a = authorRepo.findById(id).orElseThrow(() -> new NotFoundException("Автор id=" + id + " не найден"));
        authorRepo.delete(a);
    }

    private AuthorDto toDto(Author a) { return new AuthorDto(a.getId(), a.getName(), a.getBio()); }
}
