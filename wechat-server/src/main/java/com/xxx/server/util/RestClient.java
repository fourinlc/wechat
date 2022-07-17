package com.xxx.server.util;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class RestClient {

    private RestTemplate restTemplate ;
    /**
     * json格式处理
     * @param reqUrl 请求url
     * @param reqJsonStrParam
     * @return
     */
    public JSONObject postJson(String reqUrl, Object reqJsonStrParam) {
        if(reqJsonStrParam instanceof Map){
            // 校验是否带有query链接,默认约定参数
            Object query = ((Map<?, ?>) reqJsonStrParam).get("queryVO");
            // 默认按照MultiValueMap格式传参
            if(query instanceof MultiValueMap){
                ((Map<?, ?>) reqJsonStrParam).remove("queryVO");
                return postJson(reqUrl, reqJsonStrParam, (MultiValueMap)query);
            }
        }
        //设置 Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        //设置参数
        HttpEntity<Object> requestEntity = new HttpEntity<>(reqJsonStrParam, httpHeaders);
        //执行请求
        ResponseEntity<JSONObject> resp = restTemplate
                .exchange(reqUrl, HttpMethod.POST, requestEntity, JSONObject.class);
        //返回请求返回值
        return resp.getBody();
    }

    /**
     *
     * @param reqUrl
     * @param reqJsonStrParam
     * @param query 后边附带的query信息
     * @return
     */
    public JSONObject postJson(String reqUrl, Object reqJsonStrParam, MultiValueMap<String,String> query) {
        //设置 Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        //设置参数
        HttpEntity<Object> requestEntity = new HttpEntity<>(reqJsonStrParam, httpHeaders);
        UriComponents builder = UriComponentsBuilder
                .fromUriString(reqUrl)
                .queryParams(query)
                .build();
        //执行请求
        ResponseEntity<JSONObject> resp = restTemplate
                .exchange(builder.toUriString(), HttpMethod.POST, requestEntity, JSONObject.class);
        //返回请求返回值
        return resp.getBody();
    }

    /**
     * 普通表单提交处理
     * @param reqUrl
     * @param reqFormPara
     * @return
     */
    public String postForm(String reqUrl, String reqFormPara) {
        //设置 Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //设置参数
        HttpEntity<String> requestEntity = new HttpEntity<>(reqFormPara, httpHeaders);
        //执行请求
        ResponseEntity<String> resp = restTemplate
                .exchange(reqUrl, HttpMethod.POST, requestEntity, String.class);
        //返回请求返回值
        return resp.getBody();
    }


    // TODO
    public JSONObject getForm(String reqUrl, Object reqFormPara) {
        if(reqFormPara instanceof Map){
            // 校验是否带有query链接,默认约定参数
            Object query = ((Map<?, ?>) reqFormPara).get("queryVO");
            // 默认按照MultiValueMap格式传参
            if(query instanceof List){
                ((Map<?, ?>) reqFormPara).remove("queryVO");
                reqUrl = UriComponentsBuilder
                        .fromUriString(reqUrl)
                        .queryParams((MultiValueMap)query)
                        .build()
                        .toUriString();
            }
        }
        //设置 Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        // 校验是否存在,构造M
        //设置参数
        HttpEntity<Object> requestEntity = new HttpEntity<>(reqFormPara, httpHeaders);
        //执行请求
        ResponseEntity<JSONObject> resp = restTemplate
                .exchange(reqUrl, HttpMethod.GET, requestEntity, JSONObject.class);
        //返回请求返回值
        return resp.getBody();
    }

}
