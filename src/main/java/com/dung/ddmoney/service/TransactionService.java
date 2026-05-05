package com.dung.ddmoney.service;

import com.dung.ddmoney.dto.TransactionDto;
import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.entity.Transaction;
import com.dung.ddmoney.entity.Wallet;
import com.dung.ddmoney.repository.CategoryRepository;
import com.dung.ddmoney.repository.TransactionRepository;
import com.dung.ddmoney.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.repository.UserRepository;
import com.dung.ddmoney.util.SecurityUtils;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<TransactionDto.Response> getAll() {
        return transactionRepository.findByUserIdOrderByDateDescCreatedAtDesc(getCurrentUser().getId())
                .stream().map(TransactionDto.Response::from).toList();
    }

    public List<TransactionDto.Response> getByMonthYear(int month, int year) {
        return transactionRepository.findByUserIdAndMonthAndYear(getCurrentUser().getId(), month, year)
                .stream().map(TransactionDto.Response::from).toList();
    }

    public TransactionDto.Response getById(Long id) {
        return TransactionDto.Response.from(findOrThrow(id));
    }

    @Transactional
    public TransactionDto.Response create(TransactionDto.Request req) {
        Wallet wallet = walletRepository.findById(req.getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        String title = (req.getTitle() != null && !req.getTitle().isBlank())
                ? req.getTitle() : category.getName();

        Transaction tx = Transaction.builder()
                .title(title)
                .amount(req.getAmount())
                .type(req.getType())
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .note(req.getNote())
                .wallet(wallet)
                .category(category)
                .user(getCurrentUser())
                .build();

        // Handle transfer destination
        if (req.getType() == Transaction.TransactionType.TRANSFER && req.getTransferToWalletId() != null) {
            Wallet toWallet = walletRepository.findById(req.getTransferToWalletId())
                    .orElseThrow(() -> new RuntimeException("Ví đích không tồn tại"));
            tx.setTransferToWallet(toWallet);
            // Adjust balances
            wallet.setBalance(wallet.getBalance().subtract(req.getAmount()));
            toWallet.setBalance(toWallet.getBalance().add(req.getAmount()));
            walletRepository.save(wallet);
            walletRepository.save(toWallet);
        } else {
            // Update wallet balance
            BigDecimal delta = req.getType() == Transaction.TransactionType.INCOME
                    ? req.getAmount() : req.getAmount().negate();
            wallet.setBalance(wallet.getBalance().add(delta));
            walletRepository.save(wallet);
        }

        return TransactionDto.Response.from(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionDto.Response update(Long id, TransactionDto.Request req) {
        Transaction tx = findOrThrow(id);

        // Reverse old balance effect
        reverseBalance(tx);

        // Apply new values
        Wallet newWallet = walletRepository.findById(req.getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));
        Category newCategory = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        String title = (req.getTitle() != null && !req.getTitle().isBlank())
                ? req.getTitle() : newCategory.getName();

        tx.setTitle(title);
        tx.setAmount(req.getAmount());
        tx.setType(req.getType());
        tx.setDate(req.getDate());
        tx.setNote(req.getNote());
        tx.setWallet(newWallet);
        tx.setCategory(newCategory);

        // Apply new balance
        BigDecimal delta = req.getType() == Transaction.TransactionType.INCOME
                ? req.getAmount() : req.getAmount().negate();
        newWallet.setBalance(newWallet.getBalance().add(delta));
        walletRepository.save(newWallet);

        return TransactionDto.Response.from(transactionRepository.save(tx));
    }

    @Transactional
    public void delete(Long id) {
        Transaction tx = findOrThrow(id);
        reverseBalance(tx);
        transactionRepository.deleteById(id);
    }

    // ── Summary & Chart ───────────────────────────────────────────────

    public TransactionDto.Summary getMonthlySummary(int month, int year) {
        Object[] raw = transactionRepository.getMonthlySummaryByUserId(getCurrentUser().getId(), month, year);
        TransactionDto.Summary s = new TransactionDto.Summary();
        s.setTotalIncome((BigDecimal) raw[0]);
        s.setTotalExpense((BigDecimal) raw[1]);
        s.setBalance(s.getTotalIncome().subtract(s.getTotalExpense()));
        s.setMonth(month);
        s.setYear(year);
        return s;
    }

    public List<TransactionDto.MonthlyChart> getMonthlyChart(int months) {
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1);
        List<Object[]> raw = transactionRepository.findMonthlyChartDataByUserId(getCurrentUser().getId(), from);
        List<TransactionDto.MonthlyChart> result = new ArrayList<>();
        for (Object[] row : raw) {
            TransactionDto.MonthlyChart c = new TransactionDto.MonthlyChart();
            c.setMonth((Integer) row[0]);
            c.setYear((Integer) row[1]);
            c.setIncome((BigDecimal) row[2]);
            c.setExpense((BigDecimal) row[3]);
            // Label: "T1", "T2" ...
            c.setMonthLabel("T" + row[0]);
            result.add(c);
        }
        return result;
    }

    public List<TransactionDto.CategorySpending> getCategorySpending(int month, int year) {
        List<Object[]> raw = transactionRepository.getCategorySpendingByUserId(getCurrentUser().getId(), month, year);
        BigDecimal total = raw.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return raw.stream().map(r -> {
            Long catId = (Long) r[0];
            BigDecimal amount = (BigDecimal) r[1];
            TransactionDto.CategorySpending cs = new TransactionDto.CategorySpending();
            cs.setCategoryId(catId);
            cs.setAmount(amount);
            cs.setPercentage(total.compareTo(BigDecimal.ZERO) == 0 ? 0f
                    : amount.divide(total, 4, java.math.RoundingMode.HALF_UP).floatValue());
            categoryRepository.findById(catId).ifPresent(cat -> {
                cs.setCategoryName(cat.getName());
                cs.setCategoryIcon(cat.getIcon());
                cs.setCategoryColor(cat.getColorHex());
            });
            return cs;
        }).toList();
    }

    // ── Helper ────────────────────────────────────────────────────────
    private void reverseBalance(Transaction tx) {
        if (tx.getType() == Transaction.TransactionType.INCOME) {
            tx.getWallet().setBalance(tx.getWallet().getBalance().subtract(tx.getAmount()));
        } else if (tx.getType() == Transaction.TransactionType.EXPENSE) {
            tx.getWallet().setBalance(tx.getWallet().getBalance().add(tx.getAmount()));
        }
        walletRepository.save(tx.getWallet());
    }

    private Transaction findOrThrow(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch id=" + id));
        if (!tx.getUser().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Không có quyền truy cập giao dịch này");
        }
        return tx;
    }
}
