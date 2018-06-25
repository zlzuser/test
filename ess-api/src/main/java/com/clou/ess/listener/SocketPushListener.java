package com.clou.ess.listener;

import com.clou.ess.actor.InitSubscriber;
import com.clou.ess.push.SocketDataPush;
import com.clou.ess.push.SocketServer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 监听应用服务启动完毕开启Socket服务端
 */
@Component
public class SocketPushListener implements ApplicationListener<ApplicationReadyEvent> {
    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        SocketServer socketServer = new SocketServer();
        socketServer.start();
        InitSubscriber.getInstance().getSubscriber("RMP_store", "ess", SocketDataPush.getInstance());
    }
}
