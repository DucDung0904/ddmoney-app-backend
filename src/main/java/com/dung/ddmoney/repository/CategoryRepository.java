package com.dung.ddmoney.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dung.ddmoney.entity.Category;

import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.user.id = :userId OR c.isDefault = true")
    List<Category> findAllByUserIdOrDefault(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE (c.user.id = :userId OR c.isDefault = true) AND c.type IN :types")
    List<Category> findByUserIdAndTypeIn(@Param("userId") Long userId, @Param("types") List<Category.CategoryType> types);

    boolean existsByNameIgnoreCaseAndUserId(String name, Long userId);
}
