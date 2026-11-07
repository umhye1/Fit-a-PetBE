package hongik21.fit_a_pet.pets.controller;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.service.MemberService;
import hongik21.fit_a_pet.global.CommonResponse;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import hongik21.fit_a_pet.pets.dto.PetInfo;
import hongik21.fit_a_pet.pets.dto.PetJoinRequest;
import hongik21.fit_a_pet.pets.entity.Pet;
import hongik21.fit_a_pet.pets.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mypage/pet")
@RequiredArgsConstructor
public class PetController {

    private final MemberService memberService;
    private final PetService petService;

    @PostMapping
    public CommonResponse<PetInfo> createPet(@RequestBody PetJoinRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();  // 로그인한 사용자의 이메일
        Member member = memberService.getMemberByEmail(email);
        PetInfo response = petService.createPet(member, request);
        return CommonResponse.onSuccess(response, "펫 생성 성공");
    }

    @GetMapping
    public CommonResponse<List<PetInfo>> getPet() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();  // 로그인한 사용자의 이메일
        Member member = memberService.getMemberByEmail(email);
        List<PetInfo> response = petService.getPetsByMember(member);
        return CommonResponse.onSuccess(response, "펫 조회 성공");
    }

    @PutMapping("/{pet_id}")
    public CommonResponse<PetInfo> updatePet(@PathVariable("pet_id") Long pet_id, @RequestBody PetJoinRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        PetInfo response = petService.updatePet(pet_id, request, email);
        return CommonResponse.onSuccess(response, "펫 편집 성공");
    }
}
