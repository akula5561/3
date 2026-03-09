package com.danil.library;

import com.danil.library.config.LibraryProperties;
import com.danil.library.model.Author;
import com.danil.library.model.Book;
import com.danil.library.model.Reader;
import com.danil.library.repository.AuthorRepository;
import com.danil.library.repository.BookRepository;
import com.danil.library.repository.ReaderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(LibraryProperties.class)
public class Laba2RbpooApplication {

    public static void main(String[] args) {
        SpringApplication.run(Laba2RbpooApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(AuthorRepository authorRepo, BookRepository bookRepo, ReaderRepository readerRepo) {
        return args -> {
            if (authorRepo.count() == 0) {
                var orwell  = authorRepo.save(new Author(null, "George Orwell", "English novelist"));
                var rowling = authorRepo.save(new Author(null, "J. K. Rowling", "British author"));
                bookRepo.save(new Book(null, "1984", 1949, true, orwell));
                bookRepo.save(new Book(null, "Animal Farm", 1945, true, orwell));
                bookRepo.save(new Book(null, "Harry Potter 1", 1997, true, rowling));
            }
            if (readerRepo.count() == 0) {
                readerRepo.save(new Reader(null, "Ivan Ivanov", "ivan@example.com", "+79991234567"));
            }
        };
    }
}
