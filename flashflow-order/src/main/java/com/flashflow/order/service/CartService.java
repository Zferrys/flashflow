package com.flashflow.order.service;
import com.flashflow.order.entity.Cart;
import java.util.List;
public interface CartService {
    List<Cart> getUserCart(Long userId);
    Cart addOrUpdate(Cart cart);
    void updateQuantity(Long id, Integer quantity);
    void remove(Long id);
    void clearChecked(Long userId);
    void toggleChecked(Long id, Integer checked);
}
