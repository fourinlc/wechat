package com.dachen.starter.mq.custom.producer;

import com.dachen.starter.mq.annotation.MQProducer;
import com.dachen.starter.mq.base.AbstractMQProducer;
import com.dachen.starter.mq.custom.calculate.DelayLevelCalculate;
import com.dachen.starter.mq.custom.constant.GuavaRocketConstants;
import com.dachen.starter.mq.custom.wheel.SendRealMqTask;
import com.dachen.starter.mq.custom.wheel.TimeWheelFactory;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @className：DelayMqProducer
 * 自定义延时消息处理
 * @return
 */
@Slf4j
@MQProducer
public class DelayMqProducer extends AbstractMQProducer {

    @Value("${spring.rocketmq.proxy-topic}")
    private String proxyTopic;

    /**
     * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h rocketMQ自动支持18个级别 等级全部转化为秒
     */
    public SendResult sendDelay(Message msg, Date startSendTime) throws InterruptedException {
        if (startSendTime == null) {
            return syncSend(msg);
        }
        long l = Duration.between(Instant.now(), startSendTime.toInstant()).getSeconds();
        //如果不等于0,说明设置了延时等级，直接用rocketMQ支持的发送
        if (l <= GuavaRocketConstants.TIME_OUT) {
            SendRealMqTask sendRealMqTask = getSendRealMqTask(msg, l);
            return sendRealMqTask.getResult();
        } else {
            Integer level = DelayLevelCalculate.calculateDefault(l);
            fillMessage(msg, level, startSendTime);
            return syncSend(msg);
        }
    }

    private SendRealMqTask getSendRealMqTask(Message msg, long l) throws InterruptedException {
        HashedWheelTimer instance = TimeWheelFactory.getInstance();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        SendRealMqTask sendRealMqTask = new SendRealMqTask();
        sendRealMqTask.setDelayMqProducer(this);
        sendRealMqTask.setMessage(msg);
        sendRealMqTask.setCountDownLatch(countDownLatch);
        instance.newTimeout(sendRealMqTask, l < 0 ? 1 : l, TimeUnit.SECONDS);
        countDownLatch.await();
        return sendRealMqTask;
    }


    /**
     * 填充消息
     *
     * @param msg 消息体
     * @param level 延时等级
     * @param startSendTime 发送时间
     */
    private void fillMessage(Message msg, Integer level, Date startSendTime) {
        msg.putUserProperty(GuavaRocketConstants.GUAVA_TIMES, String.valueOf(startSendTime.getTime()));
        String topic = msg.getProperty(GuavaRocketConstants.GUAVA_ORIGINAL_TOPIC);
        if (StringUtils.isBlank(topic)) {
            msg.putUserProperty(GuavaRocketConstants.GUAVA_ORIGINAL_TOPIC, msg.getTopic());
        }
        String tag = msg.getProperty(GuavaRocketConstants.GUAVA_ORIGINAL_TAG);
        if (StringUtils.isBlank(tag)) {
            if (StringUtils.isNotBlank(msg.getTags())) {
                msg.putUserProperty(GuavaRocketConstants.GUAVA_ORIGINAL_TAG, msg.getTags());
            }
        }
        String keys = msg.getProperty(GuavaRocketConstants.GUAVA_ORIGINAL_KEY);
        if (StringUtils.isBlank(keys)) {
            if (StringUtils.isNotBlank(msg.getKeys())) {
                msg.putUserProperty(GuavaRocketConstants.GUAVA_ORIGINAL_KEY, msg.getKeys());
            }
        }

        String property = msg.getProperty(GuavaRocketConstants.GUAVA_ORIGINAL_UUID);
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (StringUtils.isBlank(property)) {
            msg.putUserProperty(GuavaRocketConstants.GUAVA_ORIGINAL_UUID, uuid);
        }
        msg.setDelayTimeLevel(level);
        msg.setTopic(proxyTopic);
        log.info("消息uuid {} 开发发送时间为{},延时等级本次建议为{}", uuid, String.format("%tF %<tT", new Date()), level);
    }

}
