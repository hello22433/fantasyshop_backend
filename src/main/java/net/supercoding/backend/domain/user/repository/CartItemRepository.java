package net.supercoding.backend.domain.user.repository;

import net.supercoding.backend.domain.item.entity.ItemEntity;
import net.supercoding.backend.domain.user.entity.CartItem;
import net.supercoding.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByUserAndItem(User user, ItemEntity item);
    List<CartItem> findByUser(User user);
    List<CartItem> findAllByUser(User user);
    void deleteByUser(User user);

    @Modifying
    @Transactional
    void deleteByItem_ItemPk(Long itemPk);

    void deleteByUser_UserPk(Long userPk);
}
