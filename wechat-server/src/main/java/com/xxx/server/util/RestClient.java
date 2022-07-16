package com.xxx.server.util;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    public JSONObject postJson(String reqUrl, JSONObject reqJsonStrParam) {
        //设置 Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        //设置参数
        HttpEntity<JSONObject> requestEntity = new HttpEntity<>(reqJsonStrParam, httpHeaders);
        //执行请求
        ResponseEntity<JSONObject> resp = restTemplate
                .exchange(reqUrl, HttpMethod.POST, requestEntity, JSONObject.class);
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
}
