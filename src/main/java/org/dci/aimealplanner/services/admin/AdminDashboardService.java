package org.dci.aimealplanner.services.admin;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.planning.MealEntry;
import org.dci.aimealplanner.entities.planning.MealPlan;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.Role;
import org.dci.aimealplanner.models.SourceType;
import org.dci.aimealplanner.models.UserType;
import org.dci.aimealplanner.services.planning.MealPlanningService;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.AuthProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserService userService;
    private final RecipeService recipeService;
    private final MealPlanningService mealPlanningService;

    public long totalUsers() {
        return userService.countByDeletedFalse();
    }

    public long activeUsers() {
        return userService.countByEmailVerifiedTrueAndDeletedFalse();
    }

    public long totalRecipes() {
        return recipeService.countAll();
    }

    public long totalMealPlans() {
        return mealPlanningService.count();
    }

    public long countUserRecipes() {
        return recipeService.countBySource(SourceType.USER);
    }

    public long countAiRecipes() {
        return recipeService.countBySource(SourceType.AI);
    }

    public Page<Recipe> findRecipes(SourceType sourceType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        if (sourceType == null) {
            return recipeService.findAll(pageable);
        }
        return recipeService.findBySource(sourceType, pageable);
    }

    public long countAllUsers() {
        return userService.countByIsDeletedIsFalse();
    }

    public long countActiveUsers() {
        return userService.countByEmailVerifiedTrueAndIsDeletedFalse();
    }

    public long countUnverifiedUsers() {
        return userService.countByEmailVerifiedFalseAndIsDeletedFalse();
    }

    public long countDeletedUsers() {
        return userService.countByIsDeletedTrue();
    }

    public Page<User> findUsers(String filter, String provider, String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        String like = toLike(q);

        UserType type = null;
        if (provider != null && !"ANY".equalsIgnoreCase(provider)) {
            type = UserType.valueOf(provider.toUpperCase());
        }

        switch (filter == null ? "ALL" : filter.toUpperCase()) {
            case "DELETED":
                return (type == null)
                        ? userService.searchDeleted(like, pageable)
                        : userService.searchDeletedByType(like, type, pageable);

            case "ACTIVE":
                return (type == null)
                        ? userService.searchActiveVerified(like, pageable)
                        : userService.searchActiveVerifiedByType(like, type, pageable);

            case "UNVERIFIED":
                return (type == null)
                        ? userService.searchActiveUnverified(like, pageable)
                        : userService.searchActiveUnverifiedByType(like, type, pageable);

            case "ALL":
            default:
                return (type == null)
                        ? userService.searchActive(like, pageable)
                        : userService.searchActiveByType(like, type, pageable);
        }
    }

    private String toLike(String q) {
        return (q == null || q.isBlank()) ? "%" : "%" + q.trim().toLowerCase() + "%";
    }

    @Transactional
    public void softDeleteUser(Long id) {
        User user = userService.findById(id);
        if (!user.isDeleted()) {
            user.setDeleted(true);
            user.setDeletedAt(LocalDateTime.now());
            userService.update(user);
        }
    }

    @Transactional
    public void restoreUser(Long id) {
        User user = userService.findById(id);
        if (user.isDeleted()) {
            user.setDeleted(false);
            user.setDeletedAt(null);
            userService.update(user);
        }
    }

    @Transactional
    public void verifyUser(Long id) {
        User user = userService.findById(id);
        user.setEmailVerified(true);
        userService.update(user);
    }

    @Transactional
    public void setRole(Long id, Role role) {
        User user = userService.findById(id);
        user.setRole(role);
        userService.update(user);
    }

    public Page<org.dci.aimealplanner.entities.planning.MealPlan> findMealPlans(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return mealPlanningService.findAll(pageable);
    }


    public Page<org.dci.aimealplanner.entities.planning.MealPlan> findMealPlansSimple(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        if (q == null || q.isBlank()) {
            return mealPlanningService.findAll(pageable);
        }
        return mealPlanningService.searchByEmailOrPlanName(q.trim(), pageable);
    }

    @Transactional
    public void deleteMealPlanHard(Long id) {
        mealPlanningService.deleteById(id);
    }

    public MealPlan findMealPlanById(Long id) {
        return mealPlanningService.findById(id);
    }

    @Transactional(readOnly = true)
    public Map<Object, List<MealEntry>> getEntriesGroupedByDate(MealPlan plan) {
        List<MealEntry> entries = mealPlanningService.findEntriesByMealPlan(plan.getId());

        return entries.stream().collect(Collectors.groupingBy(MealEntry::getEntryDate));
    }
}