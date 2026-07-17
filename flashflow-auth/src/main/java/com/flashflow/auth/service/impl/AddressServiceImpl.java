package com.flashflow.auth.service.impl;

import com.flashflow.auth.dao.UserAddressMapper;
import com.flashflow.auth.entity.UserAddress;
import com.flashflow.auth.service.AddressService;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> listByUserId(Long userId) {
        return userAddressMapper.selectByUserId(userId);
    }

    @Override
    public UserAddress getDefault(Long userId) {
        return userAddressMapper.selectDefault(userId);
    }

    @Override
    public UserAddress getById(Long id) {
        UserAddress addr = userAddressMapper.selectById(id);
        if (addr == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "地址不存在");
        }
        return addr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(UserAddress address) {
        if (address.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        // 如果是第一个地址，自动设为默认
        List<UserAddress> existing = userAddressMapper.selectByUserId(address.getUserId());
        if (existing.isEmpty()) {
            address.setIsDefault(1);
        } else if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            userAddressMapper.clearDefault(address.getUserId());
        }
        userAddressMapper.insert(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserAddress address) {
        UserAddress exist = getById(address.getId());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            userAddressMapper.clearDefault(exist.getUserId());
        }
        address.setUserId(exist.getUserId()); // 防止篡改 userId
        userAddressMapper.updateById(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        UserAddress addr = getById(id);
        if (!addr.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己的地址");
        }
        userAddressMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id, Long userId) {
        UserAddress addr = getById(id);
        if (!addr.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能设置自己的地址");
        }
        userAddressMapper.clearDefault(userId);
        addr.setIsDefault(1);
        userAddressMapper.updateById(addr);
    }
}
