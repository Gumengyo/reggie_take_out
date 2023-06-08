package com.itheima.reggie;

import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.*;
import com.itheima.reggie.constant.RedisConstant;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.utils.AliyunSmsUtils;
import com.itheima.reggie.utils.NameCreator;
import com.itheima.reggie.utils.RedisUtil;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Set;

@SpringBootTest
@Slf4j
public class TestCase {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Test
    public void testRemove(){
        Long aLong = new Long("1397844263642378242");
        categoryService.remove(aLong);
    }

    @Test
    public void testName(){
        int len = 3;
        String ret = "";
        for (int i = 0; i < len; i++) {
            String str = null;
            int hightPos, lowPos; // 定义高低位
            Random random = new Random();
            hightPos = (176 + Math.abs(random.nextInt(39))); // 获取高位值
            lowPos = (161 + Math.abs(random.nextInt(93))); // 获取低位值
            byte[] b = new byte[2];
            b[0] = (new Integer(hightPos).byteValue());
            b[1] = (new Integer(lowPos).byteValue());
            try {
                str = new String(b, "GBK"); // 转成中文
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
            ret += str;
        }
        System.out.println(ret);
    }

    @Test
    public void testName2(){
        String name = NameCreator.createName();
        System.out.println(name);
    }

    @Test
    public void testSdiff(){
        Set<String> set = redisUtil.sdiff(RedisConstant.REGGIE_PIC_RESOURCES, RedisConstant.REGGIE_PIC_DB_RESOURCES);
        System.out.println(set);
    }

    @Test
    public void testGetSaleNum(){
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setDishId(new Long("1397851668262465537"));
        Integer saleNum = orderDetailService.getSaleNum(orderDetail);
        System.out.println(saleNum);
    }
}
