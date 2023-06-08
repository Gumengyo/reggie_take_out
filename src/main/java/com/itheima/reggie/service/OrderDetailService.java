package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.OrderDetail;

import java.util.Map;

public interface OrderDetailService extends IService<OrderDetail> {
    Integer getSaleNum(OrderDetail orderDetail);
}
