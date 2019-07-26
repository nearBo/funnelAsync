package com.widely.funnel.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangbo23 on 2019/7/24.
 * 漏桶算法，实现异步任务，针对调用其他服务有次数限制，执行限流
 */
public class FunnelThreadPool extends ThreadPoolExecutor {

    private AbstractTaskManager asyncTaskManager;

    public FunnelThreadPool(int corePoolSize, int maximumPoolSize,
                             long keepAliveTime, TimeUnit timeUnit,
                             BlockingQueue<Runnable> blockingQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, blockingQueue);
    }

    public Future<?> funnelSubmit(Callable<?> callable) {
        return super.submit(funnelCallable(callable));
    }

    /**
     * 装饰提交的任务
     * @param callable
     * @param <T>
     * @return
     */
    private <T> Callable<T> funnelCallable(Callable<T> callable) {
        return new FunnelCallable<T>(callable, asyncTaskManager);
    }

    class FunnelCallable<T> implements Callable<T> {

        private AbstractTaskManager asyncTaskManager;
        private Callable<T> callable;

        public FunnelCallable(Callable<T> callable, AbstractTaskManager asyncTaskManager) {
            this.asyncTaskManager = asyncTaskManager;
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            if (asyncTaskManager == null) {
                throw new Exception("AsyncTaskManager not init");
            }
            long sleepTime = asyncTaskManager.preSleep();
            if (sleepTime > 0) {
                TimeUnit.MICROSECONDS.sleep(sleepTime);
            }
            callable.call();
            // 睡眠 等待间隔执行
            asyncTaskManager.afterSleep();

            return null;
        }
    }

    class AsyncTaskManager extends AbstractTaskManager {
        /**
         * 无间隔执行方式
         * @param limitTime
         * @param timeUnit
         */
        public AsyncTaskManager(long limitTime, TimeUnit timeUnit, int limitSize, int threadCount) {
            this.limitTime = limitTime;
            this.timeUnit = timeUnit;
            this.threadCount = threadCount;
            this.limitTimes = limitSize;
        }
    }

    class AsyncIntervalTaskManager extends AbstractTaskManager {
        /**
         * 间隔执行方式
         * @param limitTime
         * @param timeUnit
         * @param intervalTime
         * @param intervalTimeUnit
         */
        public AsyncIntervalTaskManager(long limitTime, TimeUnit timeUnit, long intervalTime, TimeUnit intervalTimeUnit, int threadCount) {
            this.limitTime = limitTime;
            this.timeUnit = timeUnit;
            this.threadCount = threadCount;
            this.intervalTime = intervalTime;
            this.intervalTimeUnit = intervalTimeUnit;
        }
    }

    private long limitTime;
    private TimeUnit limitTimeUnit;

    private long intervalTime;
    private TimeUnit intervalTimeUnit;

    private int limitSize;
    private boolean asyncType = false;

    public FunnelThreadPool setLimitTime(long limitTime, TimeUnit timeUnit) {
        this.limitTime = limitTime;
        this.limitTimeUnit = timeUnit;
        return this;
    }

    public FunnelThreadPool setLimitSize(int size) {
        this.limitSize = size;
        return this;
    }

    public FunnelThreadPool setIntervalTime(long intervalTime, TimeUnit timeUnit) {
        this.intervalTime = intervalTime;
        this.intervalTimeUnit = timeUnit;
        return this;
    }

    public FunnelThreadPool setAsyncType(boolean isInterval) {
        this.asyncType = isInterval;
        return this;
    }

    public FunnelThreadPool build() {
        int threadSize = super.getCorePoolSize() + super.getMaximumPoolSize();
        if (asyncType) {
            asyncTaskManager = new AsyncIntervalTaskManager(limitTime, limitTimeUnit, intervalTime, intervalTimeUnit, threadSize);
        } else {
            asyncTaskManager = new AsyncTaskManager(limitTime, limitTimeUnit, limitSize, threadSize);
        }
        return this;
    }
}
