package org.dci.aimealplanner.repositories.ingredients;

import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByName(String name);
    Optional<Ingredient> findByNameIgnoreCaseLike(String name);
    List<IngredientSummary> findByIdIn(Collection<Long> ids);
    List<Ingredient> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String q);

    @Query("""
        select i.id as id, i.name as name
        from ingredients i
        where lower(i.name) like lower(concat('%', :q, '%'))
        order by 
          case 
            when lower(i.name) like lower(concat(:q, '%')) then 0 
            else 1 
          end,
          i.name asc
    """)
    Page<IngredientSummary> smartSearch(@Param("q") String q, Pageable pageable);

    // smart fuzzy with ordering (exact → startsWith → endsWith → contains; then shortest name)
    @Query("""
        select i
        from ingredients i
        where lower(i.name) like lower(concat('%', :q, '%'))
        order by 
          case 
            when lower(i.name) = lower(:q) then 0
            when lower(i.name) like lower(concat(:q, '%')) then 1
            when lower(i.name) like lower(concat('%', :q)) then 2
            else 3
          end,
          length(i.name) asc
        """)
    List<Ingredient> searchByNameFuzzy(@org.springframework.data.repository.query.Param("q") String q);
}
