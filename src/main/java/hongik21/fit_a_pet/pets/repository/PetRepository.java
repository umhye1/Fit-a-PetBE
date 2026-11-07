package hongik21.fit_a_pet.pets.repository;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.pets.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByMember(Member member);

    boolean existsByIdAndMember_Email(Long id, String email);
    Optional<Pet> findByIdAndMember_Email(Long id, String email);


    @Query("SELECT p FROM Pet p LEFT JOIN FETCH p.traits WHERE p.id = :id")
    Optional<Pet> findByIdWithTraits(@Param("id") Long id);

}
