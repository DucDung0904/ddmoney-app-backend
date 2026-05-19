package com.dung.ddmoney.controller;

import com.dung.ddmoney.dto.ExpenseBookDto;
import com.dung.ddmoney.entity.Transaction;
import com.dung.ddmoney.service.ExpenseBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expense-book")
@RequiredArgsConstructor
public class ExpenseBookController {

    private final ExpenseBookService expenseBookService;

    @GetMapping("/summary")
    public ExpenseBookDto.Summary getSummary(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return expenseBookService.getSummary(fromDate, toDate);
    }

    @GetMapping("/transactions")
    public List<ExpenseBookDto.TransactionItem> getTransactions(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "type", required = false) Transaction.TransactionType type,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "walletId", required = false) Long walletId
    ) {
        return expenseBookService.getTransactions(fromDate, toDate, type, categoryId, walletId);
    }

    @GetMapping("/category-statistics")
    public List<ExpenseBookDto.CategoryStatistic> getCategoryStatistics(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return expenseBookService.getCategoryStatistics(fromDate, toDate);
    }

    @GetMapping("/daily-summary")
    public List<ExpenseBookDto.DailySummary> getDailySummary(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return expenseBookService.getDailySummary(fromDate, toDate);
    }
}
