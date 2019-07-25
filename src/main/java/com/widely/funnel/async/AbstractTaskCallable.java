package com.widely.funnel.async;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangbo23 on 2019/7/24.
 */
public abstract class AbstractTaskCallable<V> implements Callable<V> {

    private AsyncTaskManager asyncTaskManager;

    @Override
    public V call() throws Exception {
        asyncTaskManager = getAbstractAsyncTaskManager();
        if (asyncTaskManager == null) {
            throw new Exception("AsncTtaskManager not init");
        }
        long sleeptime = asyncTaskManager.preExecuteCheck();
        if (sleeptime > 0) {
            TimeUnit.MILLISECONDS.sleep(sleeptime);
        }
        // 进行计数逻辑
        V v = taskHandler();
        // 睡眠 等待间隔执行
        if (asyncTaskManager.getIntervalTime() > 0 && asyncTaskManager.getIntervalTimeUnit() != null) {
            asyncTaskManager.getIntervalTimeUnit().sleep(asyncTaskManager.getIntervalTime());
        }
        return v;
    }

    /**
     * 子类实现自己的业务
     * @return
     */
    public abstract V taskHandler();

    /**
     * 必须实现此方法
     * @return
     */
    public abstract AsyncTaskManager getAbstractAsyncTaskManager();
}
