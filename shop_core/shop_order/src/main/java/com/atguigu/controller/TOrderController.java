package com.atguigu.controller;


import com.atguigu.entity.TOrder;
import com.atguigu.entity.TOrderDetail;
import com.atguigu.mapper.TOrderMapper;
import com.atguigu.service.OrderInfoService;
import com.atguigu.service.TOrderDetailService;
import com.atguigu.service.TOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-06-05
 */
@RestController
public class TOrderController {
    @Autowired
    private TOrderService order1Service;
    @Autowired
    private TOrderDetailService detail1Service;
    @Autowired
    private TOrderMapper order1Mapper;

    //1.保存订单利用分库分表
    @GetMapping("test01/{loopNum}")
    public void test01(@PathVariable Integer loopNum) {
        for (int i = 0; i < loopNum; i++) {
            TOrder tOrder = new TOrder();
            tOrder.setOrderPrice(99);
            String uuid = UUID.randomUUID().toString();
            tOrder.setTradeNo(uuid);
            tOrder.setOrderStatus("未支付");
            //设置分库分表字段
            int userId = new Random().nextInt(20);
            tOrder.setUserId(Long.parseLong(userId + ""));
            System.out.println("用户id" + userId);
            order1Service.save(tOrder);
        }
    }

    //2.保存订单与订单详情分库分表
    @GetMapping("test02/{userId}")
    public void saveOrderInfo(@PathVariable Long userId) {
        TOrder tOrder = new TOrder();
        tOrder.setTradeNo("enjoy6288");
        tOrder.setOrderPrice(9900);
        tOrder.setUserId(userId);//ds-1,table_4
        order1Service.save(tOrder);

        TOrderDetail iphone13 = new TOrderDetail();
        iphone13.setOrderId(tOrder.getId());
        iphone13.setSkuName("Iphone13");
        iphone13.setSkuNum(1);
        iphone13.setSkuPrice(6000);
        iphone13.setUserId(userId);
        detail1Service.save(iphone13);

        TOrderDetail sanxin = new TOrderDetail();
        sanxin.setOrderId(tOrder.getId());
        sanxin.setSkuName("三星");
        sanxin.setSkuNum(2);
        sanxin.setSkuPrice(3900);
        sanxin.setUserId(userId); //要进行分片计算
        detail1Service.save(sanxin);
        System.out.println("执行完成");
    }


    //3.查询订单与订单详情
    @GetMapping("test03/{userId}")
    public void test03(@PathVariable Long userId) {
        List<TOrder> orderList = order1Mapper.queryOrderAndDetail(userId, null);
        //根据订单的id查询订单(全库查询) 没有使用分片的键 速度相当慢
//        List<TOrder> orderList=order1Mapper.queryOrderAndDetail(null,1665680576490893313L);
        System.out.println(orderList);
    }


}

