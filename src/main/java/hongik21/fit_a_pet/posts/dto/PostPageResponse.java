package hongik21.fit_a_pet.posts.dto;
import java.time.LocalDateTime;
import java.util.List;
public record PostPageResponse(List<PostListResponse> items, boolean hasNext,
                               LocalDateTime nextDate, Long nextId) {}
