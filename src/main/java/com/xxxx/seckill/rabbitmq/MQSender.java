package com.xxxx.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * @author Evan
 * @date 2024/1/10 14:45
 */
@Service
@Slf4j
public class MQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;
//
    public void send(Object msg){
        log.info("发送消息：" + msg);
        rabbitTemplate.convertAndSend("fanoutExchange","",msg);
    }
//
//    public void send01(Object msg){//direct模式需要routing，通过路由key去匹配队列
//        log.info("发送red消息："+msg);
//        rabbitTemplate.convertAndSend("directExchange","queue.red",msg);//要和绑定的路由key一样才能正确到达
//    }
//
//    public void send02(Object msg){
//        log.info("发送green消息："+msg);
//        rabbitTemplate.convertAndSend("directExchange","queue.green",msg);//要和绑定的路由key一样才能正确到达
//    }
//
//    public void send03(Object msg){//具体发消息的时候路由要写详细的
//        log.info("发送消息(QUEUE01接收)："+msg);
//        rabbitTemplate.convertAndSend("topicExchange","queue.red.message",msg);
//    }
//
//    public void send04(Object msg){
//        log.info("发送消息(被两个queue接收)："+msg);//routingKey能够匹配上两个
//        rabbitTemplate.convertAndSend("topicExchange","message.queue.green.abc",msg);
//    }
//
//    public void send05(String msg){
//        log.info("发送消息(被两个queue接收)："+msg);
//        MessageProperties messageProperties = new MessageProperties();
//        messageProperties.setHeader("color","red");
//        messageProperties.setHeader("speed","fast");
//        Message message = new Message(msg.getBytes(),messageProperties);
//        rabbitTemplate.convertAndSend("headersExchange","",message);
//    }
//
//    public void send06(String msg){
//        log.info("发送消息(被QUEUE01接收)："+msg);
//        MessageProperties properties = new MessageProperties();
//        properties.setHeader("color","red");
//        properties.setHeader("speed","normal");
//        Message message = new Message(msg.getBytes(),properties);
//        rabbitTemplate.convertAndSend("headersExchange","",message);
//    }

    //发送秒杀信息
    public void sendSeckillMessage(String message){
        log.info("发送消息："+message);
        rabbitTemplate.convertAndSend("seckillExchange","seckill.message",message);
    }
}
