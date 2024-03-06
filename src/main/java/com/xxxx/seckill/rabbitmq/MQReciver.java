package com.xxxx.seckill.rabbitmq;

import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Evan
 * @date 2024/1/10 14:45
 */
@Service
@Slf4j
public class MQReciver {//消费
//
//    @RabbitListener(queues = "queue")//监听名为queue的队列
//    public void receive(Object msg){
//        log.info("接收消息："+msg);//打印
//    }
//
//    @RabbitListener(queues = "queue_fanout01")
//    public void receive01(Object msg){
//        log.info("QUEUE01接收消息：" + msg);
//    }
//
//    @RabbitListener(queues = "queue_fanout02")
//    public void receive02(Object msg){
//        log.info("QUEUE02接收消息：" + msg);
//    }
//
//    @RabbitListener(queues = "queue_direct01")
//    public void receive03(Object msg){
//        log.info("QUEUE01接收消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_direct02")
//    public void receive04(Object msg){
//        log.info("QUEUE02接收消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_topic01")
//    public void receiver05(Object msg){
//        log.info("QUEUE01接收消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_topic02")
//    public void receiver06(Object msg){
//        log.info("QUEUE02接收消息："+msg);
//    }
//
//    @RabbitListener(queues = "queue_header01")
//    public void receiver07(Message message){
//        log.info("QUEUE01接收Message对象："+message);
//        log.info("QUEUE01接收消息："+new String(message.getBody()));
//    }
//
//    @RabbitListener(queues = "queue_header02")
//    public void receiver08(Message message){
//        log.info("QUEUE02接收Message对象："+message);
//        log.info("QUEUE02接收消息："+new String(message.getBody()));
//    }

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IOrderService orderService;

    //下单操作,通过rabbbitmq进行异步下单
    @RabbitListener(queues = "seckillQueue")
    public void receive(String message){
        log.info("接收的消息："+message);
        SeckillMessage seckillMessage = JsonUtil.jsonStr2Object(message, SeckillMessage.class);
        Long goodsId = seckillMessage.getGoodsId();
        User user = seckillMessage.getUser();
        //判断库存，用来rabbitmq这个库存减少和判断变为了异步的操作
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        if(goodsVo.getStockCount() < 1){
            return;//出现溢出直接返回
        }
        //判断是否重复抢购
        SeckillOrder seckillOrder =
                (SeckillOrder) redisTemplate.opsForValue().get("order:"+user.getId() + ":" + goodsId);
        if(seckillOrder != null){
            return;
        }
        //下单操作
        orderService.seckill(user, goodsVo);//在rabbitmq中下单
    }
}
