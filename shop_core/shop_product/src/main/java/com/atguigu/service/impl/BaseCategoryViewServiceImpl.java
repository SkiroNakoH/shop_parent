package com.atguigu.service.impl;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.mapper.BaseCategoryViewMapper;
import com.atguigu.search.CategroyViewVo;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-19
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {

//    @Override
    public List<CategroyViewVo> getCategoryViewFromView() {
        List<BaseCategoryView> categoryViewList = list(null);

        //一级菜单
        Map<Long, List<BaseCategoryView>> categroy1Map = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        return categroy1Map.entrySet().stream().map(categroyEntry -> {
            CategroyViewVo categroyViewVo = new CategroyViewVo();

            Long categroy1Id = categroyEntry.getKey();
            List<BaseCategoryView> category1ViewList = categroyEntry.getValue();
            String category1Name = category1ViewList.get(0).getCategory1Name();


            categroyViewVo.setCategoryId(categroy1Id);
            categroyViewVo.setCategoryName(category1Name);


            //二级菜单
            Map<Long, List<BaseCategoryView>> categroy2Map =
                    category1ViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

            categroyViewVo.setCategoryChild(
                    categroy2Map.entrySet().stream().map(entrySet1 -> {
                        CategroyViewVo categroyViewVo2 = new CategroyViewVo();
                        List<BaseCategoryView> category2ViewList = entrySet1.getValue();

                        //赋值
                        categroyViewVo2.setCategoryId(entrySet1.getKey());
                        categroyViewVo2.setCategoryName(category2ViewList.get(0).getCategory2Name());

                        //三级菜单
                        Map<Long, List<BaseCategoryView>> categroy3Map =
                                category2ViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));

                        categroyViewVo2.setCategoryChild(
                                categroy3Map.entrySet().stream().map(entrySet2 -> {
                                    CategroyViewVo categroyViewVo3 = new CategroyViewVo();

                                    categroyViewVo3.setCategoryId(entrySet2.getKey());
                                    categroyViewVo3.setCategoryName(entrySet2.getValue().get(0).getCategory3Name());

                                    return categroyViewVo3;
                                }).collect(Collectors.toList())
                        );
                        return categroyViewVo2;
                    }).collect(Collectors.toList())
            );
            return categroyViewVo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CategroyViewVo> getCategoryView() {
        return baseMapper.getCategoryView();
    }
}
