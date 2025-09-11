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
public class IngredientFromAI {
    private String name;
    private String category;
    private Nutrition nutrition;

    private List<UnitSuggestion> units;
    private List<UnitRatioSuggestion> ratios;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nutrition {
        private Double kcal;
        private Double protein;
        private Double carbs;
        private Double fat;
        private Double fiber;
        private Double sugar;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitSuggestion {
        private String code;
        private String display;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitRatioSuggestion {
        private String fromUnitCode;
        private String toUnitCode;
        private Double factor;
    }
}
