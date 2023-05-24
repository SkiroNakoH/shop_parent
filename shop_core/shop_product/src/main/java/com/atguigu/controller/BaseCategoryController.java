package com.atguigu.controller;


import com.atguigu.entity.BaseCategory1;
import com.atguigu.entity.BaseCategory2;
import com.atguigu.entity.BaseCategory3;
import com.atguigu.result.RetVal;
import com.atguigu.search.CategroyViewVo;
import com.atguigu.service.BaseCategory1Service;
import com.atguigu.service.BaseCategory2Service;
import com.atguigu.service.BaseCategory3Service;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-15
 */
//@CrossOrigin
@Api(tags = "category分类接口")
@RestController
@RequestMapping("/product")
public class BaseCategoryController {
    @Autowired
    private BaseCategory1Service baseCategory1Service;
    @Autowired
    private BaseCategory2Service baseCategory2Service;
    @Autowired
    private BaseCategory3Service baseCategory3Service;
    @Autowired
    private BaseCategoryViewService baseCategoryViewService;

    @ApiOperation("查找一级分类")
    @GetMapping("/getCategory1")
    public RetVal getCategory1() {

        List<BaseCategory1> baseCategory1List = baseCategory1Service.list(null);
        return RetVal.ok(baseCategory1List);
    }

    @ApiOperation("根据一级分类查找二级分类")
    @GetMapping("/getCategory2/{category1Id}")
    public RetVal getCategory2(@PathVariable Long category1Id) {
        LambdaQueryWrapper<BaseCategory2> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory2::getCategory1Id, category1Id);

        List<BaseCategory2> baseCategory2List = baseCategory2Service.list(queryWrapper);
        return RetVal.ok(baseCategory2List);
    }

    @ApiOperation("根据二级分类查找三级分类")
    @GetMapping("/getCategory3/{category2Id}")
    public RetVal getCategory3(@PathVariable Long category2Id) {
        LambdaQueryWrapper<BaseCategory3> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory3::getCategory2Id, category2Id);

        List<BaseCategory3> baseCategory3List = baseCategory3Service.list(queryWrapper);
        return RetVal.ok(baseCategory3List);
    }

    @ApiOperation("首页分类数据的显示 ")
    @GetMapping("/getCategoryView")
    public List<CategroyViewVo> getCategoryView() {
        return baseCategoryViewService.getCategoryView();
    }

}

