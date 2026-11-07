package hongik21.fit_a_pet.trails.repository;

import hongik21.fit_a_pet.trails.domain.WalkRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WalkRecordRepository extends JpaRepository<WalkRecord, Long> {

    // 날짜 기준 월별 조회 - 날짜 범위
    List<WalkRecord> findByMemberIdEmailAndWalkDateBetween(String email, LocalDate startDate, LocalDate endDate);

    // 단건 조회
    Optional<WalkRecord> findByRecordIdAndMemberIdEmail(Long recordId, String email);



}
