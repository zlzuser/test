package com.clou.ess.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ess.socket.push.threadpool")
public class SocketPushProperties {

    /**
     * 线程池维护线程的最小数量
     */
    private int corePoolSize = 5;
    /**
     * 线程池维护线程的最大数量
     */
    private int maximumPoolSize = 20;
    /**
     * 线程池维护线程所允许的空闲时间
     */
    private int keepAliveTime = 0;
    /**
     * 线程池所使用的缓冲队列大小
     */
    private int workQueueSize = 10;
    /**
     * Socket连接超时时间
     */
    private int socketTimeOut = 20;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getWorkQueueSize() {
        return workQueueSize;
    }

    public void setWorkQueueSize(int workQueueSize) {
        this.workQueueSize = workQueueSize;
    }

    public int getSocketTimeOut() {
        return socketTimeOut;
    }

    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }
}
