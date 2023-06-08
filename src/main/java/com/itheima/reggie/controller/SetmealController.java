package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.SetmealService;
import com.itheima.reggie.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){

//        log.info("套餐信息：{}",setmealDto);
        setmealService.saveWithDish(setmealDto);

        // 清理某个分类下的套餐缓存数据
        String key = "setmeal_" + setmealDto.getCategoryId() + "_1";
        redisUtil.delete(key);

        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        // 分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件，根据name进行like模糊查询
        queryWrapper.like(name != null, Setmeal::getName, name);
        // 添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(pageInfo,queryWrapper);

        // 对象拷贝
        BeanUtils.copyProperties(pageInfo,dtoPage,"records");
        List<Setmeal> records = pageInfo.getRecords();

        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            // 对象拷贝
            BeanUtils.copyProperties(item,setmealDto);
            // 分类id
            Long categoryId = item.getCategoryId();
            // 根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                // 分类名称
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);
        return  R.success(dtoPage);
    }

    /**
     * 删除套餐
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        if (ids.size() > 1){
            // 清理所有菜品的缓存数据
            Set keys = redisTemplate.keys("setmeal_*");
            redisTemplate.delete(keys);
        }else {
            Setmeal setmeal = setmealService.getById(ids.get(0));
            // 清理某个分类下的套餐缓存数据
            String key = "setmeal_" + setmeal.getCategoryId() + "_1";
            redisUtil.delete(key);
        }

        setmealService.deleteByIds(ids);
        return R.success("套餐数据删除成功");
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id){
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    @GetMapping("/dish/{id}")
    public R<List<DishDto>> getSetmealDish(@PathVariable Long id){
        List<DishDto> list = setmealService.getSetmealDish(id);
        return R.success(list);
    }

    /**
     * 修改套餐
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithDish(setmealDto);

        // 清理某个分类下的套餐缓存数据
        String key = "setmeal_" + setmealDto.getCategoryId() + "_1";
        redisUtil.delete(key);

        return R.success("修改菜品成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateSetmealStatus(@PathVariable int status, Long[] ids) {
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);

        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Setmeal::getId,ids);
        setmealService.update(setmeal,wrapper);

        if (ids.length > 1){
            // 清理所有菜品的缓存数据
            Set keys = redisTemplate.keys("setmeal_*");
            redisTemplate.delete(keys);
        }else {
            Setmeal serviceById = setmealService.getById(ids[0]);
            // 清理某个分类下的套餐缓存数据
            String key = "setmeal_" + serviceById.getCategoryId() + "_1";
            redisUtil.delete(key);
        }


        return R.success("修改套餐售卖状态成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<SetmealDto>> list(Setmeal setmeal){

        String key = "setmeal_" + setmeal.getCategoryId() + "_1";

        // 先从redis中获取缓存数据
        List<SetmealDto> setmealDtos = null;

        setmealDtos = (List<SetmealDto>) redisUtil.getValue(key);

        if (setmealDtos != null){
            // 如果存在，直接返回，无需查询数据库
            return R.success(setmealDtos);
        }

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId()  );
        queryWrapper.eq(Setmeal::getStatus,1);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);
        setmealDtos = list.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSetmealId(item.getId());
            Integer saleNum = orderDetailService.getSaleNum(orderDetail);
            setmealDto.setSaleNum(saleNum);
            return setmealDto;
        }).collect(Collectors.toList());

        // 如果不存在，需要查询数据库，将查询到的套餐数据缓存到Redis
        redisUtil.setValue(key,setmealDtos,60L, TimeUnit.MINUTES);

        return R.success(setmealDtos);
    }
}
