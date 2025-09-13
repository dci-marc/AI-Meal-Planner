package org.dci.aimealplanner.services.planning;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.planning.MealEntry;
import org.dci.aimealplanner.entities.planning.MealPlan;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.planning.AddMealEntryDTO;
import org.dci.aimealplanner.models.planning.CreateMealPlanDTO;
import org.dci.aimealplanner.repositories.planning.MealEntryRepository;
import org.dci.aimealplanner.repositories.planning.MealPlanRepository;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanningService {
    private final MealPlanRepository mealPlanRepository;
    private final MealEntryRepository mealEntryRepository;
    private final RecipeService recipeService;
    private final UserService userService;

    @Transactional
    public MealPlan createPlan(Long userId, CreateMealPlanDTO dto) {
        if (dto.startDate() == null || dto.endDate() == null || dto.startDate().isAfter(dto.endDate())) {
            throw new IllegalArgumentException("Invalid date range");
        }
        MealPlan plan = new MealPlan();
        User user =userService.findById(userId);
        plan.setUser(user);
        plan.setName(dto.name());
        plan.setStartDate(dto.startDate());
        plan.setEndDate(dto.endDate());
        plan.setKcalTargetPerDay(dto.kcalTargetPerDay());
        return mealPlanRepository.save(plan);
    }

    @Transactional
    public void deletePlan(Long planId, Long userId) {
        mealPlanRepository.deleteById(planId);
    }

    @Transactional
    public MealEntry addEntry(Long userId, AddMealEntryDTO mealEntryDTO) {
        MealPlan plan = mealPlanRepository.findById(mealEntryDTO.mealPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Meal plan not found"));

        if (!plan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Invalid user id");
        }

        LocalDate date = mealEntryDTO.entryDate();
        if (date == null || date.isBefore(plan.getStartDate()) || date.isAfter(plan.getEndDate())) {
            throw new IllegalArgumentException("Entry date outside the plan range");
        }

        Recipe recipe = recipeService.findById(mealEntryDTO.recipeId());

        MealEntry mealEntry = new MealEntry();
        mealEntry.setMealPlan(plan);
        mealEntry.setEntryDate(date);
        mealEntry.setMealSlot(mealEntryDTO.mealSlot());
        mealEntry.setRecipe(recipe);
        mealEntry.setServings(mealEntryDTO.servings() != null ? mealEntryDTO.servings() : BigDecimal.ONE);

        return mealEntryRepository.save(mealEntry);
    }

    @Transactional
    public void removeEntry(Long entryId, Long userId) {
        mealEntryRepository.deleteById(entryId);
    }

    @Transactional
    public List<MealEntry> listEntries(Long planId) {
        return mealEntryRepository.findByMealPlanIdOrderByEntryDateAsc(planId);
    }

    @Transactional
    public Map<LocalDate, List<MealEntry>> entriesGroupedByDate(Long planId) {
        return listEntries(planId).stream()
                .collect(Collectors.groupingBy(MealEntry::getEntryDate,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<MealPlan> listPlansForUser(Long userId) {
        return mealPlanRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public MealPlan getPlanForUser(Long planId, Long userId) {
        MealPlan plan = mealPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Meal plan not found"));
        if (!plan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You do not have access to this plan");
        }
        return plan;
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, List<MealEntry>> getEntriesGroupedByDateForUser(Long planId, Long userId) {
        getPlanForUser(planId, userId);
        return entriesGroupedByDate(planId);
    }
}
