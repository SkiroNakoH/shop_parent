package com.atguigu.controller;

import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.CategroyViewVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;

    @RequestMapping({"/", "/index.html", "/index"})
    public String index(Model model) {
        List<CategroyViewVo> categroyViewVoList = skuDetailFeignClient.getCategoryView();
        model.addAttribute("list", categroyViewVoList);
        return "index/index";
    }
}