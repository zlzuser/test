package com.clou.ess.actor;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/26 10:42
 */
public interface InfoConsumer {

    /**
     * 消息接受者 每一个消息接受者都要继承此类
     * @param info
     */
    public void consumeInfo(Object info);


}
