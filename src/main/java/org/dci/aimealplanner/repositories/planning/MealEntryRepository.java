package org.dci.aimealplanner.repositories.planning;

import org.dci.aimealplanner.entities.planning.MealEntry;
import org.dci.aimealplanner.models.recipes.MealSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("""
      SELECT e FROM MealEntry e
      WHERE e.mealPlan.user.id = :userId
        AND e.entryDate >= :today
      ORDER BY e.entryDate ASC, e.mealSlot ASC
    """)
    List<MealEntry> upcomingForUser(Long userId, LocalDate today);
}
