package com.xxxx.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wf.captcha.ArithmeticCaptcha;
import com.xxxx.seckill.config.AccessLimit;
import com.xxxx.seckill.exception.GlobalException;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.ui.Model;
import com.xxxx.seckill.pojo.User;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequestMapping("/seckill")
public class SecKillController implements InitializingBean {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private RedisScript<Long> script;//redis用于加载lua脚本，通过lua脚本可以实现分布式锁

    private Map<Long, Boolean> EmptyStockMap = new HashMap<>();//long对应id，boolean对应是否卖完，当卖完后不对redis访问了

    @RequestMapping("/doSecKill2")
    public String doSecKill2(Model model, User user, Long goodsId) {
        if (user == null) {
            return "/login";
        }
        model.addAttribute("user", user);
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goods.getStockCount() < 1) {
            model.addAttribute("errmg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));
        if (seckillOrder != null) {
            model.addAttribute("errmg", RespBeanEnum.REPEAT_ERROR.getMessage());
            return "secKillFail";
        }
        Order order = orderService.seckill(user, goods);
        model.addAttribute("order", order);
        model.addAttribute("goods", goods);
        return "orderDetail";
    }

    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody//返回的RespBean
    //user是通过实现HandlerMethodArgumentResolver中的resolveArgument返回
    //goodId是前端传过来的
    public RespBean doSeckill(@PathVariable String path, User user, Long goodsId) {//Model对象是一种用于在控制器(Controller)和视图(View)之间传递数据的容器
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);//回到登录页面,控制器（Controller）负责处理请求并返回相应的响应,控制器方法通常返回一个字符串，这个字符串代表要渲染的视图名称。
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        boolean check = orderService.checkPath(path, user,goodsId);//使用@PathVariable避免明文传输
        if(!check){
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }

        //判断是否重复抢购
        SeckillOrder seckillOrder =
                (SeckillOrder) redisTemplate.opsForValue().get("order:"+user.getId() + ":" + goodsId);
        if(seckillOrder != null){
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
        }
        if(EmptyStockMap.get(goodsId)){//通过内存标记来减少对redis访问
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //预减库存,使用分布式锁保证数据安全，避免多个进程或服务器同时操作数据库
//        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);//递减，会减到-1
        Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:"+goodsId),
                Collections.EMPTY_LIST);//从redis的获取的时候也有锁的存在
        //lua脚本在>0的时候才会减库存，这样就不会出现超卖的情况，这边为什么还要判断stock<0呢
        if(stock < 0){
            EmptyStockMap.put(goodsId,true);//卖完
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        SeckillMessage message = new SeckillMessage(user,goodsId);//通过rabbitmq下单，send传输数据，receiver消费(下单)
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(message));//rabbitmq将redis的信息同步到mysql中
        return RespBean.success(0);//要解决当大量请求来了，但是redis库存早就没了，此时还是会和redis通信，要解决这个问题，用map
        //在此处快速返回请求

//        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
//        //判断库存
//        if (goods.getStockCount() < 1) {
//            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
//        }
//        //判断是否重复抢购
//        //SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id",user.getId()).eq("goods_id",goodsId));
//        //通过redis去获取索引判断是否重复抢购以及超卖
//        //减少数据库访问，库存判断时，将商品库存加载到了redis，先用redis预减库存就不会走到数据库里，如果redis预减库存已经没了就直接返回
//        //如果库存足就生成订单，如果大量请求可以先异步，然后再通过消息队列慢慢处理
//        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goods.getId());
//        if (seckillOrder != null) {
//            return RespBean.error(RespBeanEnum.REPEAT_ERROR);
//        }
//        Order order = orderService.seckill(user, goods);
//        return RespBean.success(order);//为什么此处只能返回orderDetail，返回其他的html不行
    }


    @RequestMapping(value = "result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Long orderId = seckillOrderService.getResult(user, goodsId);
        return RespBean.success(orderId);
    }
    @AccessLimit(second=5,maxCount=5,needLogin=true)//在拦截器里面通过request和response获取到了ticket和其对应的user
    @RequestMapping(value = "path",method = RequestMethod.GET)
    @ResponseBody
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        //存入用户一分钟内访问次数,限制访问次数，需要获取请求。现在用的最简单的计数算法   可以用漏桶算法或者令牌桶算法来处理请求
        //漏桶算法，用一个桶(队列)去装请求，然后一边处理请求，但是这个可能会由于大量的请求一下把桶撑爆就无法接收后续请求
        //令牌桶算法：一个桶里存有令牌，一边不断生成令牌，另一边请求拿到了令牌就去执行后续操作，如果没有拿到可以丢弃或者存入缓存
        //限制访问次数，但是如果每个接口都要限流，每个接口都要写，很麻烦。可以用拦截器进行优化,
//
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        String uri = request.getRequestURI();
//        Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
//        if(count == null){
//            valueOperations.set(uri + ":" + user.getId(),1,5,TimeUnit.SECONDS);
//        }else if(count < 5){
//            valueOperations.increment(uri + ":" + user.getId());
//        }else{
//            return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
//        }

        //校验验证码正确性
        boolean check = orderService.checkCaptcha(user,goodsId,captcha);
        if(!check){
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }
        String str = orderService.createPath(user,goodsId);
        return RespBean.success(str);
    }
    @RequestMapping(value = "captcha",method = RequestMethod.GET)//用流的方式输出,不用指定输出
    public void verifyCode(User user, Long goodsId, HttpServletResponse response) {//通过response输出验证码
        if(user == null || goodsId < 0){
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        //设置请求头为输出图片的类型
        response.setContentType("image/jpg");
        response.setHeader("Pargam","No-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0);
        //生成验证码，将结果放入redis，设置一个失效时间
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32 );
        redisTemplate.opsForValue().set("captcha:"+user.getId()+":"+goodsId,captcha.text(),300, TimeUnit.SECONDS);
        try {
            log.info("验证码：{}",captcha.text());
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            log.error("验证码生成失败",e.getMessage());
        }
    }

    /**
//     * 系统初始化，把商品库存数量加载到redis
//     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if(CollectionUtils.isEmpty(list)){
            return;
        }
        //提前将mysql的商品数量写入到redis，好在redis中预减库存
        list.forEach(goodsVo -> {
            redisTemplate.opsForValue().set("seckillGoods:"+goodsVo.getId(),goodsVo.getStockCount());
            EmptyStockMap.put(goodsVo.getId(),false);
        });
    }

}
