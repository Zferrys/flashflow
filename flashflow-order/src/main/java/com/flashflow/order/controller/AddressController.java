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
        addressService.update(address);
        return R.ok();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
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
