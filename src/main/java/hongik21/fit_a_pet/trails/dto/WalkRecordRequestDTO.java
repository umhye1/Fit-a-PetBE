package hongik21.fit_a_pet.trails.dto;

import lombok.Getter;
import lombok.Setter;

// 프론트 -> 백엔드로 산책 기록 저장 요청을 보낼 때 사용
// 요청 DTO는 Spring이 setter로 자동 채워주기 때문에 생성자 불필요,
@Getter
@Setter
public class WalkRecordRequestDTO {

    // 문자열로 받아서 LocalTime, LocalDate로 변경하는게 일반적이라고 함
    private String walk_date;
    private String walk_start;
    private String walk_end;

    private Float distance;
    private Integer rating;
    private String memo;

    private Integer pet_id;
    private Integer user_id;

    // 위치 정보
    private String Address;

}
