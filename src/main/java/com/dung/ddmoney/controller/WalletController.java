package com.dung.ddmoney.controller;

import com.dung.ddmoney.dto.WalletDto;
import com.dung.ddmoney.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public List<WalletDto.Response> getAll() {
        return walletService.getAll();
    }

    @GetMapping("/{id}")
    public WalletDto.Response getById(@PathVariable("id") Long id) {
        return walletService.getById(id);
    }

    @GetMapping("/total-balance")
    public ResponseEntity<Map<String, Object>> getTotalBalance() {
        return ResponseEntity.ok(Map.of("totalBalance", walletService.getTotalBalance()));
    }

    @PostMapping
    public ResponseEntity<WalletDto.Response> create(@Valid @RequestBody WalletDto.Request req) {
        return ResponseEntity.status(201).body(walletService.create(req));
    }

    @PutMapping("/{id}")
    public WalletDto.Response update(@PathVariable("id") Long id, @Valid @RequestBody WalletDto.Request req) {
        return walletService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        walletService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transfer(@Valid @RequestBody WalletDto.TransferRequest req) {
        walletService.transfer(req.getFromWalletId(), req.getToWalletId(), req.getAmount());
        return ResponseEntity.ok(Map.of("message", "Chuyển tiền thành công"));
    }
}
