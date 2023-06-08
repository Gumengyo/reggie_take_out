package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import com.itheima.reggie.constant.RedisConstant;
import com.itheima.reggie.utils.QiniuUtils;
import com.itheima.reggie.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传和下载
 */
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 图片上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        // file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会删除

        // 原始文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 使用UUID重新生成文件名，防止文件名称重复造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        try {
            // 将文件上传到七牛云服务器
            QiniuUtils.upload2Qiniu(file.getBytes(),fileName);
            redisUtil.sadd(RedisConstant.REGGIE_PIC_RESOURCES,fileName);
            return R.success(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return R.error("图片上传失败");
        }
    }
}
