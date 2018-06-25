package com.clou.ess.push;

import org.omg.CORBA.IMP_LIMIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription : socket线程池
 * @since :2018/4/3 16:02
 */
@Component
@EnableConfigurationProperties(SocketPushProperties.class)
public class ThreadPoolManager {

    private final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);

    private final SocketPushProperties properties;

    private final ThreadPoolExecutor threadPoolExecutor;
    //private static ThreadPoolManager threadPoolManager = new ThreadPoolManager();

    //消息队列
    Queue<Runnable> queueList = new LinkedList<>();

    public ThreadPoolManager(SocketPushProperties properties) {
        this.properties = properties;
        this.scheduledExecutorService.scheduleAtFixedRate(bufferTast, 0, 1, TimeUnit.SECONDS);
        this.threadPoolExecutor = new ThreadPoolExecutor(properties.getCorePoolSize(), properties.getMaximumPoolSize(),
                properties.getKeepAliveTime(), TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(properties.getWorkQueueSize()),new SockThreadFactory(), rejectedExecutionHandler);

    }

    /*public static ThreadPoolManager newInstance() {
        return threadPoolManager;
    }*/

    final Runnable bufferTast = new Runnable() {
        @Override
        public void run() {
            if (hasBufferTask(queueList)) {
                AccessGlobalSocket accessGlobalSocket = (AccessGlobalSocket) queueList.poll();
                threadPoolExecutor.execute(accessGlobalSocket);
            }
        }
    };

    final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    final RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.info("线程最大数超过：" + (properties.getMaximumPoolSize() + properties.getWorkQueueSize()) + ",将尝试放入队列中等待执行！");
            if (!queueList.offer((AccessGlobalSocket) r)) {
                logger.error("-------放入缓存队列失败，请重新请求任务--------");
            }
        }
    };

    public void accessSocket(Socket socket) {
        AccessGlobalSocket accessGlobalSocket = new AccessGlobalSocket(socket);
        threadPoolExecutor.execute(accessGlobalSocket);
    }

    private Boolean hasBufferTask(Queue<Runnable> queueList) {
        return !queueList.isEmpty();
    }

    private class SockThreadFactory implements ThreadFactory {

        private AtomicInteger count = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(ThreadPoolManager.class.getSimpleName()+count.addAndGet(1));
            return t;
        }
    }

}
