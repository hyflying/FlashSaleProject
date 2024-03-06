package com.xxxx.seckill.config;//package com.xxxx.seckill.config;
//
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.HeadersExchange;
//import org.springframework.amqp.core.Queue;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.logging.Handler;
//
///**
// * @author Evan
// * @date 2024/1/10 14:45
// */
//@Configuration
//public class RabbitMQHeadersConfig {
//
//    private static final String QUEUE01="queue_header01";
//    private static final String QUEUE02="queue_header02";
//    private static final String EXCHANGE="headersExchange";
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
//    public HeadersExchange headersExchange(){
//        return new HeadersExchange(EXCHANGE);
//    }
//
//    @Bean
//    public Binding binding01(){//绑定
//        Map<String,Object> map = new HashMap<>();
//        map.put("color","red");
//        map.put("speed","low");
//        return BindingBuilder.bind(queue01()).to(headersExchange()).whereAny(map).match();//color和speed的value只要匹配上一个就转发
//        //whereAll匹配多个键值对都要满足
//        //whereAny匹配任意一个键值对满足即可
//    }
//
//    @Bean
//    public Binding binding02(){
//        Map<String,Object> map = new HashMap<>();
//        map.put("color","red");
//        map.put("speed","fast");
//        return BindingBuilder.bind(queue02()).to(headersExchange()).whereAll(map).match();//color和speed要同时匹配上才转发
//    }
//}
