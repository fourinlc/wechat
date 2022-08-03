package com.xxx.server.mq;

import com.alibaba.fastjson2.JSONObject;
import com.dachen.starter.mq.annotation.MQConsumer;
import com.dachen.starter.mq.base.AbstractMQPushConsumer;
import com.dachen.starter.mq.base.MessageExtConst;
import com.google.gson.internal.LinkedTreeMap;
import com.xxx.server.enums.WechatApiHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * 统一业务消息处理入口
 */
@Slf4j
@MQConsumer(topic = "${spring.rocketmq.consumer-topic}", consumerGroup = "${spring.rocketmq.consumer-group}")
@AllArgsConstructor
public class WechatMqConsumer extends AbstractMQPushConsumer<JSONObject> {

    private Map<String,MqMessageHandler> mqMessageHandlers;

    @Override
    public boolean process(JSONObject message, Map<String, Object> extMap) {

        JSONObject jsonObject = new JSONObject(extMap);
        String tags = jsonObject.getString(MessageExtConst.PROPERTY_TAGS);
        log.info("原始消息信息 ：body:{}, tag:{}, messageId:{}", message, tags, jsonObject.getString(MessageExtConst.PROPERTY_EXT_MSG_ID));
        // 先尝试直接用 WechatApiHelper进行处理
        WechatApiHelper wechatApiHelper = WechatApiHelper.getWechatApiHelper(jsonObject.getString("code"));
        if(mqMessageHandlers.containsKey(tags)){
            // 有子类实现直接策略处理
            return mqMessageHandlers.get(tags).process(message);
        }else if(wechatApiHelper != null){
            // 直接默认处理数据信息了
            LinkedTreeMap query = (LinkedTreeMap)message.get("query");
            JSONObject param = message.getJSONObject("param");
            MultiValueMap<String,String> multiValueMap = new LinkedMultiValueMap(query);
            wechatApiHelper.invoke(param, multiValueMap);
            return true;
        }else {
            log.info("未处理消息：msgId:{}", jsonObject.getString(MessageExtConst.PROPERTY_EXT_MSG_ID));
            return false;
        }
    }
}
