package com.dung.ddmoney.repository;

import com.dung.ddmoney.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w WHERE w.user.id = :userId AND w.isActive = true")
    BigDecimal sumTotalBalanceByUserId(@Param("userId") Long userId);
}
