package hongik21.fit_a_pet.posts.controller;

import hongik21.fit_a_pet.global.CommonResponse;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.posts.domain.PostCategoryType;
import hongik21.fit_a_pet.posts.dto.*;
import hongik21.fit_a_pet.posts.repository.PostRepository;
import hongik21.fit_a_pet.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final PostRepository postRepository;

    @PostMapping()
    public CommonResponse<PostWriteResponse> write(
            @RequestBody PostWriteRequest request) throws ApplicationException{
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        PostWriteResponse response = postService.write(email, request);
        return CommonResponse.onSuccess(response,"포스트 작성에 성공했습니다");
    }

    @PutMapping("/{postId:\\d+}")
    public CommonResponse<PostEditResponse> editPost(
            @RequestBody PostEditRequest request,
            @PathVariable("postId") Long postId) throws ApplicationException{
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        PostEditResponse response = postService.editPost(request, postId, email);
        return CommonResponse.onSuccess(response,"포스트 수정에 성공했습니다.");
    }


    @DeleteMapping("/{postId:\\d+}")
    public CommonResponse<?> deletePost(@PathVariable("postId")  Long postId){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        postService.deletePost(postId, email);
        return CommonResponse.onSuccess(null, "포스트 삭제에 성공했습니다.");
    }

    @GetMapping("/{postId:\\d+}")
    public CommonResponse<PostDetailResponse> viewPost(@PathVariable("postId") Long postId){

        PostDetailResponse response = postService.viewPost(postId);
        return CommonResponse.onSuccess(response,"포스트 조회에 성공했습니다.");

    }

    @GetMapping
    public CommonResponse<PostPageResponse> listPosts(
            @RequestParam(name = "categoryType",required = false) PostCategoryType categoryType,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime cursorDate,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size) {
        PostPageResponse posts = postService.listPost(categoryType, cursorDate, cursorId, size);

        return CommonResponse.onSuccess(posts,"포스트 카테고리 조회에 성공했습니다.");
    }

    @GetMapping("/doTest")
    public String doTest(){
        System.out.println("test");
        return "테스트 성공";
    }
}
