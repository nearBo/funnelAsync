package com.widely.funnel.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by zhangbo23 on 2019/7/24.
 */
public class AsyncTaskManager {
    /*** 限制时间长度 ***/
    private long limitTime;
    /*** 时间单位 ***/
    private TimeUnit timeUnit;
    /*** 限制时间内执行次数 ***/
    private int limitTimes;
    /*** 间隔执行时间 ***/
    private long intervalTime;
    /*** 间隔执行时间单位 ***/
    private TimeUnit intervalTimeUnit;
    /*** 线程数量 ***/
    private int threadCount;

    /**
     * 间隔执行方式
     * @param limitTime
     * @param timeUnit
     * @param intervalTime
     * @param intervalTimeUnit
     */
    public AsyncTaskManager(long limitTime, TimeUnit timeUnit, long intervalTime, TimeUnit intervalTimeUnit, int threadCount) {
        this.limitTime = limitTime;
        this.timeUnit = timeUnit;
        this.threadCount = threadCount;
        this.intervalTime = intervalTime;
        this.intervalTimeUnit = intervalTimeUnit;
    }

    /**
     * 无间隔执行方式
     * @param limitTime
     * @param timeUnit
     * @param limitTimes
     * @param threadCount
     */
    public AsyncTaskManager(long limitTime, TimeUnit timeUnit, int limitTimes, int threadCount) {
        this.limitTime = limitTime;
        this.timeUnit = timeUnit;
        this.limitTimes = limitTimes;
        this.threadCount = threadCount;
    }


    /*** 第一个线程第一个开始时间 ***/
    private AtomicLong startTime = new AtomicLong(0);
    /*** 记录执行次数 ***/
    private AtomicInteger executeTimes = new AtomicInteger(0);
    /*** 进入wait线程个数 ***/
    private AtomicInteger threadinWaitCount = new AtomicInteger(0);

    /**
     * 设置第一个线程第一次开始的时间
     */
    public void setStartTime() {
        this.startTime.compareAndSet(0L, System.currentTimeMillis());
    }

    /**
     * 判断是否允许进入下次执行，执行次数+1
     * @return 返回0，可以进入下次执行，否者wait(result)
     */
    public long preExecuteCheck() {
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

    public long getLimitTime() {
        return limitTime;
    }

    public void setLimitTime(long limitTime) {
        this.limitTime = limitTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public int getLimitTimes() {
        return limitTimes;
    }

    public void setLimitTimes(int limitTimes) {
        this.limitTimes = limitTimes;
    }

    public long getIntervalTime() {
        return intervalTime;
    }

    public void setIntervalTime(long intervalTime) {
        this.intervalTime = intervalTime;
    }

    public TimeUnit getIntervalTimeUnit() {
        return intervalTimeUnit;
    }

    public void setIntervalTimeUnit(TimeUnit intervalTimeUnit) {
        this.intervalTimeUnit = intervalTimeUnit;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
}
