package com.dachen.starter.mq.custom.wheel;

import com.dachen.starter.mq.custom.constant.GuavaRocketConstants;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * @author : Guava
 * @version 1.0
 * @projectName：civism-rocket
 * @className：SendRealMqTask
 * @date 2020/1/9 1:39 下午
 * @return
 */
@Slf4j
public class SendRealMqTask implements TimerTask {

    private DelayMqProducer delayMqProducer;

    private Message         message;

    private CountDownLatch  countDownLatch;

    private SendResult      sendResult;

    @Override
    public void run(Timeout timeout) throws Exception {
        validate();
        toRealMessage(message);
        sendResult = delayMqProducer.syncSend(message);
        log.info("实际Mq投递时间{} 发送成功", String.format("%tF %<tT", new Date()));
        countDownLatch.countDown();
    }

    public SendResult getResult() {
        return sendResult;
    }

    private void validate() {
        if (delayMqProducer == null) {
            throw new IllegalArgumentException("producer is  null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is  null");
        }
        if (countDownLatch == null) {
            throw new IllegalArgumentException("countDownLatch is null");
        }
    }

    private void toRealMessage(Message message) {
        String topic = message.getProperty(GuavaRocketConstants.GUAVA_ORIGINAL_TOPIC);
        if (StringUtils.isNotBlank(topic)) {
            message.setTopic(topic);
        }
        String tag = message.getProperty(GuavaRocketConstants.GUAVA_ORIGINAL_TAG);
        if (StringUtils.isNotBlank(tag)) {
            message.setTags(tag);
        }
        String key = message.getProperty(GuavaRocketConstants.GUAVA_ORIGINAL_KEY);
        if (StringUtils.isNotBlank(key)) {
            message.setKeys(key);
        }
    }

    public DelayMqProducer getDelayMqProducer() {
        return delayMqProducer;
    }

    public void setDelayMqProducer(DelayMqProducer delayMqProducer) {
        this.delayMqProducer = delayMqProducer;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
}
