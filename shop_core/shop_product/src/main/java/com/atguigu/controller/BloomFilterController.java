package com.atguigu.controller;

import com.atguigu.entity.SkuInfo;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/init")
public class BloomFilterController {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private RBloomFilter skuBloomFilter;

    //每天凌晨4点执行
    @Scheduled(cron = "* * 4 * * ? ")
    @GetMapping("/sku/bloom")
    public String skuBloom() {

        //查询数据
        LambdaQueryWrapper<SkuInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(SkuInfo::getId);

        List<SkuInfo> skuInfoList = skuInfoService.list(queryWrapper);
        for (SkuInfo skuInfo : skuInfoList) {
            Long id = skuInfo.getId();
            skuBloomFilter.add(id);
        }

        return "success";
    }

    @GetMapping("/sku/hasBloom")
    public String hasBloom() {
        boolean flag24 = skuBloomFilter.contains(24L);
        System.out.println("flag24 = " + flag24);

        boolean flag50 = skuBloomFilter.contains(50L);
        System.out.println("flag50 = " + flag50);
        return "success";
    }
}
