package com.xxx.server.controller;


import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.mapper.WeixinBaseInfoMapper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.service.IWeixinBaseInfoService;
import com.xxx.server.util.RestClient;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

/**
 * <p>
 *  微信基础操作
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@RestController
@RequestMapping("/weixin-base-info")
@AllArgsConstructor
@Slf4j
public class WeixinBaseInfoController{

    private WeixinBaseInfoMapper weixinBaseInfoMapper;

    private RestClient restClient;

    @Autowired
    IWeixinBaseInfoService weixinBaseInfoService;

    @ApiOperation(value = "获取登录二维码")
    @PostMapping("/getLoginQrcode")
    RespBean getLoginQrcode(){
        return weixinBaseInfoService.getLoginQrcode();
    }

    @PostConstruct
    public void test(){
        JSONObject jsonObject = new JSONObject();
        /*jsonObject.put("Proxy","");
        log.info("获取二维码测试:{}",WechatApiHelper.GET_LOGIN_QRCODE_NEW.invoke(jsonObject));*/

        jsonObject.put("CurrentWxcontactSeq", 1);
        jsonObject.put("CurrentChatRoomContactSeq ", 5);
        LinkedMultiValueMap<Object, Object> objectObjectLinkedMultiValueMap = new LinkedMultiValueMap<>();
        objectObjectLinkedMultiValueMap.add("key","de16191a-c633-418f-9458-a9af51b99d0e");
        // jsonObject.put("queryVO", objectObjectLinkedMultiValueMap);
        // log.info("分页获取联系人:{}",WechatApiHelper.GET_CONTACT_LIST.invoke(jsonObject, objectObjectLinkedMultiValueMap));

        // jsonObject.put("key", "de16191a-c633-418f-9458-a9af51b99d0e");
        MultiValueMap<String,String> map = new LinkedMultiValueMap<>();
        map.add("key", "de16191a-c633-418f-9458-a9af51b99d0e");
        log.info("获取个人信息:{}",WechatApiHelper.GET_PROFILE.invoke(null, map));
    }
}
