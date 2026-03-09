package com.danil.library.dto;

import java.time.LocalDate;

public class LoanDto {
    private Long id;
    private Long bookId;
    private Long readerId;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private boolean active;

    public LoanDto() {}
    public LoanDto(Long id, Long bookId, Long readerId, LocalDate loanDate,
                   LocalDate dueDate, LocalDate returnDate, boolean active) {
        this.id = id; this.bookId = bookId; this.readerId = readerId;
        this.loanDate = loanDate; this.dueDate = dueDate; this.returnDate = returnDate; this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Long getReaderId() { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
