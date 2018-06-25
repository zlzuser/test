package com.clou.ess.actor;

import akka.actor.UntypedActor;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/26 10:40
 */
public class InfoRouterActor extends UntypedActor {

    private InfoConsumer consumer;

    public InfoRouterActor(InfoConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        consumer.consumeInfo(msg);
    }
}
