package com.danil.library.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateLoanRequest {
    @NotNull
    private Long bookId;
    @NotNull
    private Long readerId;
    private LocalDate dueDate; // опционально

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Long getReaderId() { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
