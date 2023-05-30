package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.CartInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-30
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;

    //判断是否第一次过来，第一次，新增，否则，更新
    @Override
    public void addCart(Long skuId, Integer skuNum, String oneOfUserId) {
        BoundHashOperations hashOps = getRedisHashOps(oneOfUserId);

        //第一次
        if (!hashOps.hasKey(skuId.toString())) {
            //新增
            CartInfo cartInfo = new CartInfo();
            SkuInfo skuInfo = skuDetailFeignClient.getSkuInfo(skuId);
            cartInfo.setUserId(oneOfUserId);
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setRealTimePrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
            //最新添加的商品需要进行勾选
            cartInfo.setIsChecked(1);
            hashOps.put(skuId.toString(), cartInfo);

        } else {
            //不是第一次，修改数量
            CartInfo redisCartInfo = (CartInfo) hashOps.get(skuId.toString());
            redisCartInfo.setSkuNum(redisCartInfo.getSkuNum() + skuNum);
            redisCartInfo.setUpdateTime(new Date());
            //如果商户后台修改了价格 redis里面也要修改
            redisCartInfo.setRealTimePrice(skuDetailFeignClient.getPrice(skuId));

            //提交
            hashOps.put(skuId.toString(), redisCartInfo);
        }
        //更新过期时间
        redisTemplate.expire(getUserCartKey(oneOfUserId), RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //1.未登录的情况
        if(StringUtils.isEmpty(userId)&&!StringUtils.isEmpty(userTempId)){
           return getCartListFromRedis(userTempId);
        }

        //2.已登录的情况
        if(!StringUtils.isEmpty(userId)&&!StringUtils.isEmpty(userTempId)){

        }

        return cartInfoList;
    }

    @SneakyThrows
    private List<CartInfo> getCartListFromRedis(String userTempId) {
        //获取购物车列表
        BoundHashOperations hashOps = getRedisHashOps(userTempId);
        List<CartInfo>  cartInfoList = hashOps.values();
        //db价格有修改，需要更新
        CompletableFuture<List<CartInfo>> cartInfoFuture = CompletableFuture.supplyAsync(() -> {
            updateCartInfoPrice4Redis(hashOps, cartInfoList);
            return cartInfoList;
        });
        //按照时间排序
        return cartInfoFuture.thenApplyAsync(infoList ->
                infoList.stream()
                        .sorted(Comparator.comparing(CartInfo::getUpdateTime).reversed())
                        .collect(Collectors.toList())).get();
    }

    //更新redis的价格
    private void updateCartInfoPrice4Redis(BoundHashOperations hashOps, List<CartInfo> cartInfoList) {
        for (CartInfo cartInfo : cartInfoList) {
            Long skuId = cartInfo.getSkuId();
            //查询现在的价格
            BigDecimal dbPrice = skuDetailFeignClient.getPrice(skuId);
            //查出redsiPrice
            BigDecimal redisPrice = cartInfo.getCartPrice();
            if(!dbPrice.equals(redisPrice)){
                //更新价格 然后同步到redis当中
                cartInfo.setRealTimePrice(dbPrice);
                //提交
                hashOps.put(skuId.toString(), cartInfo);
            }
        }
    }

    private BoundHashOperations getRedisHashOps(String oneOfUserId) {
        //生成redis中的key
        String userCartKey = getUserCartKey(oneOfUserId);
        //获取对应的hasOps
        return redisTemplate.boundHashOps(userCartKey);
    }

    private static String getUserCartKey(String oneOfUserId) {
        String userCartKey = RedisConst.USER_KEY_PREFIX + oneOfUserId + RedisConst.USER_CART_KEY_SUFFIX;
        return userCartKey;
    }
}
