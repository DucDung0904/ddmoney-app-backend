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
        return walletRepository.findByUserIdAndIsActiveTrue(getCurrentUser().getId())
                .stream().map(WalletDto.Response::from).toList();
    }

    public WalletDto.Response getById(Long id) {
        return WalletDto.Response.from(findOrThrow(id));
    }

    @Transactional
    public WalletDto.Response create(WalletDto.Request req) {
        Wallet wallet = Wallet.builder()
                .name(req.getName())
                .balance(req.getBalance() != null ? req.getBalance() : BigDecimal.ZERO)
                .type(req.getType())
                .bankName(req.getBankName())
                .cardNumber(req.getCardNumber())
                .colorHex(req.getColorHex())
                .isActive(true)
                .user(getCurrentUser())
                .build();
        return WalletDto.Response.from(walletRepository.save(wallet));
    }

    @Transactional
    public WalletDto.Response update(Long id, WalletDto.Request req) {
        Wallet wallet = findOrThrow(id);
        wallet.setName(req.getName());
        wallet.setBalance(req.getBalance() != null ? req.getBalance() : wallet.getBalance());
        wallet.setType(req.getType());
        wallet.setBankName(req.getBankName());
        wallet.setCardNumber(req.getCardNumber());
        wallet.setColorHex(req.getColorHex());
        return WalletDto.Response.from(walletRepository.save(wallet));
    }

    @Transactional
    public void delete(Long id) {
        Wallet wallet = findOrThrow(id);
        wallet.setIsActive(false); // soft delete
        walletRepository.save(wallet);
    }

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId.equals(toId)) throw new IllegalArgumentException("Không thể chuyển vào cùng 1 ví");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0");

        Wallet from = findOrThrow(fromId);
        Wallet to = findOrThrow(toId);

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví id=" + id));
        if (!wallet.getUser().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Không có quyền truy cập ví này");
        }
        return wallet;
    }
}
