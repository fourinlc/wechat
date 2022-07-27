package com.dachen.starter.mq.custom.consumer;

import com.dachen.starter.mq.annotation.MQConsumer;
import com.dachen.starter.mq.base.AbstractMQPushConsumer;
import com.dachen.starter.mq.custom.constant.GuavaRocketConstants;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@MQConsumer(topic = GuavaRocketConstants.PROXY_TOPIC, consumerGroup = "guava-group")
public class DelayMqConsumer extends AbstractMQPushConsumer<MessageExt> {

    private DelayMqProducer delayMqProducer;

    @Override
    public boolean process(MessageExt messageExt, Map<String,Object> extMap) {
        return true;
    }

    @Override
    public ConsumeConcurrentlyStatus dealMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        try {
            for (MessageExt messageExt : msgs) {
                Map<String, String> properties = messageExt.getProperties();
                String topic = properties.get(GuavaRocketConstants.GUAVA_ORIGINAL_TOPIC);
                String times = properties.get(GuavaRocketConstants.GUAVA_TIMES);
                String tag = properties.get(GuavaRocketConstants.GUAVA_ORIGINAL_TAG);
                String keys = properties.get(GuavaRocketConstants.GUAVA_ORIGINAL_KEY);
                String uuid = properties.get(GuavaRocketConstants.GUAVA_ORIGINAL_UUID);
                if (StringUtils.isBlank(topic)) {
                    continue;
                }
                if (StringUtils.isBlank(times)) {
                    log.error("该延时消息未收到延时时间");
                    continue;
                }
                properties.remove(GuavaRocketConstants.GUAVA_TIMES);
                log.info("中转消息uuId {} --topic: {}-- tags: {} #####body:{}", uuid, messageExt.getTopic(), messageExt.getTags(), new String(messageExt.getBody()));
                Message message = new Message();
                message.setTopic(topic);
                if (StringUtils.isNotBlank(tag)) {
                    message.setTags(tag);
                }
                if (StringUtils.isNotBlank(keys)) {
                    message.setKeys(keys);
                }
                if (StringUtils.isNotBlank(uuid)) {
                    message.putUserProperty(GuavaRocketConstants.GUAVA_ORIGINAL_UUID, uuid);
                }
                message.setBody(messageExt.getBody());
                delayMqProducer.sendDelay(message, new Date(Long.parseLong(times)));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        } catch (Exception e) {
            log.error("消息发送失败", e);
        }
        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
    }
}
