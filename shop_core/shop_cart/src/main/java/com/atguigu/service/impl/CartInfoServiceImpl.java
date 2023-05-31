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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
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
            //添加的商品需要进行勾选
            redisCartInfo.setIsChecked(1);
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
        if (StringUtils.isEmpty(userId) && !StringUtils.isEmpty(userTempId)) {
            return getCartListFromRedis(userTempId);
        }

        //2.已登录的情况
        if (!StringUtils.isEmpty(userId) && !StringUtils.isEmpty(userTempId)) {
            //查询所有已登录购物项
            BoundHashOperations hashOps = getRedisHashOps(userId);
            Set keys = hashOps.keys();
            if (!CollectionUtils.isEmpty(keys)) {
                //合并两个购物车
                return mergeCartInfoList(userId, userTempId);
            } else {
                //todo 登录的购物车没货物，只需要该未登录的购物车名称为登录用户名
                return putNoLoginCart2Login(userId, userTempId);
            }
        }

        return cartInfoList;
    }

    @Override
    public void checkCart(Long skuId, Integer isChecked, String oneOfUserId) {
        //取出redis中对应的值
        BoundHashOperations redisHashOps = getRedisHashOps(oneOfUserId);
        if (redisHashOps.hasKey(skuId.toString())) {
            CartInfo cartInfo = (CartInfo) redisHashOps.get(skuId.toString());

            cartInfo.setIsChecked(isChecked);

            redisHashOps.put(skuId.toString(), cartInfo);
            //设置过期时间
            redisTemplate.expire(getUserCartKey(oneOfUserId), RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
        }
    }

    @Override
    public void deleteCart(Long skuId, String oneOfUserId) {
        BoundHashOperations redisHashOps = getRedisHashOps(oneOfUserId);
        if (redisHashOps.hasKey(skuId.toString())) {
            redisHashOps.delete(skuId.toString());
        }
    }

    private List<CartInfo> putNoLoginCart2Login(String userId, String userTempId) {
        //查询所有未登录的
        BoundHashOperations noLoginHashOps = getRedisHashOps(userTempId);
        List<CartInfo> noLoginCartInfoList = noLoginHashOps.values();

        Map<String, CartInfo> loginCartInfoMap = noLoginCartInfoList.stream().map(cartInfo -> {
            cartInfo.setUserId(userId);
            cartInfo.setRealTimePrice(skuDetailFeignClient.getPrice(cartInfo.getSkuId()));
            return cartInfo;
        }).collect(Collectors.toMap(cartInfo -> cartInfo.getSkuId().toString(), cartInfo -> cartInfo, (a, b) -> a));


        //提交所有
        getRedisHashOps(userId).putAll(loginCartInfoMap);
        //把临时购物车里面的内容删除
        String userCartKey = getUserCartKey(userTempId);
        redisTemplate.delete(userCartKey);

        return loginCartInfoMap.entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(CartInfo::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }


    //合并购物车
    private List<CartInfo> mergeCartInfoList(String userId, String userTempId) {
        //获取两个购物车
        //查询所有未登录的
        BoundHashOperations noLoginHashOps = getRedisHashOps(userTempId);
        List<CartInfo> noLoginCartInfoList = noLoginHashOps.values();
        //查询所有已登录的
        BoundHashOperations loginHashOps = getRedisHashOps(userId);
        List<CartInfo> loginCartInfoList = loginHashOps.values();

        //将登录购物车转map，再判断未登录是否再登录购物车里
        Map<String, CartInfo> loginCartInfoMap = loginCartInfoList.stream()
                .map(cartInfo -> {
                    cartInfo.setRealTimePrice(skuDetailFeignClient.getPrice(cartInfo.getSkuId()));
                    return cartInfo;
                })
                .collect(Collectors.toMap(cartInfo -> cartInfo.getSkuId().toString(), cartInfo -> cartInfo, (a, b) -> a));

        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
            String noLoginSkuId = String.valueOf(noLoginCartInfo.getSkuId());
            if (loginCartInfoMap.containsKey(noLoginSkuId)) {
                //修改数量
                CartInfo loginCartInfo = loginCartInfoMap.get(noLoginSkuId);
                loginCartInfo.setSkuNum(loginCartInfo.getSkuNum() + noLoginCartInfo.getSkuNum());
            } else {
                //当skuId不同把临时用户id改为登录用户id
                noLoginCartInfo.setUserId(userId);
                noLoginCartInfo.setRealTimePrice(skuDetailFeignClient.getPrice(noLoginCartInfo.getSkuId()));
                loginCartInfoMap.put(noLoginSkuId, noLoginCartInfo);
            }

            loginCartInfoMap.get(noLoginSkuId).setUpdateTime(new Date());
        }

        //提交所有
        loginHashOps.putAll(loginCartInfoMap);
        //把临时购物车里面的内容删除
        String userCartKey = getUserCartKey(userTempId);
        redisTemplate.delete(userCartKey);

        return loginCartInfoMap.entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(CartInfo::getUpdateTime).reversed())
                .collect(Collectors.toList());

    }

    @SneakyThrows
    private List<CartInfo> getCartListFromRedis(String userTempId) {
        //获取购物车列表
        BoundHashOperations hashOps = getRedisHashOps(userTempId);
        List<CartInfo> cartInfoList = hashOps.values();
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
            if (!dbPrice.equals(redisPrice)) {
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
