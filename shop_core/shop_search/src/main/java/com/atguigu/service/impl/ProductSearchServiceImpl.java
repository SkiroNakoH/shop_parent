package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseBrand;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SkuDetailFeignClient;
import com.atguigu.dao.ProductSearchRepository;
import com.atguigu.search.*;
import com.atguigu.service.ProductSearchService;
import lombok.SneakyThrows;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ProductSearchServiceImpl implements ProductSearchService {
    @Autowired
    private SkuDetailFeignClient skuDetailFeignClient;
    @Autowired
    private ProductSearchRepository productSearchMapper;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    //执行es语句的客户端
    @Autowired
    private RestHighLevelClient restHighLevelClient;


    @Override
    public void onSale(Long skuId) {
        Product product = new Product();

        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            //添加商品信息
            SkuInfo skuInfo = skuDetailFeignClient.getSkuInfo(skuId);
            product.setId(skuId);
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            product.setProductName(skuInfo.getSkuName());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setCreateTime(new Date());

            return skuInfo;
        }, threadPoolExecutor);

        CompletableFuture<Void> brandFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            //添加品牌相关信息
            BaseBrand brand = skuDetailFeignClient.getBrand(skuInfo.getBrandId());
            product.setBrandId(brand.getId());
            product.setBrandName(brand.getBrandName());
            product.setBrandLogoUrl(brand.getBrandLogoUrl());
        }, threadPoolExecutor);

        CompletableFuture<Void> categoryViewFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            //设置分类信息
            BaseCategoryView categoryView = skuDetailFeignClient.getCategoryView(skuInfo.getCategory3Id());
            product.setCategory1Id(categoryView.getCategory1Id());
            product.setCategory1Name(categoryView.getCategory1Name());
            product.setCategory2Id(categoryView.getCategory2Id());
            product.setCategory2Name(categoryView.getCategory2Name());
            product.setCategory3Id(categoryView.getCategory3Id());
            product.setCategory3Name(categoryView.getCategory3Name());
        }, threadPoolExecutor);

        CompletableFuture<Void> platformPropertyKeyListFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            //设置平台属性集合对象
            List<PlatformPropertyKey> platformPropertyKeyList = skuDetailFeignClient.getPlatformPropertyBySkuId(skuId);
            List<SearchPlatformProperty> searchPlatformPropertyList = platformPropertyKeyList.stream()
                    .map(platformPropertyKey -> {
                        SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();

                        searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                        searchPlatformProperty.setPropertyValue(platformPropertyKey.getPropertyValueList().get(0).getPropertyValue());
                        searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());

                        return searchPlatformProperty;
                    }).collect(Collectors.toList());

            product.setPlatformProperty(searchPlatformPropertyList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(skuInfoFuture, brandFuture, categoryViewFuture, platformPropertyKeyListFuture).join();

        productSearchMapper.save(product);
    }

    @Override
    public void offSale(Long skuId) {
        productSearchMapper.deleteById(skuId);
    }

    @SneakyThrows   //执行时，异常改为try...catch..
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //1. 封装查询语句
        SearchRequest searchRequest = buildQueryDSL(searchParam);
        //2. 执行查询语句
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //3. 解析查询结果
        SearchResponseVo searchResponseVo = parseSearchResponse(searchResponse);

        //4. 封装其他数据
        Integer pageSize = searchParam.getPageSize();
        Integer pageNo = searchParam.getPageNo();
        Long total = searchResponseVo.getTotal();

        searchResponseVo.setPageSize(pageSize);
        searchResponseVo.setPageNo(pageNo);
        searchResponseVo.setTotalPages(
                (total % pageSize == 0) ? (total / pageSize) : (total / pageSize + 1)
        );

        return searchResponseVo;
    }

    //解析查询结果
    private SearchResponseVo parseSearchResponse(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();

        //查询总数
        searchResponseVo.setTotal(hits.getTotalHits());
        //查询的每条结果
        SearchHit[] innerHits = hits.getHits();
        if (innerHits != null && innerHits.length > 0) {
            for (SearchHit innerHit : innerHits) {
                if (innerHit != null) {
                    Product product = JSONObject.parseObject(innerHit.getSourceAsString(), Product.class);
                    //设置高亮显示
                    HighlightField highlightField = innerHit.getHighlightFields().get("productName");
                    if (highlightField != null) {
                        Text[] fragments = highlightField.getFragments();
                        if (fragments != null & fragments.length > 0) {
                            product.setProductName(fragments[0].toString());
                        }
                    }
                    //将结果放入vo中
                    searchResponseVo.getProductList().add(product);
                }
            }
        }
        //查询品牌聚合信息
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        if (brandIdAgg != null) {
            List<SearchBrandVo> searchBrandVoList = brandIdAgg.getBuckets().stream()
                    .map(bucket -> {
                        SearchBrandVo searchBrandVo = new SearchBrandVo();
                        //品牌id
                        Number brandId = bucket.getKeyAsNumber();
                        searchBrandVo.setBrandId(brandId.longValue());
                        //品牌log
                        ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
                        searchBrandVo.setBrandLogoUrl(brandLogoUrlAgg.getBuckets().get(0).getKeyAsString());
                        //品牌名称
                        ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
                        searchBrandVo.setBrandName(brandNameAgg.getBuckets().get(0).getKeyAsString());

                        return searchBrandVo;
                    }).collect(Collectors.toList());

            searchResponseVo.setBrandVoList(searchBrandVoList);
        }
        //查询平台属性聚合信息
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        if (platformPropertyAgg != null) {
            ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
            if (propertyKeyIdAgg != null) {
                List<SearchPlatformPropertyVo> searchPlatformPropertyVoList = propertyKeyIdAgg.getBuckets().stream()
                        .map(propertyKeyBucket -> {
                            SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
                            //平台属性id
                            Number propertyKeyId = propertyKeyBucket.getKeyAsNumber();
                            searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());
                            //平台属性名称key
                            ParsedStringTerms propertyKeyAgg = propertyKeyBucket.getAggregations().get("propertyKeyAgg");
                            searchPlatformPropertyVo.setPropertyKey(propertyKeyAgg.getBuckets().get(0).getKeyAsString());
                            //设置平台属性值value
                            ParsedStringTerms propertyValueAgg = propertyKeyBucket.getAggregations().get("propertyValueAgg");
                            List<String> propertyValueList = propertyValueAgg.getBuckets().stream()
                                    .map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                            searchPlatformPropertyVo.setPropertyValueList(propertyValueList);

                            return searchPlatformPropertyVo;
                        }).collect(Collectors.toList());

                searchResponseVo.setPlatformPropertyList(searchPlatformPropertyVoList);
            }
        }

        return searchResponseVo;
    }

    //封装查询条件
    private SearchRequest buildQueryDSL(SearchParam searchParam) {
        //构建返回对象
        SearchRequest searchRequest = new SearchRequest("product");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //根据分类id查询
        Long category1Id = searchParam.getCategory1Id();
        if (category1Id != null && category1Id != 0) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", category1Id));
        }
        Long category2Id = searchParam.getCategory2Id();
        if (category2Id != null && category2Id != 0) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", category2Id));
        }
        Long category3Id = searchParam.getCategory3Id();
        if (category3Id != null && category3Id != 0) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", category3Id));
        }
        //查询品牌id 1:苹果
        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)) {
            String[] brandSplit = brandName.split(":");
            if (brandSplit != null && brandSplit.length > 0) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("brandId", brandSplit[0]));
            }
        }

        //查询平台属性,使用的是嵌套nested props=4:骁龙888:CPU型号&props=5:5.0英寸以下:屏幕尺寸
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] propSpilt = prop.split(":");
                if (propSpilt != null && propSpilt.length == 3) {
                    //内部嵌套查询
                    BoolQueryBuilder innerBoolQueryBuilder = QueryBuilders.boolQuery();

                    innerBoolQueryBuilder.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", propSpilt[0]));
                    innerBoolQueryBuilder.must(QueryBuilders.termQuery("platformProperty.propertyValue", propSpilt[1]));

                    //添加内部嵌套查询
                    NestedQueryBuilder queryBuilder = QueryBuilders.nestedQuery("platformProperty", innerBoolQueryBuilder, ScoreMode.None);
                    boolQueryBuilder.filter(queryBuilder);
                }
            }
        }

        //查询关键字 keyword=高端苹果
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("productName", keyword).operator(Operator.OR));
        }

        sourceBuilder.query(boolQueryBuilder);

        //分页
        Integer pageNo = searchParam.getPageNo();
        Integer pageSize = searchParam.getPageSize();

        sourceBuilder.from((pageNo - 1) * pageSize);
        sourceBuilder.size(pageSize);

        //排序 1:asc
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] orderSplit = order.split(":");
            if (orderSplit != null && orderSplit.length == 2) {
                switch (orderSplit[0]) {
                    case "1":
                        sourceBuilder.sort("hotScore", "asc".equals(orderSplit[1]) ? SortOrder.ASC : SortOrder.DESC);
                        break;
                    case "2":
                        sourceBuilder.sort("price", "asc".equals(orderSplit[1]) ? SortOrder.ASC : SortOrder.DESC);
                        break;
                }
            }
        } else {
            //综合排序默认降序
            sourceBuilder.sort("hotScore", SortOrder.DESC);
        }

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");

        sourceBuilder.highlighter(highlightBuilder);

        //设置聚合关系--品牌相关
        sourceBuilder.aggregation(
                AggregationBuilders.terms("brandIdAgg").field("brandId")
                        .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                        .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"))
        );
        //设置聚合关系--平台相关
        sourceBuilder.aggregation(
                AggregationBuilders.nested("platformPropertyAgg", "platformProperty")
                        .subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                                .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                                .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue")))
        );

        //设置使用哪个type
        searchRequest.types("info");
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    @Deprecated
    public void onSaleDeprecated(Long skuId) {
        Product product = new Product();

        //添加商品信息
        SkuInfo skuInfo = skuDetailFeignClient.getSkuInfo(skuId);
        product.setId(skuId);
        product.setDefaultImage(skuInfo.getSkuDefaultImg());
        product.setProductName(skuInfo.getSkuName());
        product.setPrice(skuInfo.getPrice().doubleValue());
        product.setCreateTime(new Date());

        //添加品牌相关信息
        BaseBrand brand = skuDetailFeignClient.getBrand(skuInfo.getBrandId());
        product.setBrandId(brand.getId());
        product.setBrandName(brand.getBrandName());
        product.setBrandLogoUrl(brand.getBrandLogoUrl());

        //设置分类信息
        BaseCategoryView categoryView = skuDetailFeignClient.getCategoryView(skuInfo.getCategory3Id());
        product.setCategory1Id(categoryView.getCategory1Id());
        product.setCategory1Name(categoryView.getCategory1Name());
        product.setCategory2Id(categoryView.getCategory2Id());
        product.setCategory2Name(categoryView.getCategory2Name());
        product.setCategory3Id(categoryView.getCategory3Id());
        product.setCategory3Name(categoryView.getCategory3Name());

        //设置平台属性集合对象
        List<PlatformPropertyKey> platformPropertyKeyList = skuDetailFeignClient.getPlatformPropertyBySkuId(skuId);
        List<SearchPlatformProperty> searchPlatformPropertyList = platformPropertyKeyList.stream().map(platformPropertyKey -> {
            SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();

            searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
            searchPlatformProperty.setPropertyValue(platformPropertyKey.getPropertyValueList().get(0).getPropertyValue());
            searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());

            return searchPlatformProperty;
        }).collect(Collectors.toList());

        product.setPlatformProperty(searchPlatformPropertyList);

        productSearchMapper.save(product);
    }
}
