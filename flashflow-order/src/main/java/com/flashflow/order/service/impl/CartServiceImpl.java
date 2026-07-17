package com.flashflow.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashflow.order.dao.CartMapper;
import com.flashflow.order.entity.Cart;
import com.flashflow.order.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;

    @Override
    public List<Cart> getUserCart(Long userId) {
        return cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId).orderByDesc(Cart::getCreateTime));
    }

    @Override
    public Cart addOrUpdate(Cart cart) {
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, cart.getUserId()).eq(Cart::getSkuId, cart.getSkuId()));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + cart.getQuantity());
            cartMapper.updateById(exist);
            return exist;
        }
        cart.setChecked(1);
        cartMapper.insert(cart);
        return cart;
    }

    @Override
    public void updateQuantity(Long id, Integer quantity) {
        Cart c = new Cart();
        c.setId(id);
        c.setQuantity(quantity);
        cartMapper.updateById(c);
    }

    @Override
    public void remove(Long id) {
        cartMapper.deleteById(id);
    }

    @Override
    public void clearChecked(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId).eq(Cart::getChecked, 1));
    }

    @Override
    public void toggleChecked(Long id, Integer checked) {
        Cart c = new Cart();
        c.setId(id);
        c.setChecked(checked);
        cartMapper.updateById(c);
    }
}
