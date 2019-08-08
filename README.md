使用漏捅算法实现线程池
====================
实现原理：<br/>
   FunnelThreadPoolExecutor继承ThreadPoolExecutor，支持ThreadPoolExecutor的所有方法<br/>
   FunnelThreadPoolExecutor重写newTaskFor(Callable<T> callable)方法，提交任务时对提交任务进行装饰<br/>
   
使用方法：<br/>
   FunnelThreadPoolExecutor executor <br/>
   = new FunnelThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit,blockingQueue,abstractTaskManager);<br/>
   这里 创建AbstractTaskManager实例，每个实例的构造方式可以不同<br/>
   AbstractTaskManager manager  <br/>
   = new AsyncTaskManager(long limitTime, TimeUnit timeUnit, int limitSize, executor)
   <br/>
    一直执行，直到达到limitsize--》等待下个时间单位<br/>
   executor.submit(callable, manager);<br/>
   
   同一个线程池只支持一种任务的管理，避免不同任务之间相互影响
   ============================================================
