package hongik21.fit_a_pet.posts.repository;

import hongik21.fit_a_pet.posts.domain.Post;
import hongik21.fit_a_pet.posts.domain.PostCategoryType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Select a bounded, ordered key slice before fetching authors. */
@Repository
@RequiredArgsConstructor
public class PostPageQuery {
    private final EntityManager entityManager;
    public record Key(Long postId, LocalDateTime postDate) {}

    public List<Key> findKeys(PostCategoryType category, LocalDateTime date, Long id, int limit) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Key.class);
        var post = query.from(Post.class);
        var predicates = new ArrayList<Predicate>();
        if (category != null) predicates.add(cb.equal(post.get("category"), category.name()));
        if (date != null) predicates.add(cb.or(cb.lessThan(post.get("postDate"), date),
                cb.and(cb.equal(post.get("postDate"), date), cb.lessThan(post.get("postId"), id))));
        query.select(cb.construct(Key.class, post.get("postId"), post.get("postDate")))
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(cb.desc(post.get("postDate")), cb.desc(post.get("postId")));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }
}
