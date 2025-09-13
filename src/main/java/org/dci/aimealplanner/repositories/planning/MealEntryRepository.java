package org.dci.aimealplanner.repositories.planning;

import org.dci.aimealplanner.entities.planning.MealEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {
    List<MealEntry> findByMealPlanIdOrderByEntryDateAsc(Long mealPlanId);
    List<MealEntry> findByMealPlanIdAndEntryDate(Long mealPlanId, LocalDate date);
}
