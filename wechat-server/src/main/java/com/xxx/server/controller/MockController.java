package com.xxx.server.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.constant.ResConstant;
import com.xxx.server.enums.WechatApiHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@Api
public class MockController {
    // 模拟创建群聊
    @ApiOperation(value = "模拟创建群聊")
    @GetMapping("buildQun")
    public void buildQun(String key, Integer count, @RequestParam("userList") List<String> userList) throws InterruptedException {
        JSONObject jsonObject = JSONObject.of();
        MultiValueMap multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("key", key);
        JSONObject jsonObject1 = JSONObject.of("Val", 1);
        jsonObject.put("userList", userList);
        for (int i = 0; i < count; i++) {
            // 增加一个随机数
            jsonObject.put("TopIc", "test"  + i + "_" + RandomUtil.randomInt());
            JSONObject CREATE_CHATROOM = WechatApiHelper.CREATE_CHATROOM.invoke(jsonObject, multiValueMap);
            Thread.sleep(2000);
            if (!StrUtil.equals(CREATE_CHATROOM.getString(ResConstant.CODE), "200")) {
                log.info("创建群聊失败，退出");
                break;
            }
            Thread.sleep(2000);
            String chatRoomName = CREATE_CHATROOM.getJSONObject(ResConstant.DATA).getJSONObject("chatRoomName").getString("str");
            // 保存群聊信息
            jsonObject1.put("ChatRoomName", chatRoomName);
            JSONObject MOVETO_CONTRACT = WechatApiHelper.MOVETO_CONTRACT.invoke(jsonObject1, multiValueMap);
            // 保存群聊成功继续下一个群
            if (!StrUtil.equals(MOVETO_CONTRACT.getString(ResConstant.CODE), "200")) {
                log.info("保存群聊失败，退出");
                break;
            }
        }


    }
}
