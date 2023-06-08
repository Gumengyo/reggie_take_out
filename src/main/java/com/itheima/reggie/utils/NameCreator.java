package com.itheima.reggie.utils;

import java.util.Random;

public class NameCreator {
    public static String createName(){
        String line = "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦" +
                "章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞仁袁柳酆鲍史唐费廉岑薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅" +
                "皮卞齐康伍余元卜顾孟平黄和穆萧尹姚邵湛汪祁毛禹狄米贝明臧计伏成戴谈宋茅庞熊纪舒屈项祝董梁杜" +
                "阮蓝闵席季麻强贾路娄危江童颜郭梅盛林刁钟徐邱骆高夏蔡田樊胡";
        Random random = new Random();
        String name = line.charAt(random.nextInt(line.length()))+"";
        for(int i= 1+random.nextInt(2);i>0;i--){
            name+=line.charAt(random.nextInt(line.length()))+"";
        }
        return name;
    }
}
