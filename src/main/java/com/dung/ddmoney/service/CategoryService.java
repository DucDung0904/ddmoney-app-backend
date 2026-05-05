package com.dung.ddmoney.service;

import com.dung.ddmoney.dto.CategoryDto;
import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.repository.UserRepository;
import com.dung.ddmoney.util.SecurityUtils;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<CategoryDto.Response> getAll() {
        return categoryRepository.findAllByUserIdOrDefault(getCurrentUser().getId())
                .stream().map(CategoryDto.Response::from).toList();
    }

    public List<CategoryDto.Response> getByTypes(List<Category.CategoryType> types) {
        return categoryRepository.findByUserIdAndTypeIn(getCurrentUser().getId(), types)
                .stream().map(CategoryDto.Response::from).toList();
    }

    public CategoryDto.Response getById(Long id) {
        return CategoryDto.Response.from(findOrThrow(id));
    }

    @Transactional
    public CategoryDto.Response create(CategoryDto.Request req) {
        Category cat = Category.builder()
                .name(req.getName())
                .icon(req.getIcon())
                .colorHex(req.getColorHex())
                .type(req.getType())
                .isDefault(false)
                .user(getCurrentUser())
                .build();
        return CategoryDto.Response.from(categoryRepository.save(cat));
    }

    @Transactional
    public CategoryDto.Response update(Long id, CategoryDto.Request req) {
        Category cat = findOrThrow(id);
        cat.setName(req.getName());
        cat.setIcon(req.getIcon());
        cat.setColorHex(req.getColorHex());
        cat.setType(req.getType());
        return CategoryDto.Response.from(categoryRepository.save(cat));
    }

    @Transactional
    public void delete(Long id) {
        Category cat = findOrThrow(id);
        if (cat.getIsDefault()) {
            throw new IllegalStateException("Không thể xóa danh mục mặc định");
        }
        categoryRepository.deleteById(id);
    }

    public Category findOrThrow(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục id=" + id));
        if (!cat.getIsDefault() && !cat.getUser().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Không có quyền truy cập danh mục này");
        }
        return cat;
    }
}
