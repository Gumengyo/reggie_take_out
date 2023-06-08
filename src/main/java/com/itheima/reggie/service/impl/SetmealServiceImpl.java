package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.constant.RedisConstant;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import com.itheima.reggie.utils.RedisUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     *
     * @param setmealDto
     */
    @Transactional
    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        // 保存套餐的基本信息，操作setmeal，执行insert操作
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealDto.getId());
        }

        // 保存套餐和菜品的关联信息，操作setmeal_dish，执行insert操作
        setmealDishService.saveBatch(setmealDishes);
        redisUtil.sadd(RedisConstant.REGGIE_PIC_DB_RESOURCES,setmealDto.getImage());
    }

    /**
     * 删除套餐，同时需要删除套餐和菜品的关联数据
     * @param ids
     */
    @Override
    public void deleteByIds(List<Long> ids) {
        // 查询套餐状态，确定是否可以删除
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);

        int count = this.count(queryWrapper);

        // 如果不能删除，抛出一个业务异常
        if (count > 0){
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        // 删除在redis中存储的图片
        List<Setmeal> setmeals = this.listByIds(ids);
        for (Setmeal setmeal : setmeals) {
            redisUtil.srem(RedisConstant.REGGIE_PIC_DB_RESOURCES,setmeal.getImage());
        }

        // 如果可以删除，先删除套餐表中的数据
        this.removeByIds(ids);

        // 删除关系表中的数据
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(lambdaQueryWrapper);
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        // 查询套餐基本信息，从setmeal表查询
        Setmeal setmeal = this.getById(id);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);

        // 查询当前套餐对应的菜品信息，从setmeal_dish表查询
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        List<SetmealDish> flavors = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(flavors);

        return setmealDto;
    }

    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        // 更新setmeal表基本信息
        this.updateById(setmealDto);

        // 清理当前套餐对应菜品数据——setmeal_dish表的delete操作
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        // 添加当前提交过来的菜品数据——setmeal_dish表的insert操作
        Long setmealId = setmealDto.getId();

        // 套餐下菜品
        List<SetmealDish> dishes = setmealDto.getSetmealDishes();
        for (SetmealDish dish : dishes) {
            dish.setSetmealId(setmealId);
        }

        // 保存菜品数据到套餐菜品表setmeal_dish
        setmealDishService.saveBatch(dishes);
        redisUtil.sadd(RedisConstant.REGGIE_PIC_DB_RESOURCES,setmealDto.getImage());
    }

    @Override
    public List<DishDto> getSetmealDish(Long id) {
        LambdaQueryWrapper<SetmealDish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SetmealDish::getSetmealId,id);
        List<SetmealDish> list = setmealDishService.list(wrapper);
        List<DishDto> dishDtos = list.stream().map((item) ->{
            DishDto dishDto = new DishDto();
            Dish dish = dishService.getById(item.getDishId());
            BeanUtils.copyProperties(dish,dishDto);
            dishDto.setCopies(item.getCopies());
            return dishDto;
        }).collect(Collectors.toList());

        return dishDtos;
    }
}
