package com.xxx.server.mq;

import com.alibaba.fastjson2.JSONObject;
import com.dachen.starter.mq.annotation.MQConsumer;
import com.dachen.starter.mq.base.AbstractMQPushConsumer;
import com.dachen.starter.mq.base.MessageExtConst;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        if(!mqMessageHandlers.containsKey(tags)) {
            log.debug("该tag未存在处理类");
            return true;
        }
        return mqMessageHandlers.get(tags).process(message);
    }
}
