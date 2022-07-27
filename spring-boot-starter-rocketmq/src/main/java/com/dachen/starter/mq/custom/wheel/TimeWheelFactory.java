package com.dachen.starter.mq.custom.wheel;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author : Guava
 * @version 1.0
 * @projectName：civism-rocket
 * @className：TimeWheelFactory
 * @date 2020/1/9 10:38 上午
 * @return
 */
public class TimeWheelFactory {

    private static HashedWheelTimer hashedWheelTimer;

    public static HashedWheelTimer getInstance() {
        if (hashedWheelTimer == null) {
            synchronized (TimeWheelFactory.class) {
                if (hashedWheelTimer == null) {
                    hashedWheelTimer = new HashedWheelTimer(new DefaultThreadFactory("wechat-timer"), 100,
                            TimeUnit.MILLISECONDS, 1024, false);
                }
            }
        }
        hashedWheelTimer.start();
        return hashedWheelTimer;
    }

    public Timeout newTask(TimerTask timerTask, long delay, TimeUnit timeUnit) {
        return hashedWheelTimer.newTimeout(timerTask, delay, timeUnit);
    }

    public static void stop() {
        if (hashedWheelTimer != null) {
            hashedWheelTimer.stop();
        }
    }
}
