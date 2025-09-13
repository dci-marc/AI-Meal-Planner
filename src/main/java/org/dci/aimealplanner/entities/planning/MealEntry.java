package org.dci.aimealplanner.entities.planning;

import jakarta.persistence.*;
import lombok.*;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.models.recipes.MealSlot;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "meal_entries",
        indexes = {
                @Index(name="idx_meal_entries_plan", columnList="meal_plan_id"),
                @Index(name="idx_meal_entries_date", columnList="entry_date")
        })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MealEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="meal_plan_id", nullable=false)
    private MealPlan mealPlan;

    @Column(name="entry_date", nullable=false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name="meal_slot", nullable=false)
    private MealSlot mealSlot;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="recipe_id", nullable=false)
    private Recipe recipe;

    @Column(precision = 10, scale = 2)
    private BigDecimal servings;
}
