package org.dci.aimealplanner.repositories.planning;

import org.dci.aimealplanner.entities.planning.MealPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<MealPlan> findByUser_EmailIgnoreCaseContainingOrNameIgnoreCaseContaining(
            String emailLike, String planNameLike, Pageable pageable
    );

    @Query("""
      SELECT mp FROM MealPlan mp
      WHERE (:q = '' OR
             LOWER(mp.name)       LIKE LOWER(CONCAT('%', :q, '%')) OR
             LOWER(mp.user.email) LIKE LOWER(CONCAT('%', :q, '%')))
      """)
    Page<MealPlan> searchByEmailOrPlanName(@Param("q") String q, Pageable pageable);
}
