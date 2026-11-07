package hongik21.fit_a_pet.posts.repository;

import hongik21.fit_a_pet.posts.domain.Post;
import hongik21.fit_a_pet.posts.domain.PostCategoryType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    // postId, email로 본인이 맞는지 확인하는 용도
    Optional<Post> findByPostIdAndMember_Email(@Param("postId") Long postId, @Param("email") String email);

    // 단건 조회
    Optional<Post> findByPostId(Long postId);

    @Query("SELECT p FROM Post p JOIN FETCH p.member WHERE p.postId IN :ids")
    List<Post> findPageWithMembers(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Post p JOIN FETCH p.member")
    List<Post> findAllWithMembers();

}
