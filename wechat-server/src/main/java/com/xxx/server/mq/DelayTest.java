package com.xxx.server.mq;

import com.alibaba.fastjson.JSON;
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
    @PostConstruct
    public void test() throws InterruptedException {
        Map<String, String> content = new HashMap<>();
        content.put("name", "new wechat");
        content.put("message", "hello word test 222");
        Message message = new Message("wechat", "qunChat", JSON.toJSONBytes(content));
        delayMqProducer.sendDelay(message, DateUtils.addSeconds(new Date(), 35));
    }
}
