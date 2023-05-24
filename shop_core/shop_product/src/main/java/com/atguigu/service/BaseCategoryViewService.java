package com.atguigu.service;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.search.CategroyViewVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * VIEW 服务类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-19
 */
public interface BaseCategoryViewService extends IService<BaseCategoryView> {

    List<CategroyViewVo> getCategoryView();
}
