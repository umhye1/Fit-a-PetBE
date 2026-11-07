package hongik21.fit_a_pet.petposts.repository;


import hongik21.fit_a_pet.petposts.domain.PetPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetPostRepository extends JpaRepository<PetPost, Long>{

    Optional<PetPost> findByPetPostIdAndMember_Email(Long petPostId, String email);

    Optional<PetPost> findByPetPostId(Long petPostId);
}
