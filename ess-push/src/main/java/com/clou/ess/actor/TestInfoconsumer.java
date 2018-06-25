package com.clou.ess.actor;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/26 11:16
 */
public class TestInfoconsumer implements InfoConsumer {
    @Override
    public void consumeInfo(Object info) {
        System.out.println("接收到消息：######################"+info.toString());
    }

}
