package org.dci.aimealplanner.services.planning;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.planning.MealEntry;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.planning.NextMealDTO;
import org.dci.aimealplanner.models.recipes.RecipeCardDTO;
import org.dci.aimealplanner.repositories.planning.MealEntryRepository;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningSnapshotService {
    private final MealEntryRepository  mealEntryRepository;
    private final UserService userService;

    public List<NextMealDTO> nextMeals(String email, int limit) {
        User u = userService.findByEmail(email);
        var entries = mealEntryRepository.upcomingForUser(u.getId(), LocalDate.now());
        return entries.stream()
                .limit(Math.max(1, Math.min(limit, 4)))
                .map(this::toDto)
                .toList();
    }

    private NextMealDTO toDto(MealEntry e) {
        var r = e.getRecipe();
        String hero = (r.getImage() != null) ? r.getImage().getImageUrl() : null;

        BigDecimal perServing = null;
        if (r.getKcalPerServ() != null) {
            BigDecimal servings = (r.getServings() == null || r.getServings().compareTo(BigDecimal.ZERO) <= 0)
                    ? BigDecimal.ONE : r.getServings();
            perServing = r.getKcalPerServ().divide(servings, 0, RoundingMode.HALF_UP);
        }

        RecipeCardDTO card = new RecipeCardDTO(
                r.getId(),
                r.getTitle(),
                hero,
                r.getPreparationTimeMinutes(),
                perServing
        );

        return new NextMealDTO(e.getEntryDate(), e.getMealSlot().name(), card,
                e.getServings() == null ? BigDecimal.ONE : e.getServings());
    }
}
