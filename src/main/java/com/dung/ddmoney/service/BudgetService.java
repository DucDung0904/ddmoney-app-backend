package com.dung.ddmoney.service;

import com.dung.ddmoney.dto.BudgetDto;
import com.dung.ddmoney.dto.TransactionDto;
import com.dung.ddmoney.entity.Budget;
import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.repository.BudgetRepository;
import com.dung.ddmoney.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;
    private final com.dung.ddmoney.repository.UserRepository userRepository;
    private final com.dung.ddmoney.util.SecurityUtils securityUtils;

    private com.dung.ddmoney.entity.User getCurrentUser() {
        return userRepository.findByEmail(com.dung.ddmoney.util.SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<BudgetDto.Response> getByMonthYear(int month, int year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearOrdered(getCurrentUser().getId(), month, year);

        // Get spending for this month
        List<TransactionDto.CategorySpending> spending = transactionService.getCategorySpending(month, year);
        Map<Long, BigDecimal> spendingMap = spending.stream()
                .collect(Collectors.toMap(
                        TransactionDto.CategorySpending::getCategoryId,
                        TransactionDto.CategorySpending::getAmount
                ));

        return budgets.stream().map(b -> {
            BudgetDto.Response r = BudgetDto.Response.from(b);
            BigDecimal spent = spendingMap.getOrDefault(b.getCategory().getId(), BigDecimal.ZERO);
            r.setSpentAmount(spent);
            r.setRemainingAmount(b.getAmount().subtract(spent));
            r.setPercentage(b.getAmount().compareTo(BigDecimal.ZERO) == 0 ? 0f
                    : spent.divide(b.getAmount(), 4, RoundingMode.HALF_UP).floatValue());
            return r;
        }).toList();
    }

    @Transactional
    public BudgetDto.Response create(BudgetDto.Request req) {
        Category cat = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        // Upsert: if exists for this category/month/year, update amount
        Budget budget = budgetRepository
                .findByUserIdAndCategoryIdAndMonthAndYear(getCurrentUser().getId(), req.getCategoryId(), req.getMonth(), req.getYear())
                .orElse(Budget.builder()
                        .user(getCurrentUser())
                        .category(cat)
                        .month(req.getMonth())
                        .year(req.getYear())
                        .build());

        budget.setAmount(req.getAmount());
        return BudgetDto.Response.from(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetDto.Response update(Long id, BudgetDto.Request req) {
        Budget budget = findOrThrow(id);
        budget.setAmount(req.getAmount());
        return BudgetDto.Response.from(budgetRepository.save(budget));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        budgetRepository.deleteById(id);
    }

    private Budget findOrThrow(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngân sách id=" + id));
        if (!budget.getUser().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Không có quyền truy cập ngân sách này");
        }
        return budget;
    }
}
