package com.atguigu.service.impl;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.*;
import com.atguigu.feign.OrderFeignClient;
import com.atguigu.feign.UserFeignClient;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.HttpClientUtil;
import com.atguigu.util.MD5;
import com.atguigu.util.UserIdUtil;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-08
 */
@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillProductService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private OrderFeignClient orderFeignClient;

    //创建一级缓存
    Map<Long, SeckillProduct> cacheMap = new ConcurrentHashMap<>();

    @Override
    public List<SeckillProduct> queryAllSeckill() {
        //一级缓存有值
        if (cacheMap.size() > 0) {
            return cacheMap.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .sorted(Comparator.comparing(SeckillProduct::getStartTime)).collect(Collectors.toList());
        }

        //缓存没值
        //从redis中查询
        Boolean flag = redisTemplate.hasKey(RedisConst.SECKILL_PRODUCT);
        if (flag) {
            List<SeckillProduct> seckillProductList = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).values();

            //按开始时间排序
//        seckillProductList.stream().sorted((o1,o2)-> DateUtil.truncatedCompareTo(o1.getStartTime(),o2.getStartTime(), Calendar.SECOND)).collect(Collectors.toList());
            seckillProductList = seckillProductList.stream().sorted(Comparator.comparing(SeckillProduct::getStartTime)).collect(Collectors.toList());
            //存入缓存
            for (SeckillProduct seckillProduct : seckillProductList) {
                cacheMap.put(seckillProduct.getSkuId(), seckillProduct);
            }

            return seckillProductList;
        }
        return null;
    }

    @Override
    public SeckillProduct querySecKillBySkuId(Long skuId) {
        //查看一级缓存
        if (cacheMap.containsKey(skuId))
            return cacheMap.get(skuId);

        //查看缓存
        Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).hasKey(skuId.toString());
        if (flag) {
            return (SeckillProduct) redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).get(skuId.toString());
        }
        return null;
    }

    //生成抢购码
    @Override
    public RetVal generateSeckillCode(Long skuId, HttpServletRequest request) {
        String userId = UserIdUtil.getUserId(request, redisTemplate);
        if (StringUtils.isEmpty(userId))
            return RetVal.fail("请您登录后重试");

        //从redis中获取商品，判断此商品是否已开始秒杀
        BoundHashOperations seckillHashOps = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT);
        if (seckillHashOps.hasKey(skuId.toString())) {
            SeckillProduct seckillProduct = (SeckillProduct) seckillHashOps.get(skuId.toString());
            if (DateUtil.dateCompare(seckillProduct.getStartTime(), new Date())
                    && DateUtil.dateCompare(new Date(), seckillProduct.getEndTime())) {
                //生成抢购码
                String secKillCode = MD5.encrypt(userId);
                return RetVal.ok(secKillCode);
            } else {
                return RetVal.fail("当前不处于秒杀时间");
            }
        }
        return RetVal.fail("该商品未参与秒杀活动");
    }

    //判断是否可以预下单
    @Override
    public RetVal prepareSeckill(Long skuId, String seckillCode, HttpServletRequest request) {
        //判断是否具有抢购资格
        String userId = UserIdUtil.getUserId(request, redisTemplate);
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(seckillCode))
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);

        String encrypt = MD5.encrypt(userId);
        if (!seckillCode.equals(encrypt))
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);

        //判断商品秒杀状态位
        String state = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId);
        //判断是否存在秒杀状态位
        if (StringUtils.isEmpty(state))
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);

        if (!RedisConst.CAN_SECKILL.equals(state)) {
            //通知其他redis，修改商品秒杀状态位
            redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL, skuId + ":" + RedisConst.CAN_NOT_SECKILL);
            return RetVal.build(null, RetValCodeEnum.SECKILL_END);
        }

        //有此商品，且可以秒杀    mq预下单
        mqPrepareSeckill(skuId, userId);

        return RetVal.ok();
    }

    //预下单
    @Override
    public void prepareSeckillOrder(UserSeckillSkuInfo userSeckillSkuInfo) {
        String userId = userSeckillSkuInfo.getUserId();
        Long skuId = userSeckillSkuInfo.getSkuId();

        //校验秒杀状态位
        String state = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId);
        //秒杀状态不对
        if (!RedisConst.CAN_SECKILL.equals(state))
            return;


        //尝试存储userId和skuId的对应关系
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(RedisConst.PREPARE_SECKILL_USERID_SKUID + ":" + userId + ":" + skuId, skuId.toString(),
                RedisConst.PREPARE_SECKILL_LOCK_TIME, TimeUnit.SECONDS);
        //验证是否预下过单
        if (!flag)
            return;

        //判断库存量，尝试减库存
        String redisStockSkuId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(redisStockSkuId))
            return;

        //存储userId和商品详情对应关系
        PrepareSeckillOrder prepareSeckillOrder = new PrepareSeckillOrder();
        prepareSeckillOrder.setUserId(userId);
        prepareSeckillOrder.setBuyNum(1);
        SeckillProduct seckillProduct = querySecKillBySkuId(skuId);
        prepareSeckillOrder.setSeckillProduct(seckillProduct);

        prepareSeckillOrder.setPrepareOrderCode(MD5.encrypt(userId + skuId));

        //存入redis
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).put(userId, prepareSeckillOrder);

        //更新秒杀库存量
        updateSecKillStockCount(skuId);
    }

    //判断是否具备抢购资格
    @Override
    public RetVal hasQualified(Long skuId, HttpServletRequest request) {
        String userId = UserIdUtil.getUserId(request, redisTemplate);

        //查看是否存在skuId和userId对应关系
        Boolean flag = redisTemplate.hasKey(RedisConst.PREPARE_SECKILL_USERID_SKUID + ":" + userId + ":" + skuId);
        if (flag) {
            //拿出订单
            PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
            if (prepareSeckillOrder != null) {
                return RetVal.build(prepareSeckillOrder, RetValCodeEnum.PREPARE_SECKILL_SUCCESS);
            }
        }
        //判断是否真正下过单
        Integer orderId = (Integer) redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).get(userId);
        //用户已经购买过此商品
        if (orderId != null)
            return RetVal.build(orderId, RetValCodeEnum.PREPARE_SECKILL_SUCCESS);

        //不具备抢购资格，继续排队
        //通知尝试重新下单
        // mq尝试预下单
        mqPrepareSeckill(skuId, userId);
        return RetVal.build(null, RetValCodeEnum.SECKILL_RUN);
    }

    @Override
    public RetVal confirmSeckill(HttpServletRequest request) {
        String userId = UserIdUtil.getUserId(request, redisTemplate);
        if (userId == null)
            userId = AuthContextHolder.getUserId(request);

        //获取收获人地址
        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);
        //从redis中获取订单信息，转换为订单
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder == null)
            return RetVal.fail().message("非法请求");

        SeckillProduct seckillProduct = prepareSeckillOrder.getSeckillProduct();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillProduct.getSkuId());
        orderDetail.setSkuName(seckillProduct.getSkuName());
        orderDetail.setImgUrl(seckillProduct.getSkuDefaultImg());
        orderDetail.setSkuNum(prepareSeckillOrder.getBuyNum() + "");
        //订单的价格 可以拿实时价格 也可以拿购物车里面的价格
        orderDetail.setOrderPrice(seckillProduct.getCostPrice());

        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        orderDetailList.add(orderDetail);

        //将收货人地址，订单列表，花费价格放入map，使用retval返回
        Map<String, Object> map = new HashMap<>();
        map.put("userAddressList", userAddressList);
        map.put("orderDetailList", orderDetailList);
        map.put("totalNum", prepareSeckillOrder.getBuyNum());
        map.put("totalMoney", seckillProduct.getCostPrice());

        return RetVal.ok(map);
    }

    @Override
    public RetVal submitSecKillOrder(OrderInfo orderInfo, HttpServletRequest request) {
        String userId = UserIdUtil.getUserId(request, redisTemplate);
        if (userId == null)
            userId = AuthContextHolder.getUserId(request);

        //尝试获取redis中的临时订单信息，来判断用户是否真正具有下单的资格
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder == null)
            return RetVal.fail().message("非法请求");

        orderInfo.setUserId(Long.valueOf(userId));
        //通过openfeign远程下单，返回下单的orderId
        Long orderId = orderFeignClient.saveOrderAndDetail(orderInfo);
        if(orderId == null)
            return RetVal.fail().message("下单失败");

        //删除存入redis中的临时订单信息
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).delete(userId);

        //将用户id和订单id的关系保存进redis中，以做用户点击秒杀商品按钮时，判断用户是否已经购买过此商品
        redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).put(userId,orderId);

        return RetVal.ok(orderId);
    }

    //mq预下单
    private void mqPrepareSeckill(Long skuId, String userId) {
        UserSeckillSkuInfo userSeckillSkuInfo = new UserSeckillSkuInfo();
        userSeckillSkuInfo.setUserId(userId);
        userSeckillSkuInfo.setSkuId(skuId);
        rabbitTemplate.convertAndSend(MqConst.PREPARE_SECKILL_EXCHANGE, MqConst.PREPARE_SECKILL_ROUTE_KEY, userSeckillSkuInfo);
    }

    private void updateSecKillStockCount(Long skuId) {
        //取出list中遗留的商品数量
        Long residue = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //取出总商品个数
        SeckillProduct seckillProduct = querySecKillBySkuId(skuId);
        Integer totalNum = seckillProduct.getNum();

        seckillProduct.setStockCount(totalNum - Integer.valueOf(residue + ""));

        //更新到redis中
        redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId.toString(), seckillProduct);

        //每次一定数量的商品被消费后，持久化到DB中
        if (residue % 2 == 0)
            updateById(seckillProduct);
    }


}
