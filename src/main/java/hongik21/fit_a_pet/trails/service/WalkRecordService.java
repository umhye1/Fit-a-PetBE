package hongik21.fit_a_pet.trails.service;


import hongik21.fit_a_pet.trails.domain.WalkRecord;
import hongik21.fit_a_pet.trails.dto.WalkRecordRequestDTO;
import hongik21.fit_a_pet.trails.dto.WalkRecordResponseDTO;
import hongik21.fit_a_pet.trails.repository.WalkRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalkRecordService {

    // 생성자
    private final WalkRecordRepository repository;

    public WalkRecordService(WalkRecordRepository repository) {
        this.repository = repository;
    }

    // 기능 1. 산책 기록 저장
    public WalkRecordResponseDTO save(WalkRecordRequestDTO dto){
        WalkRecord record = new WalkRecord();

        record.setWalkDate(LocalDate.parse(dto.getWalk_date()));
        record.setWalkStart(LocalTime.parse(dto.getWalk_start()));
        record.setWalkEnd(LocalTime.parse(dto.getWalk_end()));
        record.setDistance(dto.getDistance());
        record.setRating(dto.getRating());
        record.setMemo(dto.getMemo());
        record.setPetId(dto.getPet_id());
        record.setUserId(dto.getUser_id());
        record.setAddress(dto.getAddress());

        WalkRecord saved = repository.save(record);

        return new WalkRecordResponseDTO(
                saved.getRecordId(),
                saved.getWalkDate().toString(),
                saved.getWalkStart().toString(),
                saved.getWalkEnd().toString(),
                saved.getDistance(),
                saved.getRating(),
                saved.getMemo(),
                saved.getPetId(),
                saved.getUserId(),
                saved.getAddress()
        );
    }

    // 기능 2. 월별 기록 조회
    public List<WalkRecordResponseDTO> getMonthlyRecords(int year, int month){

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = LocalDate.of(year, month, 31);

        // statDate,endDate 사이의 walkRecord를 가져옴
        return repository.findByWalkDateBetween(startDate, endDate)
                .stream()
                .map(r ->  new WalkRecordResponseDTO(  // 각 walkRecord 객체 r을 WalkRecordResponseDTO 객체로 변환
                        r.getRecordId(),
                        r.getWalkDate().toString(),
                        r.getWalkStart().toString(),
                        r.getWalkEnd().toString(),
                        r.getDistance(),
                        r.getRating(),
                        r.getMemo(),
                        r.getPetId(),
                        r.getUserId(),
                        r.getAddress()
                )) // List<WalkRecord> -> 스트림형태
                .collect(Collectors.toList()); // List<WalkRecordResponseDto> 형태로 변환
    }

    // 기능 3. 날짜별 기록 조회
    public List<WalkRecordResponseDTO> getDateRecords(String dateStr){
        LocalDate date = LocalDate.parse(dateStr);

        return repository.findByWalkDate(date)
                .stream()
                .map(r -> new WalkRecordResponseDTO(
                        r.getRecordId(),
                        r.getWalkDate().toString(),
                        r.getWalkStart().toString(),
                        r.getWalkEnd().toString(),
                        r.getDistance(),
                        r.getRating(),
                        r.getMemo(),
                        r.getPetId(),
                        r.getUserId(),
                        r.getAddress()
                ))
                .collect(Collectors.toList());
    }
}
