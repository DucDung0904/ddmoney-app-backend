package com.dung.ddmoney.service;

import com.dung.ddmoney.dto.WalletDto;
import com.dung.ddmoney.entity.Wallet;
import com.dung.ddmoney.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.repository.UserRepository;
import com.dung.ddmoney.util.SecurityUtils;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<WalletDto.Response> getAll() {
        return walletRepository.findByUserIdOrderByIsArchivedAscSortOrderAscNameAsc(getCurrentUser().getId())
                .stream().map(WalletDto.Response::from).toList();
    }

    public WalletDto.Response getById(Long id) {
        return WalletDto.Response.from(findOrThrow(id));
    }

    @Transactional
    public WalletDto.Response create(WalletDto.Request req) {
        User user = getCurrentUser();
        boolean firstWallet = walletRepository.countByUserIdAndIsActiveTrueAndIsArchivedFalse(user.getId()) == 0;
        boolean shouldBeDefault = Boolean.TRUE.equals(req.getIsDefault()) || firstWallet;
        if (shouldBeDefault) {
            walletRepository.clearDefaultForUser(user.getId());
        }

        Wallet.WalletType normalizedType = normalizeType(req.getType());
        Wallet wallet = Wallet.builder()
                .name(req.getName().trim())
                .balance(defaultMoney(req.getBalance()))
                .type(normalizedType)
                .bankName(blankToNull(req.getBankName()))
                .cardNumber(blankToNull(req.getCardNumber()))
                .colorHex(defaultText(req.getColorHex(), "#4659A6"))
                .icon(defaultText(req.getIcon(), defaultIconFor(normalizedType)))
                .currency(defaultText(req.getCurrency(), "VND"))
                .isDefault(shouldBeDefault)
                .isActive(true)
                .isArchived(false)
                .isIncludedInTotal(!Boolean.FALSE.equals(req.getIsIncludedInTotal()))
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .creditLimit(req.getCreditLimit())
                .currentDebt(req.getCurrentDebt())
                .billingDay(validDay(req.getBillingDay()))
                .paymentDueDay(validDay(req.getPaymentDueDay()))
                .user(user)
                .build();
        return WalletDto.Response.from(walletRepository.save(wallet));
    }

    @Transactional
    public WalletDto.Response update(Long id, WalletDto.Request req) {
        Wallet wallet = findOrThrow(id);
        if (!Boolean.TRUE.equals(wallet.getIsActive()) || Boolean.TRUE.equals(wallet.getIsArchived())) {
            throw new IllegalArgumentException("Vi da bi luu tru hoac khong con hoat dong");
        }

        wallet.setName(req.getName().trim());
        wallet.setBalance(req.getBalance() != null ? req.getBalance() : wallet.getBalance());
        wallet.setIcon(defaultText(req.getIcon(), defaultText(wallet.getIcon(), defaultIconFor(wallet.getType()))));
        wallet.setColorHex(defaultText(req.getColorHex(), wallet.getColorHex()));
        wallet.setIsIncludedInTotal(!Boolean.FALSE.equals(req.getIsIncludedInTotal()));

        if (Boolean.TRUE.equals(req.getIsDefault()) && !Boolean.TRUE.equals(wallet.getIsDefault())) {
            walletRepository.clearDefaultForUser(wallet.getUser().getId());
            wallet.setIsDefault(true);
        }

        Wallet saved = walletRepository.save(wallet);
        ensureDefaultWallet(wallet.getUser().getId());
        return WalletDto.Response.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Wallet wallet = findOrThrow(id);
        Long userId = wallet.getUser().getId();
        wallet.setIsActive(false);
        wallet.setIsArchived(true);
        wallet.setIsDefault(false);
        walletRepository.save(wallet);
        ensureDefaultWallet(userId);
    }

    @Transactional
    public WalletDto.Response restore(Long id) {
        Wallet wallet = findOrThrow(id);
        wallet.setIsActive(true);
        wallet.setIsArchived(false);
        Wallet saved = walletRepository.save(wallet);
        ensureDefaultWallet(wallet.getUser().getId());
        return WalletDto.Response.from(saved);
    }

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        fromId = idNotNull(fromId);
        toId = idNotNull(toId);
        if (fromId.equals(toId)) throw new IllegalArgumentException("Khong the chuyen vao cung 1 vi");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("So tien phai lon hon 0");
        }

        Wallet from = findOrThrow(fromId);
        Wallet to = findOrThrow(toId);
        if (!Boolean.TRUE.equals(from.getIsActive()) || Boolean.TRUE.equals(from.getIsArchived())
                || !Boolean.TRUE.equals(to.getIsActive()) || Boolean.TRUE.equals(to.getIsArchived())) {
            throw new IllegalArgumentException("Vi da bi luu tru hoac khong con hoat dong");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        walletRepository.save(from);
        walletRepository.save(to);
    }

    public BigDecimal getTotalBalance() {
        return walletRepository.sumTotalBalanceByUserId(getCurrentUser().getId());
    }

    private Wallet findOrThrow(Long id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay vi id=" + id));
        if (!wallet.getUser().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Khong co quyen truy cap vi nay");
        }
        return wallet;
    }

    private void ensureDefaultWallet(Long userId) {
        List<Wallet> wallets = walletRepository.findByUserIdAndIsActiveTrueAndIsArchivedFalseOrderBySortOrderAscNameAsc(userId);
        if (wallets.isEmpty()) return;
        boolean hasDefault = wallets.stream().anyMatch(w -> Boolean.TRUE.equals(w.getIsDefault()));
        if (!hasDefault) {
            Wallet first = wallets.get(0);
            first.setIsDefault(true);
            walletRepository.save(first);
        }
    }

    private Wallet.WalletType normalizeType(Wallet.WalletType type) {
        if (type == null) return Wallet.WalletType.CASH;
        return type == Wallet.WalletType.CREDIT ? Wallet.WalletType.CREDIT_CARD : type;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer validDay(Integer day) {
        return day != null && day >= 1 && day <= 31 ? day : null;
    }

    private Long idNotNull(Long id) {
        if (id == null) throw new IllegalArgumentException("Wallet id khong duoc trong");
        return id;
    }

    private String defaultIconFor(Wallet.WalletType type) {
        return switch (type) {
            case CASH -> "cash";
            case BANK -> "bank";
            case EWALLET -> "ewallet";
            case CREDIT_CARD, CREDIT -> "credit_card";
            case SAVINGS -> "savings";
            case INVESTMENT -> "investment";
        };
    }
}

