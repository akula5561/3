package com.danil.library.controller;

import com.danil.library.dto.CreateLoanRequest;
import com.danil.library.dto.LoanDto;
import com.danil.library.dto.OverdueLoanDto;
import com.danil.library.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService service;
    public LoanController(LoanService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<LoanDto> issue(@Valid @RequestBody CreateLoanRequest req) {
        LoanDto saved = service.issue(req);
        return ResponseEntity.created(URI.create("/api/loans/" + saved.getId())).body(saved);
    }

    @PatchMapping("/{id}/return")
    public LoanDto returnLoan(@PathVariable Long id) {
        return service.returnLoan(id);
    }

    @GetMapping
    public List<LoanDto> all(@RequestParam(value = "active", required = false) Boolean active) {
        if (active == null) return service.getAll();
        return active ? service.getActive() : service.getReturned();
    }

    @GetMapping("/reader/{readerId}")
    public List<LoanDto> byReader(@PathVariable Long readerId) { return service.byReader(readerId); }

    @GetMapping("/overdue")
    public List<OverdueLoanDto> overdue() { return service.overdue(); }

    @GetMapping("/{id}/fine")
    public OverdueLoanDto fine(@PathVariable Long id) { return service.fineForLoan(id); }
}
