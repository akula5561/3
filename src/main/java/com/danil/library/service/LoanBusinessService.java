package com.danil.library.service;

import com.danil.library.dto.CreateLoanRequest;
import com.danil.library.dto.LoanDto;
import com.danil.library.dto.OverdueLoanDto;
import com.danil.library.exception.NotFoundException;
import com.danil.library.model.Book;
import com.danil.library.model.Loan;
import com.danil.library.model.Reader;
import com.danil.library.repository.BookRepository;
import com.danil.library.repository.LoanRepository;
import com.danil.library.repository.ReaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanBusinessService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;

    public LoanBusinessService(LoanRepository loanRepository,
                               BookRepository bookRepository,
                               ReaderRepository readerRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
    }

    /** 1) Выдать книгу читателю */
    @Transactional
    public LoanDto borrow(CreateLoanRequest req) {
        Book book = bookRepository.findById(req.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found"));
        Reader reader = readerRepository.findById(req.getReaderId())
                .orElseThrow(() -> new NotFoundException("Reader not found"));

        // уже выдана и не возвращена?
        if (loanRepository.existsByBook_IdAndReturnDateIsNull(book.getId())) {
            throw new IllegalStateException("Book is already loaned");
        }

        Loan l = new Loan();
        l.setBook(book);
        l.setReader(reader);
        l.setLoanDate(LocalDate.now());
        l.setDueDate(LocalDate.now().plusDays(14)); //
        l.setReturnDate(null);

        l = loanRepository.save(l);
        return toDto(l);
    }

    /** 2) Принять возврат */
    @Transactional
    public LoanDto returnLoan(Long loanId) {
        Loan l = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found"));
        l.setReturnDate(LocalDate.now());
        return toDto(l);
    }

    /** 3) Продлить срок */
    @Transactional
    public LoanDto extend(Long loanId, int days) {
        Loan l = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found"));
        int add = days > 0 ? days : 7;
        LocalDate base = l.getDueDate() != null ? l.getDueDate() : LocalDate.now();
        l.setDueDate(base.plusDays(add));
        return toDto(l);
    }

    /** 4) Просроченные: returnDate IS NULL AND dueDate < today */
    @Transactional(readOnly = true)
    public List<OverdueLoanDto> getOverdue() {
        LocalDate today = LocalDate.now();
        return loanRepository.findByReturnDateIsNullAndDueDateBefore(today).stream()
                .map(l -> {
                    OverdueLoanDto dto = new OverdueLoanDto();
                    dto.setLoanId(l.getId());
                    dto.setBookId(l.getBook() != null ? l.getBook().getId() : null);
                    dto.setReaderId(l.getReader() != null ? l.getReader().getId() : null);
                    dto.setDueDate(l.getDueDate());
                    dto.setAsOfDate(today);
                    long days = (l.getDueDate() != null) ? ChronoUnit.DAYS.between(l.getDueDate(), today) : 0;
                    if (days < 0) days = 0;
                    dto.setDaysOverdue(days);
                    // если есть политика штрафов — подставь здесь расчёт (days * finePerDay)
                    dto.setFine(0L);
                    dto.setActive(true);
                    return dto;
                })
                .toList();
    }

    /** 5) Все выдачи читателя */
    @Transactional(readOnly = true)
    public List<LoanDto> getReaderLoans(Long readerId) {
        return loanRepository.findByReader_Id(readerId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Маппинг строго под  LoanDto */
    private LoanDto toDto(Loan l) {
        LoanDto dto = new LoanDto();
        dto.setId(l.getId());
        dto.setBookId(l.getBook() != null ? l.getBook().getId() : null);
        dto.setReaderId(l.getReader() != null ? l.getReader().getId() : null);
        dto.setLoanDate(l.getLoanDate());
        dto.setDueDate(l.getDueDate());
        dto.setReturnDate(l.getReturnDate());
        dto.setActive(l.getReturnDate() == null);
        return dto;
    }
}
