package com.atguigu.controller;

import com.atguigu.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/product")
@RestController
public class RedisLockController {
    @Autowired
    private RedisService redisService;

    @RequestMapping("/setNum")
    public String setNum() {
        redisService.setNum();
        return "Success";
    }
}
