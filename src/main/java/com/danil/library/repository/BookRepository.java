package com.danil.library.repository;

import com.danil.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByAuthorId(Long authorId);
    List<Book> findByTitleContainingIgnoreCase(String titlePart);
    List<Book> findByAvailableTrue();
}
