package com.dung.ddmoney.service;

import com.dung.ddmoney.dto.BudgetDto;
import com.dung.ddmoney.entity.Budget;
import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.entity.Wallet;
import com.dung.ddmoney.repository.BudgetRepository;
import com.dung.ddmoney.repository.CategoryRepository;
import com.dung.ddmoney.repository.TransactionRepository;
import com.dung.ddmoney.repository.UserRepository;
import com.dung.ddmoney.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private Authentication authentication;

    private BudgetService budgetService;
    private User user;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(
                budgetRepository,
                userRepository,
                categoryRepository,
                walletRepository,
                transactionRepository
        );
        user = new User();
        user.setId(7L);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getBudgetsWithoutMonthAndYearReturnsAllActiveBudgets() {
        when(budgetRepository.findByUserIdAndActiveTrueOrderByStartDateDescNameAsc(7L))
                .thenReturn(List.of());

        budgetService.getBudgets(null, null);

        verify(budgetRepository).findByUserIdAndActiveTrueOrderByStartDateDescNameAsc(7L);
        verify(budgetRepository, never()).findByUserIdAndMonthAndYearOrdered(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void getBudgetsRejectsIncompleteLegacyPeriodFilter() {
        assertThrows(IllegalArgumentException.class, () -> budgetService.getBudgets(6, null));
    }

    @Test
    void createBudgetPersistsMultipleCategories() {
        BudgetDto.Request request = quarterRequest(List.of(1001L, 1002L));
        Category food = expenseCategory(1001L, "Ăn uống");
        Category breakfast = expenseCategory(1002L, "Ăn sáng");
        stubCategories(food, breakfast);
        stubNoConflicts(
                List.of(1001L, 1002L),
                Budget.PeriodType.QUARTER,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30)
        );
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(99L);
            return budget;
        });
        when(transactionRepository.sumExpenseForBudget(
                7L,
                com.dung.ddmoney.entity.Transaction.TransactionType.EXPENSE,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                List.of(1001L, 1002L),
                false,
                null
        )).thenReturn(BigDecimal.ZERO);

        BudgetDto.Response response = assertDoesNotThrow(() -> budgetService.createBudget(request));

        assertEquals(List.of(1001L, 1002L),
                response.getCategories().stream().map(category -> category.getId()).sorted().toList());
    }

    @Test
    void createBudgetRejectsWhenAnySelectedCategoryAlreadyHasBudget() {
        BudgetDto.Request request = quarterRequest(List.of(1001L, 1002L));
        stubCategories(
                expenseCategory(1001L, "Ăn uống"),
                expenseCategory(1002L, "Ăn sáng")
        );
        when(budgetRepository.countAllCategoryBudgetsForPeriod(
                7L,
                Budget.PeriodType.QUARTER,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                null
        )).thenReturn(0L);
        when(budgetRepository.findConflictingCategoryIds(
                7L,
                List.of(1001L, 1002L),
                Budget.PeriodType.QUARTER,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                null
        )).thenReturn(List.of(1002L));

        assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void selectingParentCategoryAlsoChecksItsChildrenForConflicts() {
        BudgetDto.Request request = quarterRequest(List.of(1001L));
        Category food = expenseCategory(1001L, "Ăn uống");
        Category breakfast = expenseCategory(1002L, "Ăn sáng");
        breakfast.setParent(food);

        stubCategories(food);
        when(categoryRepository.findAllByUserIdOrDefault(7L))
                .thenReturn(List.of(food, breakfast));
        when(budgetRepository.countAllCategoryBudgetsForPeriod(
                7L,
                Budget.PeriodType.QUARTER,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                null
        )).thenReturn(0L);
        when(budgetRepository.findConflictingCategoryIds(
                7L,
                List.of(1001L, 1002L),
                Budget.PeriodType.QUARTER,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                null
        )).thenReturn(List.of(1002L));

        assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @ParameterizedTest
    @MethodSource("standardBudgetPeriods")
    void createBudgetRejectsCategoryOverlapForEveryStandardPeriod(
            Budget.PeriodType periodType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        BudgetDto.Request request = budgetRequest(
                periodType,
                startDate,
                endDate,
                List.of(1001L, 1002L)
        );
        stubCategories(
                expenseCategory(1001L, "Ăn uống"),
                expenseCategory(1002L, "Ăn sáng")
        );
        when(budgetRepository.countAllCategoryBudgetsForPeriod(
                7L, periodType, startDate, endDate, null
        )).thenReturn(0L);
        when(budgetRepository.findConflictingCategoryIds(
                7L, List.of(1001L, 1002L), periodType, startDate, endDate, null
        )).thenReturn(List.of(1001L));

        assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudgetRejectsOverlapEvenWhenWalletIsDifferent() {
        BudgetDto.Request request = budgetRequest(
                Budget.PeriodType.MONTH,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                List.of(1001L, 1002L)
        );
        request.setWalletId(24L);
        request.setWalletScope(Budget.WalletScope.ONE_WALLET);
        Wallet wallet = Wallet.builder()
                .id(24L)
                .name("Ví phụ")
                .type(Wallet.WalletType.CASH)
                .user(user)
                .isActive(true)
                .isArchived(false)
                .build();
        stubCategories(
                expenseCategory(1001L, "Ăn uống"),
                expenseCategory(1002L, "Ăn sáng")
        );
        when(walletRepository.findById(24L)).thenReturn(Optional.of(wallet));
        when(budgetRepository.countAllCategoryBudgetsForPeriod(
                7L,
                Budget.PeriodType.MONTH,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null
        )).thenReturn(0L);
        when(budgetRepository.findConflictingCategoryIds(
                7L,
                List.of(1001L, 1002L),
                Budget.PeriodType.MONTH,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null
        )).thenReturn(List.of(1002L));

        assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    private void stubNoConflicts(
            List<Long> categoryIds,
            Budget.PeriodType periodType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        when(budgetRepository.countAllCategoryBudgetsForPeriod(
                7L, periodType, startDate, endDate, null
        )).thenReturn(0L);
        when(budgetRepository.findConflictingCategoryIds(
                7L, categoryIds, periodType, startDate, endDate, null
        )).thenReturn(List.of());
    }

    private void stubCategories(Category... categories) {
        for (Category category : categories) {
            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        }
    }

    private Category expenseCategory(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .type(Category.CategoryType.EXPENSE)
                .isDefault(true)
                .isDeleted(false)
                .sortOrder(id.intValue())
                .build();
    }

    private BudgetDto.Request quarterRequest(List<Long> categoryIds) {
        return budgetRequest(
                Budget.PeriodType.QUARTER,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                categoryIds
        );
    }

    private BudgetDto.Request budgetRequest(
            Budget.PeriodType periodType,
            LocalDate startDate,
            LocalDate endDate,
            List<Long> categoryIds
    ) {
        BudgetDto.Request request = new BudgetDto.Request();
        request.setName("Ngân sách " + periodType);
        request.setAmount(BigDecimal.valueOf(5_000_000));
        request.setCategoryIds(categoryIds);
        request.setPeriodType(periodType);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setWalletScope(Budget.WalletScope.ALL_WALLETS);
        return request;
    }

    private static Stream<Arguments> standardBudgetPeriods() {
        return Stream.of(
                Arguments.of(
                        Budget.PeriodType.WEEK,
                        LocalDate.of(2026, 6, 8),
                        LocalDate.of(2026, 6, 14)
                ),
                Arguments.of(
                        Budget.PeriodType.MONTH,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                ),
                Arguments.of(
                        Budget.PeriodType.QUARTER,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 6, 30)
                ),
                Arguments.of(
                        Budget.PeriodType.YEAR,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31)
                )
        );
    }
}
