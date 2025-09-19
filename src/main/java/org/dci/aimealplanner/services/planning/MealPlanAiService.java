package org.dci.aimealplanner.services.planning;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.planning.MealPlan;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.integration.aiapi.GroqApiClient;
import org.dci.aimealplanner.integration.aiapi.dtos.planning.MealPlanDayFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.planning.MealPlanFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.planning.MealPlanGenerationRequest;
import org.dci.aimealplanner.integration.aiapi.dtos.planning.PlannedMealFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.recipes.RecipeFromAI;
import org.dci.aimealplanner.models.planning.AddMealEntryDTO;
import org.dci.aimealplanner.models.recipes.MealSlot;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealPlanAiService {
    private final GroqApiClient groqApiClient;
    private final UserInformationService userInformationService;
    private final MealPlanningService mealPlanningService;
    private final RecipeService recipeService;

    @Transactional
    public void populatePlanWithAi(Long planId, User currectUser) {
        MealPlan plan = mealPlanningService.getPlanForUser(planId, currectUser.getId());

        UserInformation information = userInformationService.getUserInformationByUser(currectUser);

        MealPlanGenerationRequest req = new MealPlanGenerationRequest(
                plan.getStartDate(),
                plan.getEndDate(),
                information.getAge(),
                information.getGender() != null ? information.getGender().name() : null,
                information.getHeightCm(),
                information.getWeightKg() != null ? information.getWeightKg().doubleValue() : null,
                information.getActivityLevel() != null ? information.getActivityLevel().name() : null,
                information.getGoal() != null ? information.getGoal().name() : null,
                information.getMealsPerDay(),
                information.getDietaryPreferences().stream().map(dp -> dp.getName()).toList(),
                List.of(),
                List.of(),
                List.of(),
                information.getTargetKcalPerDay()
        );
        MealPlanFromAI aiPlan = groqApiClient.generateMealPlanFromProfile(req);

        Integer targetKcalPerDay = aiPlan.targetKcalPerDay() != null
                ? aiPlan.targetKcalPerDay()
                : information.getTargetKcalPerDay();

        aiPlan.days().stream()
                .sorted(Comparator.comparing(MealPlanDayFromAI::date))
                .forEach(day -> {
                    LocalDate date = LocalDate.parse(day.date());
                    if (date.isBefore(plan.getStartDate()) || date.isAfter(plan.getEndDate())) return;

                    for (PlannedMealFromAI m : day.meals()) {
                        MealSlot slot = toSlot(m.slot());
                        if (slot == null) continue;

                        String mealRecipePrompt = buildMealRecipeUserPrompt(
                                m.title(),
                                slot,
                                m.servings(),
                                targetKcalPerDay,
                                information.getMealsPerDay(),
                                information
                        );

                        RecipeFromAI aiRecipe = null;
                        try {
                            aiRecipe = groqApiClient.generateRecipeFromPrompt(mealRecipePrompt);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        var saved = recipeService.saveFromAI(aiRecipe, currectUser.getEmail());

                        mealPlanningService.addEntry(
                                currectUser.getId(),
                                new AddMealEntryDTO(
                                        plan.getId(),
                                        date,
                                        slot,
                                        saved.getId(),
                                        m.servings() != null ? BigDecimal.valueOf(m.servings()) : BigDecimal.ONE
                                )
                        );
                    }
                });
    }

    private MealSlot toSlot(String s) {
        if (s == null) return null;
        try { return MealSlot.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private String buildMealRecipeUserPrompt(String title,
                                             MealSlot slot,
                                             Double servings,
                                             Integer targetKcalPerDay,
                                             Integer mealsPerDay,
                                             UserInformation ui) {

        Integer kcalPerMeal = null;
        if (targetKcalPerDay != null && mealsPerDay != null && mealsPerDay > 0) {
            kcalPerMeal = Math.max(250, targetKcalPerDay / mealsPerDay);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Create a ").append(slot.name().toLowerCase()).append(" recipe titled \"").append(title).append("\".");
        sb.append(" Servings: ").append(servings != null ? servings : 1).append(".");
        if (kcalPerMeal != null) {
            sb.append(" Aim around ").append(kcalPerMeal).append(" kcal total.");
        }

        var prefs = ui.getDietaryPreferences().stream().map(dp -> dp.getName()).toList();
        if (!prefs.isEmpty()) {
            sb.append(" Respect these preferences: ").append(String.join(", ", prefs)).append(".");
        }

        if (ui.getGoal() != null) sb.append(" Goal: ").append(ui.getGoal().name()).append(".");
        if (ui.getActivityLevel() != null) sb.append(" Activity: ").append(ui.getActivityLevel().name()).append(".");


        return sb.toString();
    }
}
