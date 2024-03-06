package com.xxxx.seckill.config;//package com.xxxx.seckill.config;
//
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.FanoutExchange;
//import org.springframework.amqp.core.Queue;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
///**
// *  让所有的消息都要经过队列
// *
// * @author Evan
// * @date 2024/1/10 14:45
// */
//@Configuration
//public class RabbitMqConfig {
//
//    private static final String QUEUE01 = "queue_fanout01";
//    private static final String QUEUE02 = "queue_fanout02";
//    private static final String EXCHANGE = "fanoutExchange";
//
//    @Bean
//    public Queue queue(){//amqp.core.Queue,rabbitmq的queue
//        return new Queue("queue",true);
//    }
//
//    @Bean
//    public Queue queue01(){
//        return new Queue(QUEUE01);
//    }
//
//    @Bean
//    public Queue queue02(){
//        return new Queue(QUEUE02);
//    }
//
//    @Bean
//    public FanoutExchange fanoutExchange(){//交换机
//        return new FanoutExchange(EXCHANGE);
//    }
//
//    @Bean
//    public Binding binding01(){//将队列绑定到交换机
//        return BindingBuilder.bind(queue01()).to(fanoutExchange());
//    }
//
//    @Bean
//    public Binding binding02(){//绑定
//        return BindingBuilder.bind(queue02()).to(fanoutExchange());
//    }
//}
