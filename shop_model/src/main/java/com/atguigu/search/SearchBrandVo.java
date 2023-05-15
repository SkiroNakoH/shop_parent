package com.atguigu.search;

import lombok.Data;

import java.io.Serializable;

// 品牌数据
@Data
public class SearchBrandVo implements Serializable {
    //品牌的id
    private Long brandId;
    //品牌的名称
    private String brandName;
    //品牌的图片地址
    private String brandLogoUrl;
}

