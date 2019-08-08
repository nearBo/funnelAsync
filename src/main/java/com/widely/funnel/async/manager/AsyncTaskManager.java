package com.widely.funnel.async.manager;

import com.widely.funnel.async.FunnelThreadPoolExecutor;

import java.util.concurrent.TimeUnit;

/**
 * Created by nearBo on 2019/7/31.
 */
public class AsyncTaskManager extends AbstractTaskManager {

    private FunnelThreadPoolExecutor funnelThreadPoolExecutor;

    /**
     * 无间隔执行方式
     * @param limitTime
     * @param timeUnit
     */
    public AsyncTaskManager(long limitTime, TimeUnit timeUnit, int limitSize, FunnelThreadPoolExecutor poolExecutor) {
        this.limitTime = limitTime;
        this.timeUnit = timeUnit;
        this.funnelThreadPoolExecutor = poolExecutor;
        this.limitTimes = limitSize;
    }

    @Override
    public void preSleep() throws Exception {
        this.startTime.compareAndSet(0L, System.currentTimeMillis());
        for (;;) {
            int currentExecuteTimes = this.executeTimes.get();
            if (currentExecuteTimes >= this.limitTimes) {

                long sleepTime = 0L;

                long startExecuteTime = this.startTime.get();
                long currentTime = System.currentTimeMillis();

                long alredyExecuteTime = currentTime - startExecuteTime;
                long limitTimeMills = this.timeUnit.toMillis(this.limitTime);
                // 线程执行次数，wait线程数还原
                reductionLimit();
                if (alredyExecuteTime < limitTimeMills) {
                    sleepTime = limitTimeMills - alredyExecuteTime;
                }
                if (sleepTime > 0) {
                    TimeUnit.MILLISECONDS.wait(sleepTime);
                }
            }
            // 执行次数+1
            this.executeTimes.compareAndSet(currentExecuteTimes, currentExecuteTimes + 1);
        }
    }

    /**
     * wait线程数+1，并且满足条件后线程执行次数，wait线程数还原
     */
    public void reductionLimit() {
        for (;;) {
            int currentExecuteTimes = this.executeTimes.get();
            int waitTreads = this.threadinWaitCount.get();
            threadCount = funnelThreadPoolExecutor.getCorePoolSize() + funnelThreadPoolExecutor.getMaximumPoolSize();
            if (this.threadinWaitCount.compareAndSet(waitTreads, waitTreads + 1)) {
                if (this.threadinWaitCount.get() == this.threadCount && currentExecuteTimes >= this.limitTimes) {
                    // 设置为0，所用线程醒来后将重新计数
                    this.executeTimes.set(0);
                    this.threadinWaitCount.set(0);
                    this.startTime.set(0);
                }
                break;
            }
        }
    }
}
