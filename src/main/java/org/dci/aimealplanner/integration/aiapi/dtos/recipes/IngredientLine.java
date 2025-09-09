package org.dci.aimealplanner.integration.aiapi.dtos.recipes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientLine {
    private String name;
    private BigDecimal amount;
    private String unit_code;
    private String note;
}
