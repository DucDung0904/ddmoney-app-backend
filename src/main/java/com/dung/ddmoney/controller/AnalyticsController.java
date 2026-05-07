package com.dung.ddmoney.controller;

import com.dung.ddmoney.dto.TransactionDto;
import com.dung.ddmoney.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TransactionService transactionService;

    // GET /api/analytics/summary?month=4&year=2026
    @GetMapping("/summary")
    public TransactionDto.Summary getSummary(
            @RequestParam(value = "month", defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month,
            @RequestParam(value = "year", defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return transactionService.getMonthlySummary(month, year);
    }

    // GET /api/analytics/monthly-chart?months=4
    @GetMapping("/monthly-chart")
    public List<TransactionDto.MonthlyChart> getMonthlyChart(
            @RequestParam(value = "months", defaultValue = "4") int months) {
        return transactionService.getMonthlyChart(months);
    }

    // GET /api/analytics/category-spending?month=4&year=2026
    @GetMapping("/category-spending")
    public List<TransactionDto.CategorySpending> getCategorySpending(
            @RequestParam(value = "month", defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month,
            @RequestParam(value = "year", defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return transactionService.getCategorySpending(month, year);
    }
}
