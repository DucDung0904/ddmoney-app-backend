package com.dung.ddmoney.controller;

import com.dung.ddmoney.dto.CategoryDto;
import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto.Response> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/expense")
    public List<CategoryDto.Response> getExpense() {
        return categoryService.getByTypes(List.of(
                Category.CategoryType.EXPENSE, Category.CategoryType.BOTH));
    }

    @GetMapping("/income")
    public List<CategoryDto.Response> getIncome() {
        return categoryService.getByTypes(List.of(
                Category.CategoryType.INCOME, Category.CategoryType.BOTH));
    }

    @GetMapping("/debt")
    public List<CategoryDto.Response> getDebt() {
        return categoryService.getByTypes(List.of(Category.CategoryType.DEBT));
    }

    @GetMapping("/{id}")
    public CategoryDto.Response getById(@PathVariable("id") Long id) {
        return categoryService.getById(id);
    }

    @PostMapping
    public ResponseEntity<CategoryDto.Response> create(@Valid @RequestBody CategoryDto.Request req) {
        return ResponseEntity.status(201).body(categoryService.create(req));
    }

    @PutMapping("/{id}")
    public CategoryDto.Response update(@PathVariable("id") Long id, @Valid @RequestBody CategoryDto.Request req) {
        return categoryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
