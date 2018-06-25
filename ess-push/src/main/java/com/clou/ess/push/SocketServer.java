package com.clou.ess.push;

import com.clou.ess.util.Global;
import com.clou.ess.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/19 9:35
 */
public class SocketServer implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private static ServerSocket server;

    private static final String threadName = "Ess dateSocketServer";

    private Thread thread;

    private ThreadPoolManager threadPoolManager = (ThreadPoolManager) SpringContextUtil.getBean(ThreadPoolManager.class);

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(Global.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        acceptMassage();
    }

    private void acceptMassage() {
        Socket socket = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("{}{}", threadName, " Start...");
            }
            socket = server.accept();
            if (logger.isDebugEnabled()) {
                logger.debug("User：{}", socket.getRemoteSocketAddress());
            }
            threadPoolManager.accessSocket(socket);
            acceptMassage();
        } catch (IOException e) {
            try {
                if (null != socket) {
                    socket.close();
                }
            } catch (IOException e1) {
            }
            socket = null;
        }
    }

}
