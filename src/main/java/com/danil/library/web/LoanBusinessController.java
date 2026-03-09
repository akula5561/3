package com.danil.library.web;

import com.danil.library.dto.CreateLoanRequest;
import com.danil.library.dto.LoanDto;
import com.danil.library.dto.OverdueLoanDto;
import com.danil.library.service.LoanBusinessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans/ops") // итоговый путь: /api/loans/ops/**
public class LoanBusinessController {

    private final LoanBusinessService loanService;

    public LoanBusinessController(LoanBusinessService loanService) {
        this.loanService = loanService;
    }

    /** 1) Выдать книгу */
    @PostMapping("/borrow")
    public ResponseEntity<LoanDto> borrow(@RequestBody CreateLoanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrow(req));
    }

    /** 2) Вернуть книгу */
    @PatchMapping("/{loanId}/return")
    public LoanDto returnLoan(@PathVariable Long loanId) {
        return loanService.returnLoan(loanId);
    }

    /** 3) Продлить выдачу */
    @PatchMapping("/{loanId}/extend")
    public LoanDto extend(@PathVariable Long loanId,
                          @RequestParam(defaultValue = "7") int days) {
        return loanService.extend(loanId, days);
    }

    /** 4) Просроченные выдачи */
    @GetMapping("/overdue")
    public List<OverdueLoanDto> overdue() {
        return loanService.getOverdue();
    }

    //  метод /reader/{readerId} не здесь Он остаётся в LoanController
}
