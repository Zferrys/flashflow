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
        if (address.getIsDefault() == 1) clearDefault(address.getUserId());
        addressMapper.updateById(address);
    }

    @Override
    public void remove(Long id) { addressMapper.deleteById(id); }

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
