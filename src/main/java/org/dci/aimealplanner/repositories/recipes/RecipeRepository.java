package org.dci.aimealplanner.repositories.recipes;

import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.models.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
    @Query("""
        select r from Recipe r
        where lower(r.title) like lower(concat('%', :q, '%'))
          and ( r.sourceType = org.dci.aimealplanner.models.SourceType.USER
                or (r.sourceType = org.dci.aimealplanner.models.SourceType.AI
                    and r.author.id = :userId) )
        """)
    Page<Recipe> searchForPlanner(@Param("q") String q,
                                  @Param("userId") Long userId,
                                  Pageable pageable);

    @Query("""
           SELECT r FROM Recipe r
           WHERE r.featured = true
           ORDER BY 
               CASE WHEN r.image IS NOT NULL THEN 0 ELSE 1 END,
               r.id DESC
           """)
    List<Recipe> findFeatured(Pageable pageable);

    @Query("""
           SELECT r FROM Recipe r
           ORDER BY 
               CASE WHEN r.image IS NOT NULL THEN 0 ELSE 1 END,
               r.id DESC
           """)
    List<Recipe> findNewest(Pageable pageable);

    default List<Recipe> featuredOrNewest(int limit) {
        var p = org.springframework.data.domain.PageRequest.of(0, limit);
        var picks = findFeatured(p);
        return picks.isEmpty() ? findNewest(p) : picks;
    }

    Page<Recipe> findByAuthor_IdAndSourceType(Long authorId, SourceType sourceType, Pageable pageable);
}
