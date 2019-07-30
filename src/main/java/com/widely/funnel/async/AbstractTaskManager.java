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
    protected AtomicLong startTime = new AtomicLong(0);
    /*** 记录执行次数 ***/
    protected AtomicInteger executeTimes = new AtomicInteger(0);
    /*** 进入wait线程个数 ***/
    protected AtomicInteger threadinWaitCount = new AtomicInteger(0);


    /**
     * 线程在给定时间单位内达到调用限制，将会睡眠
     * @return
     */
    public void preSleep() throws Exception {

    };

    public void afterSleep() throws Exception {

    }
}
