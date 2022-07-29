package com.xxx.server.mq;

import com.alibaba.fastjson2.JSONObject;
import com.xxx.server.enums.WechatApiHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * 群聊信息处理，tag为qunChat处理类
 */
@Component("qunChat")
@Slf4j
public class QunChatMessageHandler implements MqMessageHandler{

    @Override
    public boolean process(JSONObject message) {
        log.info("消息处理====》{}", message);
        String code = message.getString("code");
        WechatApiHelper wechatApiHelper = WechatApiHelper.getWechatApiHelper(code);
        if(wechatApiHelper == null){
            log.info("群消息处理异常,跳过处理");
            return true;
        }
        //TODO 发送消息，失败是否需要重试
        wechatApiHelper.invoke(message.getJSONObject("param"), (MultiValueMap<String, String>) message.get("query"));
        return true;
    }
}
