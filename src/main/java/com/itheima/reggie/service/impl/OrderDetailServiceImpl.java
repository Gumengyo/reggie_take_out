package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.mapper.OrderDetailMapper;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {

    @Autowired
    private OrderService orderService;

    @Override
    public Integer getSaleNum(OrderDetail orderDetail) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        Integer count = 0;
        if (orderDetail.getDishId() != null){
            wrapper.eq(OrderDetail::getDishId,orderDetail.getDishId());
        }else{
            wrapper.eq(OrderDetail::getSetmealId,orderDetail.getSetmealId());
        }

        List<OrderDetail> list = this.list(wrapper);
        for (OrderDetail detail : list) {
            Long orderId = detail.getOrderId();
            LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Orders::getNumber,orderId);
            Orders orders = orderService.getOne(queryWrapper);

            if (orders.getStatus() != 5){
                count += detail.getNumber();
            }

        }
        return count;
    }
}
