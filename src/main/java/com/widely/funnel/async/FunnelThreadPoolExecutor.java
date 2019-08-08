package com.widely.funnel.async;

import com.widely.funnel.async.manager.AbstractTaskManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by nearBo on 2019/7/24.
 * 漏桶算法，实现异步任务，针对调用其他服务有次数限制，执行限流
 */
public class FunnelThreadPoolExecutor extends ThreadPoolExecutor {

    private AbstractTaskManager abstractTaskManager;

    public FunnelThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                    long keepAliveTime, TimeUnit timeUnit,
                                    BlockingQueue<Runnable> blockingQueue, AbstractTaskManager abstractTaskManager) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, blockingQueue);
        this.abstractTaskManager = abstractTaskManager;
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        Callable newCallable = funnelCallable(callable, abstractTaskManager);
        return super.newTaskFor(newCallable);
    }

    /**
     * 装饰提交的任务
     * @param callable
     * @param <T>
     * @return
     */
    private <T> Callable<T> funnelCallable(Callable<T> callable, AbstractTaskManager asyncTaskManager) {
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
}
