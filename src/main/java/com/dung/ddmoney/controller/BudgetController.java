package com.dung.ddmoney.controller;

import com.dung.ddmoney.dto.BudgetDto;
import com.dung.ddmoney.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetDto.Response>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(budgetService.getBudgets(month, year));
    }

    @GetMapping("/current")
    public ResponseEntity<List<BudgetDto.Response>> getCurrentBudgets() {
        return ResponseEntity.ok(budgetService.getCurrentBudgets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto.DetailResponse> getBudgetDetail(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetDetail(id));
    }

    @PostMapping
    public ResponseEntity<BudgetDto.Response> createBudget(@Valid @RequestBody BudgetDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto.Response> updateBudget(@PathVariable Long id,
                                                           @Valid @RequestBody BudgetDto.Request request) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
