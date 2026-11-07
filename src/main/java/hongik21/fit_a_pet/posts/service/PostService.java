package hongik21.fit_a_pet.posts.service;

import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import hongik21.fit_a_pet.posts.domain.*;
import hongik21.fit_a_pet.posts.dto.*;
import hongik21.fit_a_pet.posts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final hongik21.fit_a_pet.posts.repository.PostPageQuery postPageQuery;
    private final MemberRepository memberRepository;

    // 1. 게시글 작성
    @Transactional
    public PostWriteResponse write(String email, PostWriteRequest request){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.MEMBER_NOT_FOUND));

        Post post;
        LocalDateTime now = LocalDateTime.now();

        switch (request.getPostCategory()){
            case FREE:
                post = FreePost.builder()
                        .postTitle(request.getPostTitle())
                        .postContent(request.getPostContent())
                        .postDate(now)
                        .build();
                break;

            case QUESTION:
                post = QuestionPost.builder()
                        .postTitle(request.getPostTitle())
                        .postContent(request.getPostContent())
                        .postDate(now)
                        .build();
                break;

            case INFO:
                post = InfoPost.builder()
                        .postTitle(request.getPostTitle())
                        .postContent(request.getPostContent())
                        .postDate(now)
                        .build();
                break;
            default :
                throw new ApplicationException(CustomErrorCode.INVALID_POST_CATEGORY);
        }

        try {
            post.setMember(member);
            Post res = postRepository.save(post);

            PostCategoryType postCategoryType =
                    (res instanceof FreePost) ? PostCategoryType.FREE :
                    (res instanceof QuestionPost) ? PostCategoryType.QUESTION :
                    (res instanceof InfoPost) ? PostCategoryType.INFO :
                    null;

            if (postCategoryType == null) {
                throw new ApplicationException(CustomErrorCode.INVALID_POST_CATEGORY);
            }

                return PostWriteResponse.builder()
                    .postId(res.getPostId())
                    .postTitle(res.getPostTitle())
                    .postCategory(postCategoryType)
                    .postDate(res.getPostDate())
                    .postContent(res.getPostContent())
                    .nickname(res.getMember().getNickname())
                    .build();

        } catch(Exception e){
            throw new ApplicationException(CustomErrorCode.POST_WRITE_FAILED);
        }
    }

    // 2. 게시물 수정
    @Transactional
    public PostEditResponse editPost(PostEditRequest request, Long postId, String email) {
        Post post = postRepository.findByPostIdAndMember_Email(postId, email)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.POST_NOT_FOUND));

        post.setPostTitle(request.getPostTitle());
        post.setPostContent(request.getPostContent());
        post.setPostEditDate(LocalDateTime.now());

        Post updatedPost = postRepository.save(post);

        PostCategoryType postCategoryType;
        if (updatedPost instanceof FreePost) {
            postCategoryType = PostCategoryType.FREE;
        } else if (updatedPost instanceof QuestionPost) {
            postCategoryType = PostCategoryType.QUESTION;
        } else if (updatedPost instanceof InfoPost) {
            postCategoryType = PostCategoryType.INFO;
        } else {
            throw new ApplicationException(CustomErrorCode.INVALID_POST_CATEGORY);
        }

        return PostEditResponse.builder()
                .postId(updatedPost.getPostId())
                .postCategory(postCategoryType)
                .postTitle(updatedPost.getPostTitle())
                .postContent(updatedPost.getPostContent())
                .postEditDate(updatedPost.getPostEditDate())
                .nickname(updatedPost.getMember().getNickname())
                .build();
    }

    // 3. 게시물 삭제
    @Transactional
    public void deletePost(Long postId, String email) {
        Post post = postRepository.findByPostIdAndMember_Email(postId, email)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.POST_NOT_FOUND));

        postRepository.delete(post);
    }

    // 4. 게시물 조회
    @Transactional(readOnly = true)
    public PostDetailResponse viewPost(Long postId) {
        Post post = postRepository.findByPostId(postId)
                .orElseThrow(() -> new ApplicationException(CustomErrorCode.POST_NOT_FOUND));

        PostCategoryType postCategoryType;

        if (post instanceof FreePost) {
            postCategoryType = PostCategoryType.FREE;
        } else if (post instanceof QuestionPost) {
            postCategoryType = PostCategoryType.QUESTION;
        } else if (post instanceof InfoPost) {
            postCategoryType = PostCategoryType.INFO;
        } else {
            throw new ApplicationException(CustomErrorCode.INVALID_POST_CATEGORY);
        }

        return PostDetailResponse.builder()
                .postId(post.getPostId())
                .postCategory(postCategoryType)
                .postTitle(post.getPostTitle())
                .postDate(post.getPostDate())
                .postContent(post.getPostContent())
                .postEditDate(post.getPostEditDate())
                .nickname(post.getMember().getNickname())
                .build();
    }


    // Bounded key query followed by a bounded fetch join; no count query.
    @Transactional(readOnly = true)
    public PostPageResponse listPost(PostCategoryType category, LocalDateTime cursorDate, Long cursorId, int size) {
        if (size < 1 || size > 20 || (cursorDate == null) != (cursorId == null) || (cursorId != null && cursorId < 1))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid cursor or size");
        var keys = postPageQuery.findKeys(category, cursorDate, cursorId, size + 1);
        boolean more = keys.size() > size;
        var pageKeys = keys.subList(0, Math.min(size, keys.size()));
        if (pageKeys.isEmpty()) return new PostPageResponse(List.of(), false, null, null);
        var byId = postRepository.findPageWithMembers(pageKeys.stream().map(k -> k.postId()).toList())
                .stream().collect(Collectors.toMap(Post::getPostId, p -> p));
        List<PostListResponse> items = pageKeys.stream().map(k -> byId.get(k.postId()))
                .filter(java.util.Objects::nonNull).map(post -> PostListResponse.builder()
                        .postId(post.getPostId()).postTitle(post.getPostTitle()).postCategory(getPostCategoryType(post))
                        .nickname(post.getMember().getNickname()).postDate(post.getPostDate()).build()).toList();
        var last = more ? pageKeys.getLast() : null;
        return new PostPageResponse(items, more, last == null ? null : last.postDate(), last == null ? null : last.postId());
    }
    private PostCategoryType getPostCategoryType(Post post) {
        if (post instanceof FreePost) {
            return PostCategoryType.FREE;
        } else if (post instanceof QuestionPost) {
            return PostCategoryType.QUESTION;
        } else if (post instanceof InfoPost) {
            return PostCategoryType.INFO;
        }
        return null; // 예외 처리 필요 시 throw new ApplicationException(CustomErrorCode.INVALID_POST_CATEGORY);
    }
}
