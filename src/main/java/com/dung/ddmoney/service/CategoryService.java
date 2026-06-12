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
        User user = getCurrentUser();
        return categoryRepository.findAllByUserIdOrDefault(user.getId())
                .stream().map(CategoryDto.Response::from).toList();
    }

    public List<CategoryDto.Response> getByTypes(List<Category.CategoryType> types) {
        User user = getCurrentUser();
        return categoryRepository.findByUserIdAndTypeIn(user.getId(), types)
                .stream().map(CategoryDto.Response::from).toList();
    }

    public CategoryDto.Response getById(Long id) {
        return CategoryDto.Response.from(findOrThrow(id));
    }

    @Transactional
    public CategoryDto.Response create(CategoryDto.Request req) {
        User user = getCurrentUser();
        Category parent = resolveParent(req.getParentId(), req.getType(), null, user);
        ensureUnique(req, parent, null, user);
        Category cat = Category.builder()
                .name(req.getName().trim())
                .icon(req.getIcon())
                .colorHex(req.getColorHex())
                .parent(parent)
                .type(req.getType())
                .isDefault(false)
                .isEditable(true)
                .isDeletable(true)
                .isDeleted(false)
                .sortOrder(req.getSortOrder())
                .user(user)
                .build();
        return CategoryDto.Response.from(categoryRepository.save(cat));
    }

    @Transactional
    public CategoryDto.Response update(Long id, CategoryDto.Request req) {
        User user = getCurrentUser();
        Category cat = findAccessible(id, user);
        if (Boolean.TRUE.equals(cat.getIsDefault()) || !Boolean.TRUE.equals(cat.getIsEditable())) {
            throw new IllegalStateException("Không thể sửa danh mục mặc định");
        }
        Category parent = resolveParent(req.getParentId(), req.getType(), id, user);
        ensureUnique(req, parent, id, user);
        cat.setName(req.getName().trim());
        cat.setIcon(req.getIcon());
        cat.setColorHex(req.getColorHex());
        cat.setType(req.getType());
        cat.setParent(parent);
        cat.setSortOrder(req.getSortOrder());
        return CategoryDto.Response.from(categoryRepository.save(cat));
    }

    @Transactional
    public void delete(Long id) {
        Category cat = findOrThrow(id);
        if (Boolean.TRUE.equals(cat.getIsDefault()) || !Boolean.TRUE.equals(cat.getIsDeletable())) {
            throw new IllegalStateException("Không thể xóa danh mục mặc định");
        }
        categoryRepository.deleteById(id);
    }

    public Category findOrThrow(Long id) {
        return findAccessible(id, getCurrentUser());
    }

    private Category findAccessible(Long id, User user) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục id=" + id));
        if (Boolean.TRUE.equals(cat.getIsDeleted())) {
            throw new RuntimeException("Danh mục đã bị xóa");
        }
        if (!Boolean.TRUE.equals(cat.getIsDefault())
                && (cat.getUser() == null || !cat.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("Không có quyền truy cập danh mục này");
        }
        return cat;
    }

    private Category resolveParent(Long parentId, Category.CategoryType childType,
                                   Long currentCategoryId, User user) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(currentCategoryId)) {
            throw new IllegalArgumentException("Danh mục không thể là cha của chính nó");
        }
        Category parent = findAccessible(parentId, user);
        if (parent.getParent() != null) {
            throw new IllegalArgumentException("Chỉ hỗ trợ tối đa hai cấp danh mục");
        }
        if (parent.getType() != childType) {
            throw new IllegalArgumentException("Danh mục cha và danh mục con phải cùng loại");
        }
        return parent;
    }

    private void ensureUnique(CategoryDto.Request req, Category parent, Long excludeId, User user) {
        Long parentId = parent != null ? parent.getId() : null;
        if (categoryRepository.countVisibleDuplicates(
                user.getId(), req.getName(), req.getType(), parentId, excludeId) > 0) {
            throw new IllegalArgumentException("Danh mục cùng tên và cùng nhóm đã tồn tại");
        }
    }
}
