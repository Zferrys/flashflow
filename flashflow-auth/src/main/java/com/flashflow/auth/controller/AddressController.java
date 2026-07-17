package com.flashflow.auth.controller;

import com.flashflow.auth.entity.UserAddress;
import com.flashflow.auth.security.SecurityUtils;
import com.flashflow.auth.service.AddressService;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import com.flashflow.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端用户收货地址管理
 */
@Tag(name = "收货地址")
@RestController
@RequestMapping("/api/flashflow/auth/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public R<List<UserAddress>> list() {
        Long userId = getCurrentUserId();
        return R.ok(addressService.listByUserId(userId));
    }

    @Operation(summary = "默认地址")
    @GetMapping("/default")
    public R<UserAddress> getDefault() {
        Long userId = getCurrentUserId();
        return R.ok(addressService.getDefault(userId));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public R<Void> create(@RequestBody UserAddress address) {
        address.setUserId(getCurrentUserId());
        addressService.create(address);
        return R.ok();
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody UserAddress address) {
        address.setId(id);
        addressService.update(address);
        return R.ok();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        addressService.delete(id, getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "设为默认")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id, getCurrentUserId());
        return R.ok();
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
