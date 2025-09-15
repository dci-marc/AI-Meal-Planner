package org.dci.aimealplanner.repositories.planning;

import org.dci.aimealplanner.entities.planning.MealEntry;
import org.dci.aimealplanner.models.recipes.MealSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {
    List<MealEntry> findByMealPlanIdOrderByEntryDateAsc(Long mealPlanId);
    List<MealEntry> findByMealPlanIdAndEntryDate(Long mealPlanId, LocalDate date);
    Optional<MealEntry> findByMealPlanIdAndEntryDateAndMealSlot(Long mealPlanId,
                                                                LocalDate entryDate,
                                                                MealSlot mealSlot);
}
