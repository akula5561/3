package com.danil.library.service;

import com.danil.library.config.LibraryProperties;
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
public class LoanService {

    private final LoanRepository loanRepo;
    private final BookRepository bookRepo;
    private final ReaderRepository readerRepo;
    private final LibraryProperties props;

    public LoanService(LoanRepository loanRepo, BookRepository bookRepo,
                       ReaderRepository readerRepo, LibraryProperties props) {
        this.loanRepo = loanRepo; this.bookRepo = bookRepo; this.readerRepo = readerRepo; this.props = props;
    }

    @Transactional
    public LoanDto issue(CreateLoanRequest req) {
        Book book = bookRepo.findById(req.getBookId())
                .orElseThrow(() -> new NotFoundException("Книга id=" + req.getBookId() + " не найдена"));
        if (!book.isAvailable() || loanRepo.existsByBookIdAndReturnDateIsNull(book.getId())) {
            throw new IllegalStateException("Книга уже выдана");
        }
        Reader reader = readerRepo.findById(req.getReaderId())
                .orElseThrow(() -> new NotFoundException("Читатель id=" + req.getReaderId() + " не найден"));

        LocalDate now = LocalDate.now();
        int defaultDays = props.getLoan().getDefaultDays();
        LocalDate due = req.getDueDate() != null ? req.getDueDate() : now.plusDays(defaultDays);

        Loan loan = new Loan(null, book, reader, now, due, null);
        book.setAvailable(false);

        loan = loanRepo.save(loan);
        bookRepo.save(book);

        return toDto(loan);
    }

    @Transactional
    public LoanDto returnLoan(Long loanId) {
        Loan loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Выдача id=" + loanId + " не найдена"));
        if (loan.getReturnDate() == null) {
            loan.setReturnDate(LocalDate.now());
            Book book = loan.getBook();
            book.setAvailable(true);
            bookRepo.save(book);
            loanRepo.save(loan);
        }
        return toDto(loan);
    }

    public List<LoanDto> getAll() { return loanRepo.findAll().stream().map(this::toDto).toList(); }
    public List<LoanDto> getActive() { return loanRepo.findByReturnDateIsNull().stream().map(this::toDto).toList(); }
    public List<LoanDto> getReturned() { return loanRepo.findByReturnDateIsNotNull().stream().map(this::toDto).toList(); }
    public List<LoanDto> byReader(Long readerId) {
        return loanRepo.findByReaderId(readerId).stream().map(this::toDto).toList();
    }

    public List<OverdueLoanDto> overdue() {
        LocalDate today = LocalDate.now();
        int daily = props.getFine().getDaily();
        return loanRepo.findByReturnDateIsNullAndDueDateBefore(today).stream()
                .map(l -> overdueOf(l, today, daily))
                .toList();
    }

    public OverdueLoanDto fineForLoan(Long loanId) {
        Loan l = loanRepo.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Выдача id=" + loanId + " не найдена"));
        int daily = props.getFine().getDaily();
        LocalDate today = LocalDate.now();
        return overdueOf(l, today, daily);
    }

    private OverdueLoanDto overdueOf(Loan l, LocalDate today, int daily) {
        boolean active = l.getReturnDate() == null;
        LocalDate asOf = active ? today : l.getReturnDate();
        long days = 0;
        if (asOf.isAfter(l.getDueDate())) {
            days = ChronoUnit.DAYS.between(l.getDueDate(), asOf);
        }
        long fine = days * daily;
        return new OverdueLoanDto(
                l.getId(),
                l.getBook() != null ? l.getBook().getId() : null,
                l.getReader() != null ? l.getReader().getId() : null,
                l.getDueDate(),
                asOf,
                days,
                fine,
                active
        );
    }

    private LoanDto toDto(Loan l) {
        return new LoanDto(
                l.getId(),
                l.getBook() != null ? l.getBook().getId() : null,
                l.getReader() != null ? l.getReader().getId() : null,
                l.getLoanDate(),
                l.getDueDate(),
                l.getReturnDate(),
                l.getReturnDate() == null
        );
    }
}
