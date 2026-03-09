package com.danil.library.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fines")
@PreAuthorize("hasRole('ADMIN')")
public class FineController {

    private final Map<Long, Double> fines = new HashMap<>();

    @PostMapping
    public String issueFine(@RequestBody ManualFineRequest req) {
        fines.merge(req.readerId, req.amount, Double::sum);
        return "Штраф выдан. Текущий штраф: " + fines.get(req.readerId);
    }

    @DeleteMapping("/{readerId}")
    public String clearFine(@PathVariable Long readerId) {
        fines.remove(readerId);
        return "Штраф снят";
    }

    @GetMapping("/{readerId}")
    public double getFine(@PathVariable Long readerId) {
        return fines.getOrDefault(readerId, 0.0);
    }
}
