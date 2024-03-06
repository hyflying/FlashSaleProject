package com.xxxx.seckill.controller;


import com.xxxx.seckill.rabbitmq.MQSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.vo.RespBean;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Evan
 * @since 2024-01-08
 */
@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private MQSender mqSender;

//    //用户信息(测试)
//    @RequestMapping("info")
//    @ResponseBody
//    public RespBean info(User user){
//        return RespBean.success(user);
//    }
//
//    //测试发送RabbitMQ消息
//    @RequestMapping("mq")
//    @ResponseBody
//    public void mq(){
//        mqSender.send("Hello");
//    }
//
//    //fanout模式
//    @RequestMapping("/mq/fanout")
//    @ResponseBody
//    public void mq01(){
//        mqSender.send("Hello");
//    }
//
//    //direct模式
//    @RequestMapping("mq/direct01")
//    @ResponseBody
//    public void mq02(){
//        mqSender.send01("Hello,Red");
//    }
//    @RequestMapping("mq/direct02")
//    @ResponseBody
//    public void mq03(){
//        mqSender.send02("Hello,Green");
//    }
//
//    //topic模式
//    @RequestMapping("mq/topic01")
//    @ResponseBody
//    public void mq04(){
//        mqSender.send03("Hello,Red");
//    }
//    @RequestMapping("mq/topic02")
//    @ResponseBody
//    public void mq05(){
//        mqSender.send04("Hello,Green");
//    }
//
//    //Header模式
//    @RequestMapping("mq/header01")
//    @ResponseBody
//    public void mq06(){
//        mqSender.send05("Hello,Header01");
//    }
//    @RequestMapping("mq/header02")
//    @ResponseBody
//    public void mq07(){
//        mqSender.send06("Hello,Header02");
//    }
}
