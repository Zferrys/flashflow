package com.flashflow.order.controller;
import com.flashflow.common.context.UserContext;
import com.flashflow.common.domain.R;
import com.flashflow.order.entity.DeliveryAddress;
import com.flashflow.order.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "收货地址")
@RestController
@RequestMapping("/api/flashflow/order/address")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public R<List<DeliveryAddress>> list() {
        return R.ok(addressService.getUserAddresses(UserContext.getUserId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public R<Void> save(@RequestBody DeliveryAddress address) {
        address.setUserId(UserContext.getUserId());
        addressService.save(address);
        return R.ok();
    }

    @Operation(summary = "修改地址")
    @PutMapping
    public R<Void> update(@RequestBody DeliveryAddress address) {
        // 从 Token 获取当前用户，防止请求体伪造 userId（IDOR 防护）
        address.setUserId(UserContext.getUserId());
        addressService.update(address);
        return R.ok();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        // 先校验地址归属再删除（IDOR 防护）
        List<DeliveryAddress> addresses = addressService.getUserAddresses(UserContext.getUserId());
        boolean owned = addresses.stream().anyMatch(a -> a.getId().equals(id));
        if (!owned) {
            throw new com.flashflow.common.exception.BusinessException(
                    com.flashflow.common.domain.ErrorCode.FORBIDDEN, "无权删除此地址");
        }
        addressService.remove(id);
        return R.ok();
    }

    @Operation(summary = "设为默认")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id, UserContext.getUserId());
        return R.ok();
    }
}
