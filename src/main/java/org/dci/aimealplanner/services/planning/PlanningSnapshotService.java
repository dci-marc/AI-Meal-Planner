package org.dci.aimealplanner.services.planning;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.planning.MealEntry;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.planning.NextMealDTO;
import org.dci.aimealplanner.models.recipes.MealSlot;
import org.dci.aimealplanner.models.recipes.RecipeCardDTO;
import org.dci.aimealplanner.repositories.planning.MealEntryRepository;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningSnapshotService {
    private final MealEntryRepository  mealEntryRepository;
    private final UserService userService;

//    public List<NextMealDTO> nextMeals(String email, int limit) {
//        User u = userService.findByEmail(email);
//        var entries = mealEntryRepository.upcomingForUser(u.getId(), LocalDate.now());
//        return entries.stream()
//                .limit(Math.max(1, Math.min(limit, 4)))
//                .map(this::toDto)
//                .toList();
//    }
//
//    private NextMealDTO toDto(MealEntry e) {
//        var r = e.getRecipe();
//        String hero = (r.getImage() != null) ? r.getImage().getImageUrl() : null;
//
//        BigDecimal perServing = null;
//        if (r.getKcalPerServ() != null) {
//            BigDecimal servings = (r.getServings() == null || r.getServings().compareTo(BigDecimal.ZERO) <= 0)
//                    ? BigDecimal.ONE : r.getServings();
//            perServing = r.getKcalPerServ().divide(servings, 0, RoundingMode.HALF_UP);
//        }
//
//        RecipeCardDTO card = new RecipeCardDTO(
//                r.getId(),
//                r.getTitle(),
//                hero,
//                r.getPreparationTimeMinutes(),
//                perServing
//        );
//
//        return new NextMealDTO(e.getEntryDate(), e.getMealSlot().name(), card,
//                e.getServings() == null ? BigDecimal.ONE : e.getServings());
//    }

    public List<NextMealDTO> nextMeals(String email, int limit) {
        User u = userService.findByEmail(email);

        ZoneId zone = ZoneId.of("Europe/Berlin");
        LocalDate today = LocalDate.now(zone);
        LocalTime now = LocalTime.now(zone);
        int minSlotToday = minSlotOrderForNow(now);

        var entries = mealEntryRepository.upcomingForUser(u.getId(), today);

        return entries.stream()
                // hide past slots for today
                .filter(e ->
                        e.getEntryDate().isAfter(today) ||
                                (e.getEntryDate().isEqual(today) &&
                                        slotOrder(e.getMealSlot()) >= minSlotToday)
                )
                .sorted(
                        Comparator
                                .comparing(MealEntry::getEntryDate)
                                .thenComparing(e -> slotOrder(e.getMealSlot()))
                )
                .limit(Math.max(1, Math.min(limit, 4)))
                .map(this::toDto)
                .toList();
    }

    private int minSlotOrderForNow(LocalTime now) {

        if (now.isBefore(LocalTime.of(10, 30))) return slotOrder(MealSlot.BREAKFAST);
        if (now.isBefore(LocalTime.of(15, 0)))  return slotOrder(MealSlot.LUNCH);
        if (now.isBefore(LocalTime.of(23, 0)))  return slotOrder(MealSlot.DINNER);
        return Integer.MAX_VALUE;
    }

    private int slotOrder(MealSlot slot) {
        if (slot == null) return 999;
        return switch (slot) {
            case BREAKFAST -> 1;
            case LUNCH     -> 2;
            case DINNER    -> 3;
            default        -> 99;
        };
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

        return new NextMealDTO(
                e.getEntryDate(),
                e.getMealSlot().name(),
                card,
                e.getServings() == null ? BigDecimal.ONE : e.getServings()
        );
    }
}
