package com.danil.library.dto;

import java.time.LocalDate;

public class OverdueLoanDto {
    private Long loanId;
    private Long bookId;
    private Long readerId;
    private LocalDate dueDate;
    private LocalDate asOfDate;
    private long daysOverdue;
    private long fine;
    private boolean active;

    public OverdueLoanDto() {}

    public OverdueLoanDto(Long loanId, Long bookId, Long readerId, LocalDate dueDate,
                          LocalDate asOfDate, long daysOverdue, long fine, boolean active) {
        this.loanId = loanId; this.bookId = bookId; this.readerId = readerId;
        this.dueDate = dueDate; this.asOfDate = asOfDate; this.daysOverdue = daysOverdue;
        this.fine = fine; this.active = active;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Long getReaderId() { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public long getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }
    public long getFine() { return fine; }
    public void setFine(long fine) { this.fine = fine; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
