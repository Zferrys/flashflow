package com.flashflow.order.controller;
import com.flashflow.common.context.UserContext;
import com.flashflow.common.domain.R;
import com.flashflow.order.entity.Cart;
import com.flashflow.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "购物车")
@RestController
@RequestMapping("/api/flashflow/order/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @Operation(summary = "购物车列表")
    @GetMapping
    public R<List<Cart>> list() {
        return R.ok(cartService.getUserCart(UserContext.getUserId()));
    }

    @Operation(summary = "添加/更新购物车")
    @PostMapping
    public R<Cart> add(@RequestBody Cart cart) {
        cart.setUserId(UserContext.getUserId());
        return R.ok(cartService.addOrUpdate(cart));
    }

    @Operation(summary = "更新数量")
    @PutMapping("/{id}/quantity")
    public R<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(id, quantity);
        return R.ok();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return R.ok();
    }

    @Operation(summary = "清空已选")
    @DeleteMapping("/checked")
    public R<Void> clearChecked() {
        cartService.clearChecked(UserContext.getUserId());
        return R.ok();
    }

    @Operation(summary = "切换选中状态")
    @PutMapping("/{id}/checked")
    public R<Void> toggleChecked(@PathVariable Long id, @RequestParam Integer checked) {
        cartService.toggleChecked(id, checked);
        return R.ok();
    }
}
