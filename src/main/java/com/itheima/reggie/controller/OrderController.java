package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private UserService userService;

    @Autowired
    ShoppingCartController shoppingCartController;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户下单
     *
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 后台订单信息分页查询
     * @param page
     * @param pageSize
     * @param number
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime,String endTime){

        log.info("beginTime：{}",beginTime);
        log.info("endTime：{}",endTime);

        // 创建分页构造器对象
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();

        // 条件构造器
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        wrapper.eq(number != null, Orders::getNumber,number);
        wrapper.ge(beginTime != null ,Orders::getOrderTime,beginTime);
        wrapper.le( endTime != null,Orders::getOrderTime,endTime);
        // 添加排序条件
        wrapper.orderByDesc(Orders::getOrderTime);
        // 执行分页查询
        orderService.page(pageInfo,wrapper);

        // 对象拷贝
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");

        List<Orders> records = pageInfo.getRecords();

        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();

            BeanUtils.copyProperties(item,ordersDto);

            User user = userService.getById(item.getUserId());

            ordersDto.setUserName(user.getName());

            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);

        return R.success(pageInfo);
    }

    /**
     * 后台订单信息分页查询
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(int page,int pageSize){

        // 创建分页构造器对象
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();

        // 条件构造器
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        wrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        // 添加排序条件
        wrapper.orderByDesc(Orders::getOrderTime);

        // 执行分页查询
        orderService.page(pageInfo,wrapper);

        // 对象拷贝
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");

        List<Orders> records = pageInfo.getRecords();

        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();

            BeanUtils.copyProperties(item,ordersDto);

            String number = item.getNumber(); // 订单号
            // 根据订单号查询订单明细对象
            LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(OrderDetail::getOrderId,number);

            List<OrderDetail> orderDetails = orderDetailService.list(queryWrapper);
            ordersDto.setOrderDetails(orderDetails);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);

        return R.success(ordersDtoPage);
    }

    @PutMapping
    public R<String> editOrderDetail(@RequestBody Orders orders){
        orderService.updateById(orders);
        if (orders.getStatus() == 4){
            // 清理所有菜品的缓存数据
            Set dishKeys = redisTemplate.keys("dish_*");
            redisTemplate.delete(dishKeys);

            // 清理所有套餐的缓存数据
            Set setmealKeys = redisTemplate.keys("setmeal_*");
            redisTemplate.delete(setmealKeys);
        }
        return R.success("修改订单状态成功");
    }

    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders){
        Long ordersId = orders.getId();
        Orders orderServiceById = orderService.getById(ordersId);
        String number = orderServiceById.getNumber();

        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId,number);

        List<OrderDetail> orderDetails = orderDetailService.list(wrapper);

        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart);
            shoppingCartController.add(shoppingCart);
        }

        return R.success("添加到购物车成功");
    }

    @DeleteMapping
    public R<String> deleteOrder(Long id){
        log.info("id：{}",id);
        Orders orders = orderService.getById(id);
        orderService.removeById(id);
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId,orders.getNumber());
        orderDetailService.remove(wrapper);
        return R.success("删除订单成功");
    }
}
