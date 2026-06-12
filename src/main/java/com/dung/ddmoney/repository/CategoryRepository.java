package com.dung.ddmoney.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dung.ddmoney.entity.Category;

import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            SELECT c FROM Category c
            LEFT JOIN c.user owner
            WHERE (owner.id = :userId OR c.isDefault = true)
              AND c.isDeleted = false
            ORDER BY c.sortOrder, c.name
            """)
    List<Category> findAllByUserIdOrDefault(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Category c
            LEFT JOIN c.user owner
            WHERE (owner.id = :userId OR c.isDefault = true)
              AND c.isDeleted = false
              AND c.type IN :types
            ORDER BY c.sortOrder, c.name
            """)
    List<Category> findByUserIdAndTypeIn(@Param("userId") Long userId, @Param("types") List<Category.CategoryType> types);

    @Query("""
            SELECT COUNT(c) FROM Category c
            LEFT JOIN c.user owner
            LEFT JOIN c.parent parent
            WHERE (owner.id = :userId OR c.isDefault = true)
              AND c.isDeleted = false
              AND LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
              AND c.type = :type
              AND ((:parentId IS NULL AND parent IS NULL) OR parent.id = :parentId)
              AND (:excludeId IS NULL OR c.id <> :excludeId)
            """)
    long countVisibleDuplicates(@Param("userId") Long userId,
                                @Param("name") String name,
                                @Param("type") Category.CategoryType type,
                                @Param("parentId") Long parentId,
                                @Param("excludeId") Long excludeId);
}
