package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.mapper.PlatformPropertyKeyMapper;
import com.atguigu.service.PlatformPropertyKeyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

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

        return baseMapper.getPlatformPropertyByCategoryId(category1Id,category2Id,category3Id);
    }
}
