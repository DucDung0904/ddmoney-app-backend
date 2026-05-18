package com.dung.ddmoney.service;

import com.dung.ddmoney.dto.BudgetDto;
import com.dung.ddmoney.dto.TransactionDto;
import com.dung.ddmoney.entity.Budget;
import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.entity.Transaction;
import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.entity.Wallet;
import com.dung.ddmoney.repository.BudgetRepository;
import com.dung.ddmoney.repository.CategoryRepository;
import com.dung.ddmoney.repository.TransactionRepository;
import com.dung.ddmoney.repository.UserRepository;
import com.dung.ddmoney.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public List<BudgetDto.Response> getBudgets(Integer month, Integer year) {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();
        int targetMonth = month != null ? month : today.getMonthValue();
        int targetYear = year != null ? year : today.getYear();
        return budgetRepository.findByUserIdAndMonthAndYearOrdered(user.getId(), targetMonth, targetYear)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BudgetDto.Response> getCurrentBudgets() {
        User user = getCurrentUser();
        return budgetRepository.findCurrentActiveBudgets(user.getId(), LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }
    public List<BudgetDto.Response> findImpactedBudgetWarnings(Transaction transaction) {
        if (transaction == null || transaction.getType() != Transaction.TransactionType.EXPENSE) {
            return List.of();
        }
        Long userId = transaction.getUser().getId();
        LocalDate transactionDate = transaction.getDate();
        return budgetRepository.findCurrentActiveBudgets(userId, transactionDate)
                .stream()
                .filter(budget -> isTransactionIncludedInBudget(transaction, budget))
                .map(this::toResponse)
                .filter(response -> response.getStatus() == Budget.BudgetStatus.WARNING
                        || response.getStatus() == Budget.BudgetStatus.EXCEEDED)
                .toList();
    }

    @Transactional
    public BudgetDto.Response createBudget(BudgetDto.Request request) {
        User user = getCurrentUser();
        validateAmount(request.getAmount());

        BudgetInput input = normalizeInput(request, user, null);
        ensureNoDuplicate(user.getId(), input.category(), input.wallet(), input.startDate(), input.endDate(), null);

        Budget budget = Budget.builder()
                .user(user)
                .name(request.getName().trim())
                .amount(request.getAmount())
                .category(input.category())
                .wallet(input.wallet())
                .scope(input.scope())
                .walletScope(input.walletScope())
                .periodType(input.periodType())
                .repeatType(input.repeatType())
                .startDate(input.startDate())
                .endDate(input.endDate())
                .active(true)
                .month(input.startDate().getMonthValue())
                .year(input.startDate().getYear())
                .build();

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetDto.Response updateBudget(Long id, BudgetDto.Request request) {
        User user = getCurrentUser();
        Budget budget = findUserBudget(id, user);
        validateAmount(request.getAmount());

        BudgetInput input = normalizeInput(request, user, budget);
        ensureNoDuplicate(user.getId(), input.category(), input.wallet(), input.startDate(), input.endDate(), id);

        budget.setName(request.getName().trim());
        budget.setAmount(request.getAmount());
        budget.setCategory(input.category());
        budget.setWallet(input.wallet());
        budget.setScope(input.scope());
        budget.setWalletScope(input.walletScope());
        budget.setPeriodType(input.periodType());
        budget.setRepeatType(input.repeatType());
        budget.setStartDate(input.startDate());
        budget.setEndDate(input.endDate());
        budget.setMonth(input.startDate().getMonthValue());
        budget.setYear(input.startDate().getYear());

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long id) {
        User user = getCurrentUser();
        Budget budget = findUserBudget(id, user);
        budget.setActive(false);
        budgetRepository.save(budget);
    }

    public BudgetDto.DetailResponse getBudgetDetail(Long id) {
        User user = getCurrentUser();
        Budget budget = findUserBudget(id, user);
        BigDecimal spentAmount = calculateSpentAmount(budget);
        BudgetDto.DetailResponse response = BudgetDto.DetailResponse.from(budget, spentAmount);

        BigDecimal remaining = budget.getAmount().subtract(spentAmount).max(BigDecimal.ZERO);
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(budget.getStartDate(), budget.getEndDate()) + 1);
        LocalDate today = LocalDate.now();
        LocalDate dayForElapsed = today.isBefore(budget.getStartDate()) ? budget.getStartDate()
                : today.isAfter(budget.getEndDate()) ? budget.getEndDate() : today;
        long elapsedDays = Math.max(1, ChronoUnit.DAYS.between(budget.getStartDate(), dayForElapsed) + 1);
        long remainingDays = today.isAfter(budget.getEndDate())
                ? 0
                : Math.max(1, ChronoUnit.DAYS.between(dayForElapsed, budget.getEndDate()) + 1);

        BigDecimal actualDailySpending = spentAmount.divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP);
        BigDecimal projectedSpending = actualDailySpending.multiply(BigDecimal.valueOf(totalDays)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dailySuggestedAmount = remainingDays == 0
                ? BigDecimal.ZERO
                : remaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);

        response.setActualDailySpending(actualDailySpending);
        response.setProjectedSpending(projectedSpending);
        response.setDailySuggestedAmount(dailySuggestedAmount);
        response.setSpentByCategory(getSpentByCategory(budget, spentAmount));
        response.setTransactions(findTransactionsForBudget(budget));
        return response;
    }

    public BigDecimal calculateSpentAmount(Budget budget) {
        Long categoryId = budget.getScope() == Budget.BudgetScope.CATEGORY && budget.getCategory() != null
                ? budget.getCategory().getId()
                : null;
        Long walletId = budget.getWalletScope() == Budget.WalletScope.ONE_WALLET && budget.getWallet() != null
                ? budget.getWallet().getId()
                : null;

        BigDecimal total = transactionRepository.sumExpenseForBudget(
                budget.getUser().getId(),
                Transaction.TransactionType.EXPENSE,
                budget.getStartDate(),
                budget.getEndDate(),
                categoryId,
                walletId
        );
        return total == null ? BigDecimal.ZERO : total.setScale(2, RoundingMode.HALF_UP);
    }

    public Budget.BudgetStatus determineBudgetStatus(BigDecimal spentAmount, BigDecimal budgetAmount) {
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Budget.BudgetStatus.SAFE;
        }
        BigDecimal percentUsed = spentAmount.multiply(BigDecimal.valueOf(100))
                .divide(budgetAmount, 2, RoundingMode.HALF_UP);
        if (percentUsed.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return Budget.BudgetStatus.EXCEEDED;
        }
        if (percentUsed.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return Budget.BudgetStatus.WARNING;
        }
        return Budget.BudgetStatus.SAFE;
    }

    private BudgetDto.Response toResponse(Budget budget) {
        return BudgetDto.Response.from(budget, calculateSpentAmount(budget));
    }
    private boolean isTransactionIncludedInBudget(Transaction transaction, Budget budget) {
        boolean categoryMatches = budget.getScope() == Budget.BudgetScope.ALL_CATEGORIES
                || (budget.getCategory() != null
                && transaction.getCategory() != null
                && budget.getCategory().getId().equals(transaction.getCategory().getId()));
        boolean walletMatches = budget.getWalletScope() == Budget.WalletScope.ALL_WALLETS
                || (budget.getWallet() != null
                && transaction.getWallet() != null
                && budget.getWallet().getId().equals(transaction.getWallet().getId()));
        return categoryMatches && walletMatches;
    }

    private List<BudgetDto.CategorySpendingResponse> getSpentByCategory(Budget budget, BigDecimal totalSpent) {
        Long categoryId = budget.getScope() == Budget.BudgetScope.CATEGORY && budget.getCategory() != null
                ? budget.getCategory().getId()
                : null;
        Long walletId = budget.getWalletScope() == Budget.WalletScope.ONE_WALLET && budget.getWallet() != null
                ? budget.getWallet().getId()
                : null;

        return transactionRepository.sumExpenseGroupByCategoryForBudget(
                        budget.getUser().getId(),
                        Transaction.TransactionType.EXPENSE,
                        budget.getStartDate(),
                        budget.getEndDate(),
                        categoryId,
                        walletId
                )
                .stream()
                .map(row -> mapCategorySpending(row, totalSpent))
                .toList();
    }

    private BudgetDto.CategorySpendingResponse mapCategorySpending(Object[] row, BigDecimal totalSpent) {
        BudgetDto.CategorySpendingResponse response = new BudgetDto.CategorySpendingResponse();
        BigDecimal amount = ((BigDecimal) row[4]).setScale(2, RoundingMode.HALF_UP);
        response.setCategoryId((Long) row[0]);
        response.setCategoryName((String) row[1]);
        response.setIcon((String) row[2]);
        response.setColorHex((String) row[3]);
        response.setAmount(amount);
        response.setPercentUsed(totalSpent.compareTo(BigDecimal.ZERO) > 0
                ? amount.multiply(BigDecimal.valueOf(100)).divide(totalSpent, 2, RoundingMode.HALF_UP).floatValue()
                : 0f);
        return response;
    }

    private List<TransactionDto.Response> findTransactionsForBudget(Budget budget) {
        Long categoryId = budget.getScope() == Budget.BudgetScope.CATEGORY && budget.getCategory() != null
                ? budget.getCategory().getId()
                : null;
        Long walletId = budget.getWalletScope() == Budget.WalletScope.ONE_WALLET && budget.getWallet() != null
                ? budget.getWallet().getId()
                : null;

        return transactionRepository.findExpensesForBudget(
                        budget.getUser().getId(),
                        Transaction.TransactionType.EXPENSE,
                        budget.getStartDate(),
                        budget.getEndDate(),
                        categoryId,
                        walletId
                )
                .stream()
                .map(TransactionDto.Response::from)
                .toList();
    }

    private BudgetInput normalizeInput(BudgetDto.Request request, User user, Budget existingBudget) {
        Budget.PeriodType periodType = request.getPeriodType() != null
                ? request.getPeriodType()
                : existingBudget != null ? existingBudget.getPeriodType() : Budget.PeriodType.MONTH;
        DateRange dateRange = resolveDateRange(request, periodType, existingBudget);
        Budget.RepeatType repeatType = resolveRepeatType(request, periodType, existingBudget);
        validateDateAndRepeat(dateRange.startDate(), dateRange.endDate(), periodType, repeatType);

        Long categoryId = resolveCategoryId(request);
        Budget.BudgetScope scope = request.getScope() != null
                ? request.getScope()
                : categoryId == null ? Budget.BudgetScope.ALL_CATEGORIES : Budget.BudgetScope.CATEGORY;
        Category category = null;
        if (scope == Budget.BudgetScope.CATEGORY) {
            if (categoryId == null) {
                throw new IllegalArgumentException("Vui lòng chọn danh mục chi tiêu cho ngân sách");
            }
            category = findAndValidateCategory(categoryId, user);
        }

        Long walletId = request.getWalletId();
        Budget.WalletScope walletScope = request.getWalletScope() != null
                ? request.getWalletScope()
                : walletId == null ? Budget.WalletScope.ALL_WALLETS : Budget.WalletScope.ONE_WALLET;
        Wallet wallet = null;
        if (walletScope == Budget.WalletScope.ONE_WALLET) {
            if (walletId == null) {
                throw new IllegalArgumentException("Vui lòng chọn ví cho ngân sách");
            }
            wallet = findAndValidateWallet(walletId, user);
        }

        return new BudgetInput(category, wallet, scope, walletScope, periodType, repeatType,
                dateRange.startDate(), dateRange.endDate());
    }

    private Long resolveCategoryId(BudgetDto.Request request) {
        if (request.getCategoryId() != null) {
            return request.getCategoryId();
        }
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            return request.getCategoryIds().get(0);
        }
        return null;
    }

    private DateRange resolveDateRange(BudgetDto.Request request, Budget.PeriodType periodType, Budget existingBudget) {
        if (request.getStartDate() != null && request.getEndDate() != null) {
            return new DateRange(request.getStartDate(), request.getEndDate());
        }
        if (request.getMonth() != null && request.getYear() != null) {
            YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
            return new DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        }
        if (existingBudget != null && existingBudget.getStartDate() != null && existingBudget.getEndDate() != null) {
            return new DateRange(existingBudget.getStartDate(), existingBudget.getEndDate());
        }
        return currentDateRange(periodType, LocalDate.now());
    }

    private DateRange currentDateRange(Budget.PeriodType periodType, LocalDate date) {
        return switch (periodType) {
            case WEEK -> new DateRange(date.with(java.time.DayOfWeek.MONDAY), date.with(java.time.DayOfWeek.SUNDAY));
            case QUARTER -> {
                int firstMonthOfQuarter = ((date.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate start = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1);
                yield new DateRange(start, start.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()));
            }
            case YEAR -> new DateRange(LocalDate.of(date.getYear(), 1, 1), LocalDate.of(date.getYear(), 12, 31));
            case MONTH, CUSTOM -> {
                YearMonth yearMonth = YearMonth.from(date);
                yield new DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());
            }
        };
    }

    private Budget.RepeatType resolveRepeatType(BudgetDto.Request request, Budget.PeriodType periodType, Budget existingBudget) {
        if (request.getRepeatType() != null) {
            return request.getRepeatType();
        }
        if (Boolean.TRUE.equals(request.getRepeat())) {
            return switch (periodType) {
                case WEEK -> Budget.RepeatType.WEEKLY;
                case MONTH -> Budget.RepeatType.MONTHLY;
                case QUARTER -> Budget.RepeatType.QUARTERLY;
                case YEAR -> Budget.RepeatType.YEARLY;
                case CUSTOM -> Budget.RepeatType.NONE;
            };
        }
        if (existingBudget != null && existingBudget.getRepeatType() != null) {
            return existingBudget.getRepeatType();
        }
        return Budget.RepeatType.NONE;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền ngân sách phải lớn hơn 0");
        }
    }

    private void validateDateAndRepeat(LocalDate startDate, LocalDate endDate,
                                       Budget.PeriodType periodType, Budget.RepeatType repeatType) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc");
        }
        if (periodType == Budget.PeriodType.CUSTOM && repeatType != Budget.RepeatType.NONE) {
            throw new IllegalArgumentException("Ngân sách tùy chỉnh không hỗ trợ lặp lại");
        }
    }

    private Category findAndValidateCategory(Long categoryId, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));

        boolean belongsToUser = category.getUser() != null && category.getUser().getId().equals(user.getId());
        boolean isDefault = Boolean.TRUE.equals(category.getIsDefault());
        if (!belongsToUser && !isDefault) {
            throw new IllegalArgumentException("Danh mục không thuộc về người dùng hiện tại");
        }
        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Chỉ được tạo ngân sách cho danh mục chi tiêu");
        }
        return category;
    }

    private Wallet findAndValidateWallet(Long walletId, User user) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Ví không tồn tại"));
        if (wallet.getUser() == null || !wallet.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Ví không thuộc về người dùng hiện tại");
        }
        if (!Boolean.TRUE.equals(wallet.getIsActive()) || Boolean.TRUE.equals(wallet.getIsArchived())) {
            throw new IllegalArgumentException("Ví phải đang hoạt động và chưa bị lưu trữ");
        }
        return wallet;
    }

    private void ensureNoDuplicate(Long userId, Category category, Wallet wallet,
                                   LocalDate startDate, LocalDate endDate, Long excludeId) {
        Long categoryId = category != null ? category.getId() : null;
        Long walletId = wallet != null ? wallet.getId() : null;
        budgetRepository.findDuplicateBudget(userId, categoryId, walletId, startDate, endDate, excludeId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ngân sách cùng danh mục, ví và khoảng thời gian đã tồn tại. Vui lòng cập nhật ngân sách cũ.");
                });
    }

    private Budget findUserBudget(Long id, User user) {
        return budgetRepository.findActiveByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngân sách"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private record BudgetInput(Category category, Wallet wallet,
                               Budget.BudgetScope scope, Budget.WalletScope walletScope,
                               Budget.PeriodType periodType, Budget.RepeatType repeatType,
                               LocalDate startDate, LocalDate endDate) {
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
