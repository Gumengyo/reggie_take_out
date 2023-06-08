package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.OrderDetailService;
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

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DishService dishService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        dishService.saveWithFlavor(dishDto);

        // 清理某个分类下的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1" ;
        redisUtil.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){

        // 创建分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        // 条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        // 添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        // 执行分页查询
        dishService.page(pageInfo,queryWrapper);

        // 对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();

        List<DishDto> list = records.stream().map((item) ->{
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId(); //分类id
            // 根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     *
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {

        dishService.updateWithFlavor(dishDto);

        // 清理所有菜品的缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        // 清理某个分类下的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1" ;
        redisUtil.delete(key);

        return R.success("修改菜品成功");
    }

    /**
     * 根据id修改售卖状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateDishStatus(@PathVariable int status,Long[] ids){

        Dish dish = new Dish();
        // 根据传入值，设置售卖状态
        dish.setStatus(status);

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Dish::getId,ids);
        dishService.update(dish,wrapper);

        if (ids.length > 1){
            // 清理所有菜品的缓存数据
            Set keys = redisTemplate.keys("dish_*");
            redisTemplate.delete(keys);
        }else {
            Dish serviceById = dishService.getById(ids[0]);
            // 清理某个分类下的菜品缓存数据
            String key = "dish_" + serviceById.getCategoryId() + "_1" ;
            redisUtil.delete(key);
        }

        return R.success("修改菜品售卖状态成功");
    }

    /**
     * 根据id删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        if (ids.size() > 1){
            // 清理所有菜品的缓存数据
            Set keys = redisTemplate.keys("dish_*");
            redisTemplate.delete(keys);
        } else {
            Dish dish = dishService.getById(ids.get(0));
            // 清理某个分类下的菜品缓存数据
            String key = "dish_" + dish.getCategoryId() + "_1" ;
            redisUtil.delete(key);
        }
        dishService.deleteByIds(ids);

        return R.success("删除菜品数据成功");
    }

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
/*    @GetMapping("/list")
    public R<List<Dish>> list(Dish dish) {

        Long categoryId = dish.getCategoryId();

        // 构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(categoryId != null, Dish::getCategoryId, categoryId);
        // 添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);
        // 添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {

        String key = "dish_" + dish.getCategoryId() + "_"+ dish.getStatus();

        //  先从redis中获取缓存数据
        List<DishDto> dishDtoList = null;

        dishDtoList = (List<DishDto>) redisUtil.getValue(key);

        if (dishDtoList != null) {
            // 如果存在，直接返回，无需查询数据库
            return R.success(dishDtoList);
        }

        Long categoryId = dish.getCategoryId();

        // 构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(categoryId != null, Dish::getCategoryId, categoryId);
        // 添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);
        // 添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map((item) ->{

            // 当前菜品的id
            Long dishId = item.getId();
            DishDto dishDto = dishService.getByIdWithFlavor(dishId);

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setDishId(item.getId());
            Integer saleNum = orderDetailService.getSaleNum(orderDetail);
            dishDto.setSaleNum(saleNum);

            return dishDto;
        }).collect(Collectors.toList());

        // 如果不存在，需要查询数据库，将查询到的菜品数据缓存到Redis
        redisUtil.setValue(key,dishDtoList,60L, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }
}
