package com.xxxx.seckill.service.impl;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxxx.seckill.exception.GlobalException;
import com.xxxx.seckill.mapper.SeckillOrderMapper;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillGoods;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillGoodsService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.mapper.OrderMapper;
import com.xxxx.seckill.utils.MD5Util;
import com.xxxx.seckill.utils.UUIDUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.OrderDetailVo;
import com.xxxx.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

/**
 * @author liuyule
 * @description 针对表【t_order】的数据库操作Service实现
 * @createDate 2023-10-24 17:03:05
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService{

    @Autowired
    private ISeckillGoodsService seckillGoodsService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    //    @Autowired
//    private SeckillOrderMapper seckillOrderMapper;
    //秒杀
    @Transactional
    @Override
    public Order seckill(User user, GoodsVo goods) {
        //秒杀商品表减库存
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq
                ("goods_id",goods.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
//        seckillGoodsService.updateById(seckillGoods);//直接这样更新解决不了库存超卖的问题
        UpdateWrapper<SeckillGoods> updateWrapper = new UpdateWrapper();
//        updateWrapper.set("stock_count",
//                seckillGoods.getStockCount()).eq("id",seckillGoods.getId()).gt("stock_count",0);//这种写法可能会出问题
        //使用sql语句写
        updateWrapper.setSql("stock_count = stock_count-1").eq("goods_id",goods.getId()).gt("stock_count",0);
        boolean result = seckillGoodsService.update(updateWrapper);
        if(seckillGoods.getStockCount() < 1){
            redisTemplate.opsForValue().set("isStockEmpty:"+goods.getId(),"0");//添加进这个组件进入redis，标识库存没了
            return null;
        }
        /**总结：
         *  1.扣库存用的sql语句操作，并且要大于0的时候才操作
         *  2.在秒杀订单加了一个唯一索引，确保统一用户不重复读抢购
         *  3.从redis获取订单信息，而不是直接从数据库获取，而是先存入redis再从redis获取
         */

        //秒杀操作的时候需要生成订单，需要用户id和商品id，可以利用这个id作为索引来拦截多次购买
        //生成订单
        Order order = new Order();
        order.setUserId(user.getId());
        order.setGoodsId(goods.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);
        //生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(user.getId());
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goods.getId());
        seckillOrderService.save(seckillOrder);
//        seckillOrderMapper.insert(seckillOrder);
        redisTemplate.opsForValue().set("order:"+user.getId() + ":" + goods.getId(), seckillOrder);
        //设置redis中的键值对，如果不存在会创建
        return order;
    }

    @Override
    public OrderDetailVo detail(Long orderId) {
        if(orderId == null){
            throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
        }
        Order order = orderMapper.selectById(orderId);
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(order.getGoodsId());
        OrderDetailVo detail = new OrderDetailVo();
        detail.setOrder(order);
        detail.setGoodsVo(goodsVo);
        return detail;
    }

    //获取秒杀地址
    @Override
    public String createPath(User user, Long goodsId) {
        String str = MD5Util.md5(UUIDUtil.uuid() + "123456");//随机生成的地址一般存在redis短时间存储就够了
        redisTemplate.opsForValue().set("seckillPath:"+user.getId()+":"+goodsId,str,60, TimeUnit.SECONDS);
        return str;
    }
//
    //校验秒杀地址
    @Override
    public boolean checkPath(String path, User user, Long goodsId) {
        if(user == null || goodsId < 0 || StringUtils.isEmpty(path)){
            return false;
        }
        String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:"+user.getId()+":"+goodsId);
        return path.equals(redisPath);
    }
//
    //校验验证码
    @Override
    public boolean checkCaptcha(User user, Long goodsId, String captcha) {
        if(user == null || goodsId < 0 || StringUtils.isEmpty(captcha)){
            return false;
        }
        String redisCaptcha = (String) redisTemplate.opsForValue().get("captcha:"+user.getId()+":"+goodsId);
        return redisCaptcha.equals(captcha);
    }
}




