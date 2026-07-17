package com.flashflow.auth.service;

import com.flashflow.auth.entity.UserAddress;

import java.util.List;

/**
 * 用户地址服务
 */
public interface AddressService {

    List<UserAddress> listByUserId(Long userId);

    UserAddress getDefault(Long userId);

    UserAddress getById(Long id);

    void create(UserAddress address);

    void update(UserAddress address);

    void delete(Long id, Long userId);

    void setDefault(Long id, Long userId);
}
