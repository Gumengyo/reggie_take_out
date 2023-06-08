package com.itheima.reggie.job;

import com.itheima.reggie.constant.RedisConstant;
import com.itheima.reggie.utils.QiniuUtils;
import com.itheima.reggie.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class ClearImgJob {

    @Autowired
    private RedisUtil redisUtil;

    @Scheduled(cron = "0 0 2 * * ?")
    public void clearImg(){

        log.info("开始清理缓存图片任务...");

        // 根据Redis中保存的两个set集合进行差值计算，获得垃圾图片名称集合
        Set<String> set = redisUtil.sdiff(RedisConstant.REGGIE_PIC_RESOURCES, RedisConstant.REGGIE_PIC_DB_RESOURCES);
        if (set != null){
            for (String picName : set) {
                // 删除七牛云服务器上的图片
                QiniuUtils.deleteFileFromQiniu(picName);
                redisUtil.srem(RedisConstant.REGGIE_PIC_RESOURCES,picName);
            }
        }

    }
}
