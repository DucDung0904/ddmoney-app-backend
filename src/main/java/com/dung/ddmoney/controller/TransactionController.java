package com.dung.ddmoney.controller;

import com.dung.ddmoney.dto.TransactionDto;
import com.dung.ddmoney.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // GET /api/transactions?month=4&year=2026
    @GetMapping
    public List<TransactionDto.Response> getAll(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        if (month != null && year != null) {
            return transactionService.getByMonthYear(month, year);
        }
        return transactionService.getAll();
    }

    @GetMapping("/{id}")
    public TransactionDto.Response getById(@PathVariable("id") Long id) {
        return transactionService.getById(id);
    }

    // GET /api/transactions/summary?month=4&year=2026
    @GetMapping("/summary")
    public TransactionDto.Summary getSummary(
            @RequestParam(value = "month", defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month,
            @RequestParam(value = "year", defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return transactionService.getMonthlySummary(month, year);
    }

    // GET /api/transactions/monthly-chart?months=4
    @GetMapping("/monthly-chart")
    public List<TransactionDto.MonthlyChart> getMonthlyChart(
            @RequestParam(value = "months", defaultValue = "4") int months) {
        return transactionService.getMonthlyChart(months);
    }

    // GET /api/transactions/category-spending?month=4&year=2026
    @GetMapping("/category-spending")
    public List<TransactionDto.CategorySpending> getCategorySpending(
            @RequestParam(value = "month", defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month,
            @RequestParam(value = "year", defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return transactionService.getCategorySpending(month, year);
    }

    @PostMapping
    public ResponseEntity<TransactionDto.Response> create(@Valid @RequestBody TransactionDto.Request req) {
        return ResponseEntity.status(201).body(transactionService.create(req));
    }

    @PutMapping("/{id}")
    public TransactionDto.Response update(@PathVariable("id") Long id, @Valid @RequestBody TransactionDto.Request req) {
        return transactionService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
