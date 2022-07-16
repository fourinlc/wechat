package com.xxx.server.controller;


import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.util.RestClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@RestController
@RequestMapping("/weixin-base-info")
@AllArgsConstructor
@Slf4j
public class WeixinBaseInfoController {

    private WeixinBaseInfoMapper weixinBaseInfoMapper;

    private RestClient restClient;

    @PostConstruct
    public void test(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Proxy","");
        log.info("获取二维码测试:{}", restClient.postJson("/v1/login/GetLoginQrCodeNew", jsonObject));
    }

}
