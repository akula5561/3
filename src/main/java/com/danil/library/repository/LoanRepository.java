package com.danil.library.repository;

import com.danil.library.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    /* --- Чтение по читателю --- */
    // форма с вложенным свойством через CamelCase
    List<Loan> findByReaderId(Long readerId);
    // эквивалентная форма с подчёркиванием
    List<Loan> findByReader_Id(Long readerId);

    /* --- Проверка "книга на руках" (returnDate IS NULL) --- */
    // CamelCase
    boolean existsByBookIdAndReturnDateIsNull(Long bookId);
    // Эквивалент с подчёркиванием
    boolean existsByBook_IdAndReturnDateIsNull(Long bookId);

    /* --- Подборки по возврату --- */
    List<Loan> findByReturnDateIsNull();
    List<Loan> findByReturnDateIsNotNull();

    /* --- Просрочки: не возвращена и срок сдачи прошёл --- */
    List<Loan> findByReturnDateIsNullAndDueDateBefore(LocalDate date);
}
