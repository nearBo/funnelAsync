package com.widely.funnel.async.manager;

import java.util.concurrent.TimeUnit;

/**
 * Created by nearBo on 2019/7/31.
 */
public class AsyncIntervalTaskManager extends AbstractTaskManager {
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
            this.timeUnit.wait(this.limitTime);
        }
    }
}
