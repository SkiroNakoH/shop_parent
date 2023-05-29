package com.atguigu.controller;

import com.atguigu.feign.SearchFeignClient;
import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.CategroyViewVo;
import com.atguigu.search.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;
    @Autowired
    private SearchFeignClient searchFeignClient;

    @RequestMapping({"/", "/index.html", "/index"})
    public String index(Model model) {
        List<CategroyViewVo> categroyViewVoList = skuDetailFeignClient.getCategoryView();
        model.addAttribute("list", categroyViewVoList);
        return "index/index";
    }

    @GetMapping({"/search.html"})
    public String search(SearchParam searchParam, Model model) {
        RetVal<Map> retVal = searchFeignClient.searchProduct(searchParam);
        model.addAllAttributes(retVal.getData());
        //添加路径
        String urlParam = paramUrlJoint(searchParam);
        model.addAttribute("urlParam", urlParam);

        //添加品牌
        String brandName = searchParam.getBrandName();
        String brandNameParam = getBrandNameParam(brandName);
        model.addAttribute("brandNameParam", brandNameParam);

        //添加平台属性
        String[] props = searchParam.getProps();
        List<Map<String, String>> propsParamList = getPropsList(props);
        model.addAttribute("propsParamList", propsParamList);

        //排序
        String order = searchParam.getOrder();
        Map<String, String> orderMap = getSortMap(order);
        model.addAttribute("orderMap", orderMap);

        return "search/index";
    }

    //&order=1:desc
    private Map<String, String> getSortMap(String order) {
        Map<String, String> map = new HashMap<>();
        if (!StringUtils.isEmpty(order)) {
            String[] orderSplit = order.split(":");
            if (orderSplit != null && orderSplit.length == 2) {
                map.put("type", orderSplit[0]);
                map.put("sort", orderSplit[1]);
            }
        } else {
            map.put("type", "1");
            map.put("sort", "desc");
        }

        return map;
    }

    //&props=4:苹果A14:CPU型号&props=5:5.0英寸以下:屏幕尺寸
    private List<Map<String, String>> getPropsList(String[] props) {
        ArrayList<Map<String, String>> arrayList = new ArrayList<>();

        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] propSplit = prop.split(":");
                if (propSplit != null && propSplit.length == 3) {
                    Map<String, String> map = new HashMap<>();
                    map.put("propertyKey", propSplit[2]);
                    map.put("propertyValue", propSplit[1]);
                    map.put("propertyKeyId", propSplit[0]);

                    arrayList.add(map);
                }
            }
        }

        return arrayList;
    }

    private static String getBrandNameParam(String brandName) {
        String brandNameParam = null;
        if (!StringUtils.isEmpty(brandName)) {
            String[] brandNameSplit = brandName.split(":");
            if (brandNameSplit.length == 2) {
                brandNameParam = "品牌： " + brandNameSplit[1];
            }
        }
        return brandNameParam;
    }

    //keyword=高端苹果&brandName=1:苹果&props=4:苹果A14:CPU型号&props=5:5.0英寸以下:屏幕尺寸
    private String paramUrlJoint(SearchParam searchParam) {
        StringBuilder sb = new StringBuilder();
        //关键字拼接
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            if (sb.length() != 0) {
                sb.append("&");
            }
            sb.append("keyword=" + keyword);
        }
        //品牌拼接
        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)) {
            if (sb.length() != 0) {
                sb.append("&");
            }
            sb.append("brandName=" + brandName);
        }
        //拼接平台属性
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                if (!StringUtils.isEmpty(prop)) {
                    if (sb.length() != 0) {
                        sb.append("&");
                    }
                    sb.append("props=" + prop);
                }
            }
        }

        return "search.html?" + sb;
    }
}
