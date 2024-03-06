package com.xxxx.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum RespBeanEnum {
    SUCCESS(200,"SUCCESS"),
    ERROR(500,"服务端异常"),
    LOGIN_ERROR(500210,"用户名或密码错误"),
    MOBILE_ERROR(50021,"手机号码错误"),
    BIND_ERROR(500212,"参数校验异常"),
    //秒杀模块5005
    EMPTY_STOCK(500500,"库存不足"),
    REPEAT_ERROR(500501,"该商品每人限购一件"),
    MOBILE_NOT_EX(500213,"手机号码不存在"),
    PASSWORD_UPDATE_FAIL(500214,"更新密码失败"),
    SESSION_ERROR(500215,"用户不存在"),
    ORDER_NOT_EXIST(500300,"订单信息不存在"),

    REQUEST_ILLEGAL(500211,"请求非法"),
    ERROR_CAPTCHA(500216,"验证码错误"),
    ACCESS_LIMIT_REACHED(500217,"访问过于频繁，请稍后再试"),
    ;
    private final Integer code;
    private final  String message;
}
