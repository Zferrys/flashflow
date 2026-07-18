package com.flashflow.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashflow.order.dao.DeliveryAddressMapper;
import com.flashflow.order.entity.DeliveryAddress;
import com.flashflow.order.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final DeliveryAddressMapper addressMapper;

    @Override
    public List<DeliveryAddress> getUserAddresses(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<DeliveryAddress>()
                .eq(DeliveryAddress::getUserId, userId).orderByDesc(DeliveryAddress::getIsDefault));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(DeliveryAddress address) {
        if (address.getIsDefault() == 1) clearDefault(address.getUserId());
        addressMapper.insert(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeliveryAddress address) {
        // 校验地址归属（防止 IDOR 越权）
        DeliveryAddress existing = addressMapper.selectById(address.getId());
        if (existing == null || !existing.getUserId().equals(address.getUserId())) {
            throw new com.flashflow.common.exception.BusinessException(
                    com.flashflow.common.domain.ErrorCode.FORBIDDEN, "无权修改此地址");
        }
        if (address.getIsDefault() == 1) clearDefault(address.getUserId());
        addressMapper.updateById(address);
    }

    @Override
    public void remove(Long id) {
        // 注意：此方法需由 Controller 层校验归属后再调用
        // Controller 层应先查询地址校验 userId == currentUserId
        addressMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long id, Long userId) {
        clearDefault(userId);
        DeliveryAddress a = new DeliveryAddress();
        a.setId(id); a.setIsDefault(1);
        addressMapper.updateById(a);
    }

    private void clearDefault(Long userId) {
        List<DeliveryAddress> list = addressMapper.selectList(
                new LambdaQueryWrapper<DeliveryAddress>()
                        .eq(DeliveryAddress::getUserId, userId)
                        .eq(DeliveryAddress::getIsDefault, 1));
        for (DeliveryAddress a : list) {
            a.setIsDefault(0);
            addressMapper.updateById(a);
        }
    }
}
