package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.MD5;
import com.atguigu.util.UserIdUtil;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
                String secKillCode = MD5.encrypt(skuId.toString());
                return RetVal.ok(secKillCode);
            }else{
                return RetVal.fail("当前不处于秒杀时间");
            }
        }
        return RetVal.fail("该商品未参与秒杀活动");
    }
}
