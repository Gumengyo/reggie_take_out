package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.constant.RedisConstant;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.AliyunSmsUtils;
import com.itheima.reggie.utils.NameCreator;
import com.itheima.reggie.utils.RedisUtil;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user,HttpSession session){
        // 获取手机号
        String phone = user.getPhone();

        if(redisUtil.getValue(phone) != null){
            return R.success(redisUtil.getValue(phone).toString());
        }

        if (StringUtils.isNotEmpty(phone)){
            // 生成随机的4为验证码
            String code = ValidateCodeUtils.generateValidateCode(6).toString();
            log.info("phone={}",phone);
            log.info("code={}",code);

            try {
                // 调用阿里云提供的短信服务API完成发送短信
//                AliyunSmsUtils.sendMessage("短信签名名称","短信模板CODE",phone,code);

                // 将生成的验证码缓存到
                redisUtil.setValue(phone,code, 5L,TimeUnit.MINUTES);

                return R.success(code);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return R.error("短信验证码发送失败");
    }

    /**
     * 移动端用户登录
     *
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {

        // 获取手机号
        String phone = map.get("phone").toString();

        // 获取验证码
        String code = map.get("code").toString();
        
        // 从Redis中获取缓存的验证码
        Object codeInSession = redisUtil.getValue(phone);

        // 进行验证码的比对（页面提交的验证码和Session中保存的验证码比对）
        if (codeInSession != null && codeInSession.equals(code)){
            // 如果能够比对成功，说明登录成功
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            if (user == null){
                // 判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setName(NameCreator.createName());
                user.setSex("0");
                userService.save(user);
            }
            session.setAttribute("user",user.getId());

            // 如果用户登录成功，删除Redis中缓存的验证码
            redisUtil.delete(phone);
            return R.success(user);
        }
        return R.error("登录失败");
    }

    /**
     * 移动端用户退出
     * @param session
     * @return
     */
    @PostMapping("/loginout")
    public R<String> loginout(HttpSession session){
        // 清理Session中保存的当前登录用户的id
        session.removeAttribute("user");
        return R.success("退出成功");
    }

    /**
     * 根据id获取用户信息
     * @return
     */
    @GetMapping
    public R<User> getUser(){
        Long userId = BaseContext.getCurrentId();
        User user = userService.getById(userId);
        return R.success(user);
    }

    @PutMapping
    public R<String> updateUser(@RequestBody User user){
        userService.updateById(user);
        redisUtil.sadd(RedisConstant.REGGIE_PIC_DB_RESOURCES,user.getAvatar());
        return R.success("修改资料成功");
    }
}
