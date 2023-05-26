package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.mapper.PlatformPropertyKeyMapper;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * <p>
 * 属性表 服务实现类
 * </p>
 *
 * @author SkiroNakoH
 * @since 2023-05-15
 */
@Service
public class PlatformPropertyKeyServiceImpl extends ServiceImpl<PlatformPropertyKeyMapper, PlatformPropertyKey> implements PlatformPropertyKeyService {

    @Autowired
    private PlatformPropertyValueService platformPropertyValueService;

    /*      //    for循环里有多条sql语句查询，舍弃
        public List<PlatformPropertyKey> getPropertyByCategoryId1(Long category1Id, Long category2Id, Long category3Id) {
            //1.根据商品分类id商品平台属性名称
            List<PlatformPropertyKey> propertyKeyList = baseMapper.getPropertyKeyByCategoryId(category1Id, category2Id, category3Id);
            //2.根据平台属性名称id商品平台属性值
            if (!CollectionUtils.isEmpty(propertyKeyList)) {
                for (PlatformPropertyKey propertyKey : propertyKeyList) {
                    LambdaQueryWrapper<PlatformPropertyValue> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(PlatformPropertyValue::getPropertyKeyId, propertyKey.getId());
                    List<PlatformPropertyValue> propertyValueList = propertyValueService.list(wrapper);
                    propertyKey.setPropertyValueList(propertyValueList);
                }
            }
            return propertyKeyList;
        }
    */
    @Override
    public List<PlatformPropertyKey> getPlatformPropertyByCategoryId(Long category1Id, Long category2Id, Long category3Id) {

        return baseMapper.getPlatformPropertyByCategoryId(category1Id, category2Id, category3Id);
    }

    @Transactional
    @Override
    public void savePlatformProperty(PlatformPropertyKey platformPropertyKey) {
        //新增、修改 key
//        if (platformPropertyKey.getId() == null || platformPropertyKey.getId() == 0)
        saveOrUpdate(platformPropertyKey);

        Long keyId = platformPropertyKey.getId();
        List<PlatformPropertyValue> propertyValueList = platformPropertyKey.getPropertyValueList();

        //删除
        LambdaQueryWrapper<PlatformPropertyValue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PlatformPropertyValue::getPropertyKeyId, keyId);
        List<PlatformPropertyValue> oldPropertyValueList = platformPropertyValueService.list(queryWrapper);

//        propertyValueList1.removeAll(propertyValueList);
        oldPropertyValueList.removeIf(i -> {
            for (PlatformPropertyValue propertyValue : propertyValueList) {
                if (Objects.equals(propertyValue.getId(), i.getId()))
                    return true;
            }
            return false;
        });

        if (!oldPropertyValueList.isEmpty()) {
            ArrayList<Long> idList = new ArrayList<>();
            for (PlatformPropertyValue platformPropertyValue : oldPropertyValueList) {
                idList.add(platformPropertyValue.getId());
            }
            platformPropertyValueService.removeByIds(idList);
        }

        //新增、修改 value
        if (!propertyValueList.isEmpty()) {
            //修改
            for (PlatformPropertyValue propertyValue : propertyValueList) {
                propertyValue.setPropertyKeyId(keyId);
            }
            platformPropertyValueService.saveOrUpdateBatch(propertyValueList);

        }
    }

    @Override
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(Long skuId) {
        return baseMapper.getPlatformPropertyBySkuId(skuId);
    }
}
