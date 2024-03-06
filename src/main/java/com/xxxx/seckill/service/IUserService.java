package com.xxxx.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.vo.LoginVo;
import com.xxxx.seckill.vo.RespBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Evan
 * @since 2024-01-08
 */
public interface IUserService extends IService<User> {
    RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);

    User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response);

    //更新密码
    //更新redis数据，使其和数据库里数据一样
    //方法：对数据库的任何操作都清空redis的数据，需要调用到redis的时候(登录时)会获取到最新的用户信息
    //通过ticket获得用户
    RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response);
}
