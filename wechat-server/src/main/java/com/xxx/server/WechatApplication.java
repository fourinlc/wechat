package com.xxx.server;

import com.dachen.starter.mq.annotation.EnableMQConfiguration;
import com.dachen.starter.mq.custom.consumer.DelayMqConsumer;
import com.dachen.starter.mq.custom.producer.DelayMqProducer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * @PackageName:com.xxx.server
 * @ClassName:wechatApplication Description:
 * @author: lc
 * @date 2022/7/14 17:57
 */
@SpringBootApplication
@MapperScan("com.xxx.server.mapper")
@EnableMQConfiguration
// 导入自定义延时消息组件信息
@Import({DelayMqProducer.class, DelayMqConsumer.class})
public class WechatApplication {
    public static void main(String[] args) {
        SpringApplication.run(WechatApplication.class,args);
    }
}
