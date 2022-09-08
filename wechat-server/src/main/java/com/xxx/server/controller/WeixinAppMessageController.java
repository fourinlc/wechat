package com.xxx.server.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.XmlUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.enums.WechatApiHelper;
import com.xxx.server.pojo.RespBean;
import com.xxx.server.pojo.WeixinAppMessage;
import com.xxx.server.service.IWeixinAppMessageService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import javax.annotation.PostConstruct;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lc
 * @since 2022-09-05
 */
@RestController
@RequestMapping("/weixin-app-message")
@AllArgsConstructor
public class WeixinAppMessageController {

    private IWeixinAppMessageService weixinAppMessageService;

    // 新增修改链接
    @GetMapping("saveOrUpdate")
    @ApiOperation("链接新增修改")
    public RespBean saveOrUpdate(WeixinAppMessage weixinAppMessage){
        weixinAppMessage.setAction("view").setShowType("0").setType("5").setSoundType("0");
        return RespBean.sucess(weixinAppMessageService.saveOrUpdate(weixinAppMessage) ? "新增修改成功" : "新增修改失败", weixinAppMessage.getAppMessageId());
    }

    public static void main(String[] args) {
        WeixinAppMessage weixinAppMessage = new WeixinAppMessage().setDes("saasf").setUrl("asadad");
        Document document = XmlUtil.mapToXml(BeanUtil.beanToMap(weixinAppMessage, false, false), "appmsg");
        System.out.println(XmlUtil.toStr(document, true));
    }

    /*@PostConstruct*/
    public void init(){
        WeixinAppMessage weixinAppMessage = weixinAppMessageService.getById(3L);
        Document document = XmlUtil.mapToXml(BeanUtil.beanToMap(weixinAppMessage, false, false), "appmsg");
        JSONObject param = JSONObject.of("AppList", JSONArray.of(JSONObject.of("ToUserName", "19976248534@chatroom", "ContentXML", XmlUtil.toStr(document), "ContentType", 49)));
        MultiValueMap n = new LinkedMultiValueMap();
        n.add("key", "19a776d2-4608-4925-95c6-bc6d3fe7a112");
        System.out.println(WechatApiHelper.SEND_APP_MESSAGE.invoke(param, n));
    }

}
