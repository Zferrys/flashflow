package com.flashflow.order.service;
import com.flashflow.order.entity.DeliveryAddress;
import java.util.List;
public interface AddressService {
    List<DeliveryAddress> getUserAddresses(Long userId);
    void save(DeliveryAddress address);
    void update(DeliveryAddress address);
    void remove(Long id);
    void setDefault(Long id, Long userId);
}
