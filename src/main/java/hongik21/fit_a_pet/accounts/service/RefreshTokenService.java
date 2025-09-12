//package hongik21.fit_a_pet.accounts.service;
//
//import hongik21.fit_a_pet.accounts.entity.Member;
//import hongik21.fit_a_pet.accounts.entity.RefreshToken;
//import hongik21.fit_a_pet.accounts.repository.RefreshTokenRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
////@Service
//@RequiredArgsConstructor
//public class RefreshTokenService {
//    private final RefreshTokenRepository refreshTokenRepository;
//
//    public void save(String refreshToken, Member member) {
//        RefreshToken token = RefreshToken.builder()
//                .member(member)
//                .token(refreshToken)
//                .build();
//
//        refreshTokenRepository.save(token);
//    }
//
//    public void delete(String refreshToken) {
//        refreshTokenRepository.findByToken(refreshToken)
//                .ifPresent(refreshTokenRepository::delete);
//    }
//
//    public void delete(Long memberId) {
//        refreshTokenRepository.findByMemberId(memberId)
//                .ifPresent(refreshTokenRepository::delete);
//    }
//}
