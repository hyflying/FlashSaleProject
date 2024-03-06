package com.xxxx.seckill.mapper;

import com.xxxx.seckill.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
* @author Evan
* @description 针对表【t_user】的数据库操作Mapper
* @createDate 2023-10-21 18:34:30
* @Entity com.xxxx.seckill.pojo.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




