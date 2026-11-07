package hongik21.fit_a_pet.petposts.service;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import hongik21.fit_a_pet.petposts.domain.PetPost;
import hongik21.fit_a_pet.petposts.dto.*;
import hongik21.fit_a_pet.petposts.repository.PetPostRepository;
import hongik21.fit_a_pet.pets.entity.Pet;
import hongik21.fit_a_pet.pets.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PetPostService {

    private final PetPostRepository petPostRepository;
    private final PetRepository petRepository;
    private final MemberRepository memberRepository;

    // 1. 게시글 작성
    public PetPostWriteResponse writePetpost(String email, PetPostWriteRequest request){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND));

        Pet pet = petRepository.findByIdWithTraits(request.getPetId())
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.PET_NOT_FOUND));
        if (!pet.getMember().getId().equals(member.getId()))
            throw new ApplicationException(CustomErrorCode.PET_NOT_FOUND);
        PetPost post = PetPost.builder()
                .member(member)
                .pet(pet)
                .petPostTitle(request.getPetPostTitle())
                .petPostContent(request.getPetPostContent())
                .petPostCategory(request.getPetPostCategory())
                .petPostDate(request.getPetPostDate())
                .imageUrl(request.getImageUrl())
                .build();

        try{
            PetPost res = petPostRepository.save(post);

            Hibernate.initialize(res.getPet().getTraits());
            // petTraits 가져오기
            List<String> traitNames = res.getPet().getTraits().stream()
                    .map(petTraitRelation -> petTraitRelation.getTrait().getName())
                    .collect(Collectors.toList());

            String petTraitsString = String.join(", ", traitNames);
            return PetPostWriteResponse.builder()
                    .petPostId(res.getPetPostId())
                    .petPostTitle(res.getPetPostTitle())
                    .petPostContent(res.getPetPostContent())
                    .petPostCategory(res.getPetPostCategory())
                    .petPostDate(res.getPetPostDate())
                    .nickname(res.getMember().getNickname())
                    .petName(res.getPet().getName())
                    .petAge(res.getPet().getAge())
                    .petGender(res.getPet().getGender())
                    .petType(res.getPet().getType())
                    .petTraits(petTraitsString)
                    .imageUrl(res.getImageUrl())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND);
        }
    }

    // 2. 수정
    public PetPostEditResponse editPetpost(PetPostEditRequest request, Long petPostId, String email){
        PetPost post = petPostRepository.findByPetPostIdAndMember_Email(petPostId, email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND));

        post.setPetPostTitle(request.getPetPostTitle());
        post.setPetPostContent(request.getPetPostContent());
        post.setPetPostEditDate(request.getPetPostEditDate());
        post.setImageUrl(request.getImageUrl());

        PetPost updatedPost = petPostRepository.save(post);

        try{

            Hibernate.initialize(updatedPost.getPet().getTraits());

            List<String> traitNames = updatedPost.getPet().getTraits().stream()
                    .map(petTraitRelation -> petTraitRelation.getTrait().getName())
                    .collect(Collectors.toList());

            String petTraitsString = String.join(", ", traitNames);

            return PetPostEditResponse.builder()
                    .petPostId(updatedPost.getPetPostId())
                    .petPostTitle(updatedPost.getPetPostTitle())
                    .petPostCategory(updatedPost.getPetPostCategory())
                    .petPostContent(updatedPost.getPetPostContent())
                    .petPostEditDate(updatedPost.getPetPostEditDate())
                    .nickname(updatedPost.getMember().getNickname())
                    .petName(updatedPost.getPet().getName())
                    .petAge(updatedPost.getPet().getAge())
                    .petGender(updatedPost.getPet().getGender())
                    .petType(updatedPost.getPet().getType())
                    .petTraits(petTraitsString)
                    .imageUrl(updatedPost.getImageUrl())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND);
        }
    }
    // 3. 조회
    public PetPostDetailResponse viewPetpost(Long petPostId) {
        PetPost post = petPostRepository.findByPetPostId(petPostId)
                .orElseThrow(()->new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND));

        Hibernate.initialize(post.getPet().getTraits());

        List<String> traitNames = post.getPet().getTraits().stream()
                .map(petTraitRelation -> petTraitRelation.getTrait().getName())
                .collect(Collectors.toList());

        String petTraitsString = String.join(", ", traitNames);

        try {
            return PetPostDetailResponse.builder()
                    .petPostId(post.getPetPostId())
                    .petPostTitle(post.getPetPostTitle())
                    .petPostContent(post.getPetPostContent())
                    .petPostCategory(post.getPetPostCategory())
                    .petPostDate(post.getPetPostEditDate())
                    .nickname(post.getMember().getNickname())
                    .petName(post.getPet().getName())
                    .petAge(post.getPet().getAge())
                    .petGender(post.getPet().getGender())
                    .petType(post.getPet().getType())
                    .petTraits(petTraitsString)
                    .imageUrl(post.getImageUrl())
                    .build();

        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND);
        }
    }

    // 4. 삭제
    public void deletePetpost(Long petPostId, String email) {
        PetPost post = petPostRepository.findByPetPostIdAndMember_Email(petPostId,email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND));

        try {
            petPostRepository.delete(post);
        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.PET_POST_NOT_FOUND);
        }
    }


}
