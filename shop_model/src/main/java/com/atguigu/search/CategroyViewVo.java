package com.atguigu.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategroyViewVo {
    private Long categoryId;
    //分类名称
    private String categoryName;
    //子分类信息
    private List<CategroyViewVo> categoryChild;

}
