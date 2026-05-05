package com.dung.ddmoney.controller;

import com.dung.ddmoney.dto.BudgetDto;
import com.dung.ddmoney.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // GET /api/budgets?month=4&year=2026
    @GetMapping
    public List<BudgetDto.Response> getAll(
            @RequestParam(value = "month", defaultValue = "0") int month,
            @RequestParam(value = "year", defaultValue = "0") int year) {
        if (month == 0) month = LocalDate.now().getMonthValue();
        if (year == 0) year = LocalDate.now().getYear();
        return budgetService.getByMonthYear(month, year);
    }

    @PostMapping
    public ResponseEntity<BudgetDto.Response> create(@Valid @RequestBody BudgetDto.Request req) {
        return ResponseEntity.status(201).body(budgetService.create(req));
    }

    @PutMapping("/{id}")
    public BudgetDto.Response update(@PathVariable("id") Long id, @Valid @RequestBody BudgetDto.Request req) {
        return budgetService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
