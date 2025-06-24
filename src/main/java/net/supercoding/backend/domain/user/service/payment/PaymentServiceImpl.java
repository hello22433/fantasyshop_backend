package net.supercoding.backend.domain.user.service.payment;

import lombok.RequiredArgsConstructor;
import net.supercoding.backend.domain.item.entity.ItemEntity;
import net.supercoding.backend.domain.item.repository.ItemRepository;
import net.supercoding.backend.domain.user.dto.payment.CreatePaymentRequestDto;
import net.supercoding.backend.domain.user.dto.payment.PaymentResponseDto;
import net.supercoding.backend.domain.user.dto.payment.PaymentSimpleDto;
import net.supercoding.backend.domain.user.entity.Payment;
import net.supercoding.backend.domain.user.entity.PaymentItem;
import net.supercoding.backend.domain.user.entity.User;
import net.supercoding.backend.domain.user.repository.PaymentItemRepository;
import net.supercoding.backend.domain.user.repository.PaymentRepository;
import net.supercoding.backend.domain.user.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final ItemRepository itemRepository;
    private final CartService cartService;

    @Override
    public PaymentResponseDto createPayment(User user, List<CreatePaymentRequestDto.PaymentItemRequestDto> items) {
        // 1. 아이템 조회 및 가격/재고 확인, 재고 차감
        List<PaymentItem> paymentItems = items.stream().map(reqItem -> {
            ItemEntity item = itemRepository.findById(reqItem.getItemPk())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid item PK: " + reqItem.getItemPk()));

            Long quantity = reqItem.getQuantity();

            // 재고 부족할때 -로 처리되게 하여 예외처리 하지 않음
//            if (item.getItemInventory() < quantity) {
//                throw new IllegalArgumentException("재고가 부족한 아이템입니다: " + item.getItemName());
//            }

            // 재고 차감
            item.setItemInventory(item.getItemInventory() - quantity);

            PaymentItem paymentItem = new PaymentItem();
            paymentItem.setItemPk(item.getItemPk());
            paymentItem.setItemName(item.getItemName());
            paymentItem.setQuantity(quantity);
            paymentItem.setPriceAtOrderTime(item.getItemPrice());

            return paymentItem;
        }).collect(Collectors.toList());

        // 2. 총 결제 금액 계산
        long totalPrice = paymentItems.stream()
                .mapToLong(pi -> pi.getPriceAtOrderTime() * pi.getQuantity())
                .sum();

        // 3. 결제 객체 생성 및 저장
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTotalPrice(totalPrice); // 필요시 long 유지도 고려
        payment.setPaymentStatus("paid");

        payment = paymentRepository.save(payment);

        // 4. 각 결제 아이템에 payment 연관관계 설정 및 저장
        Payment finalPayment = payment;
        paymentItems.forEach(pi -> pi.setPayment(finalPayment));
        paymentItemRepository.saveAll(paymentItems);

        payment.setItems(paymentItems);

        // 5. 장바구니 비우기
        cartService.clearCart(user);

        // 6. DTO 변환 및 반환
        return toPaymentResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSimpleDto> getPaymentHistory(User user) {
        List<Payment> payments = paymentRepository.findAllByUserOrderByPaymentDateDesc(user);
        return payments.stream()
                .map(p -> {
                    PaymentSimpleDto dto = new PaymentSimpleDto();
                    dto.setPaymentPk(p.getId());
                    dto.setPaymentDate(p.getPaymentDate());
                    dto.setTotalPrice(p.getTotalPrice());
                    dto.setPaymentStatus(p.getPaymentStatus());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentDetail(User user, Long paymentPk) {
        Payment payment = paymentRepository.findById(paymentPk)
                .filter(p -> p.getUser().getEmail().equals(user.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found or access denied"));

        return toPaymentResponseDto(payment);
    }

    @Override
    public void deletePaymentsByUserId(Long userId) {
        List<Payment> payments = paymentRepository.findByUser_UserPk(userId);
        paymentRepository.deleteAll(payments);
    }

    private PaymentResponseDto toPaymentResponseDto(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentPk(payment.getId());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setTotalPrice(payment.getTotalPrice());
        dto.setPaymentStatus(payment.getPaymentStatus());

        List<PaymentResponseDto.PaymentItemDto> itemDtos = payment.getItems().stream().map(pi -> {
            PaymentResponseDto.PaymentItemDto itemDto = new PaymentResponseDto.PaymentItemDto();
            itemDto.setItemPk(pi.getItemPk());
            itemDto.setItemName(pi.getItemName());
            itemDto.setQuantity(pi.getQuantity());
            itemDto.setPriceAtOrderTime(pi.getPriceAtOrderTime());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);

        return dto;
    }
}