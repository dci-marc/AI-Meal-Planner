package org.dci.aimealplanner.repositories.planning;

import org.dci.aimealplanner.entities.planing.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
}
