package net.supercoding.backend.domain.user.repository;

import net.supercoding.backend.domain.user.entity.PaymentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentItemRepository extends JpaRepository<PaymentItem, Long> {
    void deleteByPayment_User_UserPk(Long userId);
    // 결제 아이템 저장, 조회를 기본 JpaRepository 기능으로 충분히 처리 가능
}
