package com.widely.funnel.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        if (asyncTaskManager != null) {
            long sleeptime = asyncTaskManager.preExecuteCheck();
            if (sleeptime > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleeptime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (asyncTaskManager == null) {
            return;
        }
        // 睡眠 等待间隔执行
        if (asyncTaskManager.getIntervalTime() > 0 && asyncTaskManager.getIntervalTimeUnit() != null) {
            try {
                asyncTaskManager.getIntervalTimeUnit().sleep(asyncTaskManager.getIntervalTime());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
