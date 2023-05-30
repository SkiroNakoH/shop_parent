package com.atguigu.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.util.IpUtil;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AccessFilter implements GlobalFilter {
    @Value("${filter.whiteFilter}")
    private String filterWhiteFilter;
    @Autowired
    private RedisTemplate redisTemplate;
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * @param exchange 交换机，与服务进行交互
     * @param chain    责任链,将对象交给下一个代理类
     * @return 请求完成
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //拦截用户非法请求内部接口
        String path = request.getURI().getPath();
        if (antPathMatcher.match("/sku/**", path)) {
            return writerDataToBrowser(response, RetValCodeEnum.NO_PERMISSION);
        }

        //判断用户是否登录过
        String userId = getUserId(request);
        //用户已登录，放行
        if (!StringUtils.isEmpty(userId))
            return chain.filter(exchange);


        String[] filterSplit = filterWhiteFilter.split(",");
        for (String filter : filterSplit) {
            if (path.indexOf(filter) != -1) {
                //需要进行过滤
                response.getHeaders().set(HttpHeaders.LOCATION, "http://passport.gmall.com/login.html?originalUrl=" + request.getURI());
                response.setStatusCode(HttpStatus.SEE_OTHER);
                return response.setComplete();
            }

        }

//        放行
        return chain.filter(exchange);
    }

    private static Mono<Void> writerDataToBrowser(ServerHttpResponse response, RetValCodeEnum retValCodeEnum) {
        //用户非法访问，返回警告
        response.getHeaders().add("Content-Type", "application/json");
        RetVal<Object> retVal = RetVal.build(null, retValCodeEnum);

        byte[] bytes = JSONObject.toJSONString(retVal).getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(dataBuffer));
    }

    private String getUserId(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst("token");

        if (cookie != null) {
            String token = cookie.getValue();
            //拼接redis存储的用户key
            String userLoginKey = "user:login:" + token;
            JSONObject userJsonObject = (JSONObject) redisTemplate.opsForValue().get(userLoginKey);
            if (userJsonObject != null) {
                //对比ip地址
                String userLoginIp = userJsonObject.getString("loginIp");
                String currentIp = IpUtil.getGatwayIpAddress(request);
                if (userLoginIp.equals(currentIp))
                    return String.valueOf(userJsonObject.getLong("userId"));
            }
        }

        return null;

    }
}
