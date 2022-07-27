package com.xxx.server.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dachen.starter.mq.base.MessageBuilder;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class DelayTest {

    private DelayMqProducer delayMqProducer;

    /**
     * 延时消息测试
     * @throws InterruptedException
     */
    /*@PostConstruct*/
    public void test() throws InterruptedException {
        Map<String, String> content = new HashMap<>();
        content.put("name", "guava");
        content.put("message", "hello word");
        Message message = new Message("guava_hello_topic", null, JSON.toJSONBytes(content));
        delayMqProducer.sendDelay(message, DateUtils.addSeconds(new Date(), 35));
    }
}
