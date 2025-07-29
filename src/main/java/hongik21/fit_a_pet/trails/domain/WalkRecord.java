package hongik21.fit_a_pet.trails.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
public class WalkRecord {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본키 생성
    private Integer recordId;

    private LocalDate walkDate;
    private LocalTime walkStart;
    private LocalTime walkEnd;

    private Float distance;
    private Integer rating;
    private String memo;

    private Integer petId;
    private Integer userId;

    // 위치 정보
    private String Address;

    // 기본 생성자
    public WalkRecord (){}

}
