package org.dci.aimealplanner.services.recipes;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.recipes.MealCategory;
import org.dci.aimealplanner.repositories.recipes.MealCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MealCategoryService {
    private final MealCategoryRepository mealCategoryRepository;

    public void addAll(List<MealCategory> mealCategories) {
        mealCategoryRepository.saveAll(mealCategories);
    }

    public List<MealCategory> findAll() {
        return mealCategoryRepository.findAll();
    }

    public MealCategory findByName(String catName) {
        return mealCategoryRepository.findByNameIgnoreCaseLike(catName)
                .orElseThrow(() -> new RuntimeException("Can't find meal category with name " + catName));
    }
}
