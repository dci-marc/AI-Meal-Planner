package org.dci.aimealplanner.controllers.api;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.IngredientUnitRatio;
import org.dci.aimealplanner.models.recipes.UnitOption;
import org.dci.aimealplanner.repositories.ingredients.IngredientSummary;
import org.dci.aimealplanner.services.ingredients.IngredientService;
import org.dci.aimealplanner.services.ingredients.IngredientUnitRatioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientApiController {

    private final IngredientService ingredientService;
    private final IngredientUnitRatioService ingredientUnitRatioService;

    @GetMapping
    public Page<IngredientSummary> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return ingredientService.search(
                q, PageRequest.of(page, size, Sort.by("name").ascending()));

    }

    @GetMapping("/by-ids")
    public List<IngredientSummary> byIds(@RequestParam("ids") List<Long> ids) {
        return (ids == null || ids.isEmpty()) ? List.of() : ingredientService.findByIdIn(ids);
    }

    @GetMapping("/{id}/units")
    public List<UnitOption> unitsForIngredient(@PathVariable Long id) {
        List<IngredientUnitRatio> ratios = ingredientUnitRatioService.findByIngredientId(id);
        return ratios.stream()
                .map(r -> new UnitOption(
                        r.getUnit().getId(),
                        (r.getUnit().getDisplayName() != null && !r.getUnit().getDisplayName().isBlank())
                                ? r.getUnit().getDisplayName()
                                : r.getUnit().getCode()))
                .toList();
    }
}