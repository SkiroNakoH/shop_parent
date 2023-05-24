package com.atguigu.mapper;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.search.CategroyViewVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * VIEW Mapper 接口
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-19
 */
public interface BaseCategoryViewMapper extends BaseMapper<BaseCategoryView> {

    List<CategroyViewVo> getCategoryView();
}
