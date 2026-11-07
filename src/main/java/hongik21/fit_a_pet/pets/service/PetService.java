package hongik21.fit_a_pet.pets.service;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.pets.dto.PetInfo;
import hongik21.fit_a_pet.pets.dto.PetJoinRequest;
import hongik21.fit_a_pet.pets.entity.*;
import hongik21.fit_a_pet.pets.repository.PetRepository;
import hongik21.fit_a_pet.pets.repository.PetTraitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final PetTraitRepository petTraitRepository;

    /**
     * 펫 생성하기
     * @param member
     * @param request
     * @return
     */
    @Transactional
    public PetInfo createPet(Member member, PetJoinRequest request) {
        Pet pet = Pet.builder()
                .name(request.getName())
                .age(request.getAge())
                .weight(request.getWeight())
                .image(request.getImage())
                .member(member)
                .createdAt(LocalDateTime.now())
                .build();

        // 1. petType
        if(request.getPetType() != null && !request.getPetType().isEmpty()) {
            String upperCase = request.getPetType().toUpperCase();
            pet.setType(PetType.valueOf(upperCase));
        }

        // 2. genderType
        if(request.getGenderType() != null && !request.getGenderType().isEmpty()) {
            String upperCase = request.getGenderType().toUpperCase();
            pet.setGender(PetGenderType.valueOf(upperCase));
        }

        // 3. 성향
        if (request.getTraitNames() != null && !request.getTraitNames().isEmpty()) {
            List<String> traitNames = request.getTraitNames();
            List<PetTrait> traits = petTraitRepository.findAllByNameIn(traitNames);

            for (PetTrait trait : traits) {
                PetTraitRelation relation = new PetTraitRelation();
                relation.setPet(pet);
                relation.setTrait(trait);
                pet.getTraits().add(relation);
            }
        }

        return PetInfo.from(petRepository.save(pet));
    }
    
    // 특정 회원의 펫(들) 조회하기
    public List<PetInfo> getPetsByMember(Member member) {
        List<Pet> pets = petRepository.findByMember(member);
        return pets.stream()
                .map(PetInfo::from)
                .toList();
    }

    // 특정 펫 정보 업데이트
    @Transactional
    public PetInfo updatePet(Long petId, PetJoinRequest request, String email) {
        Pet pet = petRepository.findByIdAndMember_Email(petId, email)
                .orElseThrow(() -> new hongik21.fit_a_pet.global.exception.ApplicationException(hongik21.fit_a_pet.global.exception.CustomErrorCode.PET_NOT_FOUND));
        pet.setName(request.getName());
        pet.setAge(request.getAge());
        pet.setWeight(request.getWeight());
        pet.setImage(request.getImage());

        // 1. petType
        if (request.getPetType() != null && !request.getPetType().isEmpty()) {
            String upperCase = request.getPetType().toUpperCase();
            PetType newPetType = PetType.valueOf(upperCase);

            if (!newPetType.equals(pet.getType())) {
                pet.setType(newPetType);
            }
        }

        // 2. genderType
        if (request.getGenderType() != null && !request.getGenderType().isEmpty()) {
            String upperCase = request.getGenderType().toUpperCase();
            PetGenderType newGenderType = PetGenderType.valueOf(upperCase);

            if (!newGenderType.equals(pet.getGender())) {
                pet.setGender(newGenderType);
            }
        }

        // 3. 성향
        if (request.getTraitNames() != null && !request.getTraitNames().isEmpty()) {
            List<String> requestedTraitNames = request.getTraitNames();

            // 현재 pet에 설정된 trait 이름들 추출
            Set<String> existingTraitNames = pet.getTraits().stream()
                    .map(relation -> relation.getTrait().getName())
                    .collect(Collectors.toSet());

            // 요청에는 있지만 기존에는 없는 것들 (추가해야함)
            List<String> traitsToAdd = requestedTraitNames.stream()
                    .filter(name -> !existingTraitNames.contains(name))
                    .collect(Collectors.toList());

            // 기존 relation 중 제거해야 할 것들
            List<PetTraitRelation> relationsToRemove = pet.getTraits().stream()
                    .filter(relation -> !requestedTraitNames.contains(relation.getTrait().getName()))
                    .toList();

            // 제거
            pet.getTraits().removeAll(relationsToRemove);

            // 추가
            if (!traitsToAdd.isEmpty()) {
                List<PetTrait> traits = petTraitRepository.findAllByNameIn(traitsToAdd);
                for (PetTrait trait : traits) {
                    PetTraitRelation relation = new PetTraitRelation();
                    relation.setPet(pet);
                    relation.setTrait(trait);
                    pet.getTraits().add(relation);
                }
            }

        } else {
            pet.getTraits().clear();
        }

        petRepository.save(pet);
        return PetInfo.from(petRepository.save(pet));
    }

    // 필요하면 알아서 추가 ...
}
