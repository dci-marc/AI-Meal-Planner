package org.dci.aimealplanner.integration.aiapi.dtos.ingredients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientUnitFromAI {
    private String ingredient;
    private List<UnitRatios> units;
}
