package hongik21.fit_a_pet.trails.service;


import hongik21.fit_a_pet.accounts.entity.Member;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.global.exception.ApplicationException;
import hongik21.fit_a_pet.global.exception.CustomErrorCode;
import hongik21.fit_a_pet.trails.domain.WalkRecord;
import hongik21.fit_a_pet.trails.dto.*;
import hongik21.fit_a_pet.trails.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalkRecordService {

    // 생성자
    private final WalkRecordRepository repository;
    private final MemberRepository memberRepository;
    private final hongik21.fit_a_pet.pets.repository.PetRepository petRepository;

    // 기능 1. 산책 기록 저장
    public WalkRecordSaveResponse register(String email, WalkRecordSaveRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.MEMBER_NOT_FOUND));

        if (request.getPetId() == null || !petRepository.existsByIdAndMember_Email(request.getPetId().longValue(), email))
            throw new ApplicationException(CustomErrorCode.PET_NOT_FOUND);
        LocalTime walkStart = LocalTime.parse(request.getWalkStart());
        LocalTime walkEnd = LocalTime.parse(request.getWalkEnd());
        Duration duration = Duration.between(walkStart,walkEnd);
        String formattedDuration = String.format("%02d:%02d:%02d",
                duration.getSeconds() / 3600,
                (duration.getSeconds() % 3600) / 60,
                duration.getSeconds() % 60
        );

        WalkRecord walkRecord = WalkRecord.builder()
                .memberId(member)
                .petId(request.getPetId())
                .walkDate(LocalDate.parse(request.getWalkDate()))
                .walkStart(LocalTime.parse(request.getWalkStart()))
                .walkEnd(LocalTime.parse(request.getWalkEnd()))
                .formattedDuration(formattedDuration)
                .distance(request.getDistance())
                .address(request.getAddress())
                .memo(request.getMemo())
                .rating(request.getRating())
                .build();

        try {
            WalkRecord res = repository.save(walkRecord);
            return WalkRecordSaveResponse.builder()
                    .recordId(res.getRecordId())
                    .walkDate(res.getWalkDate().toString())
                    .walkStart(res.getWalkStart().toString())
                    .walkEnd(res.getWalkEnd().toString())
                    .formattedDuration(formattedDuration)
                    .distance(res.getDistance())
                    .petId(res.getPetId())
                    .address(res.getAddress())
                    .memo(res.getMemo())
                    .rating(res.getRating())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.TRAIL_CREATE_FAILED);
        }
    }

    // 기능 2. 월별 기록 조회

    public List<WalkRecordMonthlyResponse> getMonthlyRecords(String email, int year, int month){

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.TRAIL_NOT_FOUND));

        try{
            return repository.findByMemberIdEmailAndWalkDateBetween(email, startDate, endDate)
                    .stream()
                    .map(r -> new WalkRecordMonthlyResponse(
                            r.getRecordId(),
                            r.getWalkDate().toString(),
                            r.getWalkStart().toString(),
                            r.getWalkEnd().toString(),
                            r.getFormattedDuration(),
                            r.getDistance(),
                            r.getPetId(),
                            r.getAddress(),
                            r.getRating(),
                            r.getMemo()
                    ))
                    .toList();
        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.TRAIL_NOT_FOUND);
        }
    }

    // 기능 3. 단건 기록 조회

    public WalkRecordDetailResponse getRecordsDetail(Long recordId, String email){

        WalkRecord walkRecord = repository.findByRecordIdAndMemberIdEmail(recordId, email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.TRAIL_NOT_FOUND));

        try{
            return WalkRecordDetailResponse.builder()
                    .recordId(walkRecord.getRecordId())
                    .walkDate(walkRecord.getWalkDate().toString())
                    .walkStart(walkRecord.getWalkStart().toString())
                    .walkEnd(walkRecord.getWalkEnd().toString())
                    .formattedDuration(walkRecord.getFormattedDuration())
                    .distance(walkRecord.getDistance())
                    .petId(walkRecord.getPetId())
                    .address(walkRecord.getAddress())
                    .rating(walkRecord.getRating())
                    .memo(walkRecord.getMemo())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.TRAIL_NOT_FOUND);
        }
    }

    // 기능 4. 산책 기록 편집

    public WalkRecordEditResponse editRecord(WalkRecordEditRequest request, Long recordId, String email){
        WalkRecord walkRecord = repository.findByRecordIdAndMemberIdEmail(recordId,email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.MEMBER_NOT_FOUND));

        try{
            walkRecord.setMemo(request.getMemo());
            walkRecord.setRating(request.getRating());

            WalkRecord updatedRecord = repository.save(walkRecord);

            return WalkRecordEditResponse.builder()
                    .recordId(updatedRecord.getRecordId())
                    .walkDate(updatedRecord.getWalkDate().toString())
                    .walkStart(updatedRecord.getWalkStart().toString())
                    .walkEnd(updatedRecord.getWalkEnd().toString())
                    .formattedDuration(updatedRecord.getFormattedDuration())
                    .distance(updatedRecord.getDistance())
                    .petId(updatedRecord.getPetId())
                    .address(updatedRecord.getAddress())
                    .rating(updatedRecord.getRating())
                    .memo(updatedRecord.getMemo())
                    .build();

        }catch (Exception e){
            throw new ApplicationException(CustomErrorCode.TRAIL_NOT_FOUND);
        }
    }

    // 기능 5. 산책 기록 삭제

    public void deleteRecord(Long recordId, String email){
        WalkRecord walkRecord = repository.findByRecordIdAndMemberIdEmail(recordId,email)
                .orElseThrow(()-> new ApplicationException(CustomErrorCode.MEMBER_NOT_FOUND));

        try{
            repository.delete(walkRecord);
        } catch (Exception e) {
            throw new ApplicationException(CustomErrorCode.TRAIL_NOT_FOUND);
        }
    }

}
