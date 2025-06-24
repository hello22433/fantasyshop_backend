package net.supercoding.backend.domain.user.service;

import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import net.supercoding.backend.domain.user.dto.login.LoginRequestDto;
import net.supercoding.backend.domain.user.dto.profile.UserProfileResponseDto;
import net.supercoding.backend.domain.user.dto.profile.UserProfileUpdateRequestDto;
import net.supercoding.backend.domain.user.dto.signup.SignupRequestDto;
import net.supercoding.backend.domain.user.entity.User;
import net.supercoding.backend.domain.user.repository.CartItemRepository;
import net.supercoding.backend.domain.user.repository.PaymentItemRepository;
import net.supercoding.backend.domain.user.repository.UserRepository;
import net.supercoding.backend.domain.user.service.payment.PaymentService;
import net.supercoding.backend.domain.user.service.payment.PaymentServiceImpl;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final CartItemRepository cartItemRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final PaymentServiceImpl paymentService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String IMAGE_BASE_PATH = "/home/ubuntu/www/fantasyshop/assets/images/";

//    private final ImageUploader imageUploader;

    @Transactional
    public void registerUser(SignupRequestDto dto) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        User user = User.builder()
                .email(dto.getEmail())
                .password(encodedPassword)
                .userName(dto.getUserName())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .build();

        userRepository.save(user);
    }

    // 로그인 검증 메서드
    public User authenticate(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("유저 없음"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("비밀번호 틀림");
            // 실무에선 해킹 위험때문에 "이메일 또는 비밀번호가 일치하지 않습니다" 응답
        }

        return user;
    }

    // 사용자를 json 형식으로 변환하기 위함
    public UserProfileResponseDto getMyProfile(User user) {
        return UserProfileResponseDto.from(user);
    }

    // 유저 정보 업데이트
    @Transactional
    public User updateUserProfile(Long userPk, UserProfileUpdateRequestDto updateDto) throws IOException, java.io.IOException {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (updateDto.getUserName() != null) user.setUserName(updateDto.getUserName());
        if (updateDto.getPhoneNumber() != null) user.setPhoneNumber(updateDto.getPhoneNumber());
        if (updateDto.getAddress() != null) user.setAddress(updateDto.getAddress());

        MultipartFile image = updateDto.getProfileImage();
        if (image != null && !image.isEmpty()) {
            String today = LocalDate.now().toString();

            // 기존 이미지 삭제
            String existingImageUrl = user.getProfileImageUrl();
            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                // 기존 (JAR 환경에서 문제)
                // String projectRoot = System.getProperty("user.dir");
                // String existingImagePath = projectRoot + "/src/main/resources/static" + existingImageUrl;
                // 변경: EC2 등 실제 저장 경로로 지정
//                String existingImagePath = "/home/ec2-user/images" + existingImageUrl;
                String existingImagePath = IMAGE_BASE_PATH + existingImageUrl;

                File existingFile = new File(existingImagePath);
                if (existingFile.exists()) existingFile.delete();
            }

            // 새 이미지 저장
            // String projectRoot = System.getProperty("user.dir");
            // String uploadDirPath = projectRoot + "/src/main/resources/static/images/" + today;
            // 변경: EC2 등 실제 저장 경로로 지정
//            String uploadDirPath = "/home/ec2-user/images/" + today; // 변경된 부분
            String uploadDirPath = IMAGE_BASE_PATH + today; // 변경된 부분

            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String savedFileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            File savedFile = new File(uploadDir, savedFileName);
            // 기존방식저장
//            image.transferTo(savedFile);
            // 변경 방식 (리사이징 + 용량 줄이기)
            Thumbnails.of(image.getInputStream())
                    .size(600, 600)          // 최대 크기 제한 (가로/세로 비율 유지됨)
                    .outputQuality(0.8f)     // 압축 품질 (0.0 ~ 1.0), 낮을수록 용량 감소
                    .toFile(savedFile);


//            String imageUrl = "/images/" + today + "/" + savedFileName;  // 상대경로
            String imageUrl = today + "/" + savedFileName;  // 상대경로
            user.setProfileImageUrl(imageUrl);
        }

        return user;
    }

//    @Transactional
//    public User updateUserProfile(Long userPk, UserProfileUpdateRequestDto updateDto) {
//        User user = userRepository.findById(userPk)
//                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
//
//        if (updateDto.getUserName() != null) user.setUserName(updateDto.getUserName());
//        if (updateDto.getPhoneNumber() != null) user.setPhoneNumber(updateDto.getPhoneNumber());
//        if (updateDto.getAddress() != null) user.setAddress(updateDto.getAddress());
//        if (updateDto.getProfileImage() != null && !updateDto.getProfileImage().isEmpty()) {
//            // 이미지 저장 처리 (예: 로컬 디스크 or S3)
//            String imageUrl = imageUploader.upload(updateDto.getProfileImage());
//            user.setProfileImageUrl(imageUrl);
//        }
//
//        // JPA 변경감지에 의해 별도의 save 호출 없이도 변경내용 반영됨
//        return user;
//    }


    // 회원탈퇴
    @Transactional
    public void deleteUserById(Long userId) {
        // 1. 장바구니 아이템 삭제
        cartItemRepository.deleteByUser_UserPk(userId);

        // 2. 결제 항목 삭제
        paymentItemRepository.deleteByPayment_User_UserPk(userId);

        // 3. 결제 정보 삭제 (paymentService 내부에서 처리되면 좋음)
        paymentService.deletePaymentsByUserId(userId);

        // 4. 유저 삭제
        userRepository.deleteById(userId);
    }

    // 비밀번호 변경
    public boolean changePassword(UserDetails userDetails, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return true;
    }

}
