package com.dung.ddmoney.repository;

import com.dung.ddmoney.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByUserIdOrderByIsArchivedAscSortOrderAscNameAsc(Long userId);

    List<Wallet> findByUserIdAndIsActiveTrueAndIsArchivedFalseOrderBySortOrderAscNameAsc(Long userId);

    long countByUserIdAndIsActiveTrueAndIsArchivedFalse(Long userId);

    @Modifying
    @Query("UPDATE Wallet w SET w.isDefault = false WHERE w.user.id = :userId AND w.isDefault = true")
    void clearDefaultForUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w WHERE w.user.id = :userId AND w.isActive = true AND w.isArchived = false AND w.isIncludedInTotal = true")
    BigDecimal sumTotalBalanceByUserId(@Param("userId") Long userId);
}
