package com.xxx.server;

import com.dachen.starter.mq.annotation.EnableMQConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @PackageName:com.xxx.server
 * @ClassName:wechatApplication Description:
 * @author: lc
 * @date 2022/7/14 17:57
 */
@SpringBootApplication
@MapperScan("com.xxx.server.mapper")
@EnableMQConfiguration
@EnableCaching
@EnableAspectJAutoProxy
public class WechatApplication {
    public static void main(String[] args) {
        SpringApplication.run(WechatApplication.class,args);
    }
}
