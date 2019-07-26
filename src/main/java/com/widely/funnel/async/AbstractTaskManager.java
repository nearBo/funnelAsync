package com.widely.funnel.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by zhangbo23 on 2019/7/26.
 */
public abstract class AbstractTaskManager {
    /*** 限制时间长度 ***/
    protected long limitTime;
    /*** 时间单位 ***/
    protected TimeUnit timeUnit;
    /*** 限制时间内执行次数 ***/
    protected int limitTimes;
    /*** 间隔执行时间 ***/
    protected long intervalTime;
    /*** 间隔执行时间单位 ***/
    protected TimeUnit intervalTimeUnit;
    /*** 线程数量 ***/
    protected int threadCount;


    /*** 第一个线程第一个开始时间 ***/
    private AtomicLong startTime = new AtomicLong(0);
    /*** 记录执行次数 ***/
    private AtomicInteger executeTimes = new AtomicInteger(0);
    /*** 进入wait线程个数 ***/
    private AtomicInteger threadinWaitCount = new AtomicInteger(0);


    /**
     * 线程在给定时间单位内达到调用限制，将会睡眠
     * @return
     */
    public long preSleep() throws Exception {

        startTime.compareAndSet(0L, System.currentTimeMillis());
        for (;;) {
            int currentExecuteTimes = executeTimes.get();
            if (currentExecuteTimes >= limitTimes) {
                long startExecuteTime = startTime.get();
                long currentTime = System.currentTimeMillis();

                long alredyExecuteTime = currentTime - startExecuteTime;
                long limitTimeMills = timeUnit.toMillis(limitTime);
                // 线程执行次数，wait线程数还原
                reductionLimit();
                if (alredyExecuteTime < limitTimeMills) {
                    return limitTimeMills - alredyExecuteTime;
                } else {
                    return 0L;
                }
            }
            // 执行次数+1
            if (currentExecuteTimes < limitTimes
                    && executeTimes.compareAndSet(currentExecuteTimes, currentExecuteTimes + 1)) {
                return 0L;
            }
        }
    }

    /**
     * wait线程数+1，并且满足条件后线程执行次数，wait线程数还原
     */
    private void reductionLimit() {
        for (;;) {
            int currentExecuteTimes = executeTimes.get();
            int waitTreads = threadinWaitCount.get();
            if (threadinWaitCount.compareAndSet(waitTreads, waitTreads + 1)) {
                if (threadinWaitCount.get() == threadCount && currentExecuteTimes >= limitTimes) {
                    // 设置为0，所用线程醒来后将重新计数
                    executeTimes.set(0);
                    threadinWaitCount.set(0);
                    startTime.set(0);
                }
                break;
            }
        }
    }

    public void afterSleep() throws Exception {
        // 睡眠 等待间隔执行
        if (intervalTime > 0 && intervalTimeUnit != null) {
            intervalTimeUnit.sleep(intervalTime);
        }
    }
}
