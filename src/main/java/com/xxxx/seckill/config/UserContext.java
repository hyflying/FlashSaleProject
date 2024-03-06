package com.xxxx.seckill.config;

import com.xxxx.seckill.pojo.User;

/**
 * @author Evan
 * @date 2024/1/10 16:55
 */
public class UserContext {//ThreadLocal绑定当前线程的值,还有线程锁

    private static ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void setUser(User user){
        userHolder.set(user);
    }

    public static User getUser(){
        return userHolder.get();
    }
}
