package net.supercoding.backend.domain.user.repository;

import net.supercoding.backend.domain.user.entity.Payment;
import net.supercoding.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByUserOrderByPaymentDateDesc(User user);

    List<Payment> findByUser_UserPk(Long userId);
}
