package com.widely.funnel.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangbo23 on 2019/7/24.
 * 漏桶算法，实现异步任务，针对调用其他服务有次数限制，执行限流
 */
public class FunnelThreadPool extends ThreadPoolExecutor {

    private AsyncTaskManager asyncTaskManager;

    public FunnelThreadPool(int corePoolSize, int maximumPoolSize,
                             long keepAliveTime, TimeUnit timeUnit,
                             BlockingQueue<Runnable> blockingQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, blockingQueue);
    }

    private <T> Callable<T> newCallable(Callable<T> callable) {
        return new FunnelCallable<T>(callable, asyncTaskManager);
    }

    /**
     * 重写该方法
     * @param callable
     * @param <T>
     * @return
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return super.newTaskFor(newCallable(callable));
    }

    class FunnelCallable<T> implements Callable<T> {

        private AsyncTaskManager asyncTaskManager;
        private Callable<T> callable;

        public FunnelCallable(Callable<T> callable, AsyncTaskManager asyncTaskManager) {
            this.asyncTaskManager = asyncTaskManager;
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            if (asyncTaskManager == null) {
                throw new Exception("AsyncTaskManager not init");
            }
            // TODO 业务处理
            callable.call();

            // TODO 业务处理
            return null;
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
            asyncTaskManager = new AsyncTaskManager(limitTime, limitTimeUnit, intervalTime, intervalTimeUnit, threadSize);
        } else {
            asyncTaskManager = new AsyncTaskManager(limitTime, limitTimeUnit, limitSize, threadSize);
        }
        return this;
    }
}
