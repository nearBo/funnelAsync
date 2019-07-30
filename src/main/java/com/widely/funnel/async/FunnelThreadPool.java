package com.widely.funnel.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nearBo on 2019/7/24.
 * 漏桶算法，实现异步任务，针对调用其他服务有次数限制，执行限流
 */
public class FunnelThreadPool extends ThreadPoolExecutor {

    private AbstractTaskManager asyncTaskManager;

    private FunnelThreadPool(int corePoolSize, int maximumPoolSize,
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
            asyncTaskManager.preSleep();

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
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
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

    class AsyncIntervalTaskManager extends AbstractTaskManager {
        /**
         * 间隔执行方式
         * @param intervalTime
         * @param intervalTimeUnit
         */
        public AsyncIntervalTaskManager(long intervalTime, TimeUnit intervalTimeUnit) {
            this.limitTime = intervalTime;
            this.timeUnit = intervalTimeUnit;
        }

        @Override
        public void afterSleep() throws Exception {
            // 睡眠 等待间隔执行
            if (this.limitTime > 0 && this.timeUnit != null) {
                this.timeUnit.sleep(this.limitTime);
            }
        }
    }

    /**
     * 设置间隔执行方式manager
     * @param limitTime
     * @param timeUnit
     * @return
     */
    public FunnelThreadPool buildAsyncIntervalManager(long limitTime, TimeUnit timeUnit) {
        asyncTaskManager = new AsyncIntervalTaskManager(limitTime, timeUnit);
        return this;
    }

    /**
     * 设置非间隔方式manager
     * @param limitTime
     * @param timeUnit
     * @param limitSize
     * @return
     */
    public FunnelThreadPool buildAsyncManager(long limitTime, TimeUnit timeUnit, int limitSize) {
        int threadSize = super.getCorePoolSize() + super.getMaximumPoolSize();
        asyncTaskManager = new AsyncTaskManager(limitTime, timeUnit, limitSize, threadSize);
        return this;
    }
}
