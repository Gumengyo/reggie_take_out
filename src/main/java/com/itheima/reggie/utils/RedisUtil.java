package com.itheima.reggie.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取值
     *
     * @param key
     * @return
     */
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设值
     *
     * @param key
     * @param value
     */
    public void setValue(Object key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设值
     * <p>
     * 可以设置失效时间
     *
     * @param key
     * @param value
     */
    public void setValue(Object key, Object value, Long timeout,TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key,value,timeout,timeUnit);
    }

    /**
     * 删除数据
     *
     * @param key
     */
    public void delete(Object key) {
        redisTemplate.delete(key);
    }

    /**
     * key是否存在
     *
     * @param key
     * @return
     */
    public boolean hasKey(Object key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 在set集合设置值
     * @param key
     * @param member
     */
    public void sadd(Object key,Object member){
        redisTemplate.opsForSet().add(key,member);
    }

    /**
     * 在set集合删除值
     * @param key
     * @param member
     */
    public void srem(Object key,Object member){
        redisTemplate.opsForSet().remove(key,member);
    }

    /**
     * 返回两个集合差值
     * @param key1
     * @param key2
     * @return
     */
    public Set<String> sdiff(String key1, String key2){
        return redisTemplate.opsForSet().difference(key1,key2);
    }

}
