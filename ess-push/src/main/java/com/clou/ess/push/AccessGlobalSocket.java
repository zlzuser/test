package com.clou.ess.push;

import com.clou.ess.util.Global;
import com.clou.ess.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/4/3 15:42
 */
public class AccessGlobalSocket extends DataPush implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(AccessGlobalSocket.class);

    private Socket socket;

    private SocketPushProperties properties;

    private static Long beginTime = null;

    private static Long endTime = null;

    public AccessGlobalSocket(Socket socket) {
        this.socket = socket;
        this.properties = (SocketPushProperties) SpringContextUtil.getBean(SocketPushProperties.class);
    }

    @Override
    public void run() {
        beginTime = System.currentTimeMillis();
        BufferedReader input = null;
        Integer userId = null;
        Boolean exit = true;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = null;
            while (exit) {
                line = input.readLine();
                //设置超时时间，避免客户端socket异常(开发工具停止运行导致)，但未正常关闭，服务端监测不到异常，并一直读取到null，占用线程池资源
                endTime = System.currentTimeMillis();
                Long timeOut = endTime - beginTime;
                if (timeOut > (properties.getSocketTimeOut() * 1000)) {
                    closeStream(input, socket, userId);
                    exit = false;
                    if (logger.isDebugEnabled()) {
                        long times = (long) Math.ceil(timeOut / 1000l);
                        logger.debug("读取内容超时 ==> 用时:{}秒，设置超时时间为{}秒。", times, properties.getSocketTimeOut());
                    }
                }
                if (!ObjectUtils.isEmpty(line)) {
                    beginTime = System.currentTimeMillis();
                    if (logger.isDebugEnabled()) {
                        logger.debug("读取客户端发送的数据:{}", line);
                    }

                    if (!"ok".equals(line)) {
                        try {
                            Map map = objectMapper.readValue(line, Map.class);
                            if (!ObjectUtils.isEmpty(map) && null != map.get("user_id")) {
                                userId = Integer.valueOf(map.get("user_id") + "");
                                Global.scoketMap.put(userId, socket);
                                if ("YES".equals(map.get("close") + "")) {
                                    closeStream(input, socket, userId);
                                    exit = false;
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("客户端用户【{}】已关闭Socket连接。", userId);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("解析客户端数据转JSON异常，客户端数据:{}", line);
                            }
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("socket读取超时………………………………………………………………………………");
            }
            closeStream(input, socket, userId);
        } catch (IOException e) {
            closeStream(input, socket, userId);
            if (logger.isDebugEnabled()) {
                logger.debug("socket IO异常………………………………………………………………………………");
            }
        }
    }

    private void closeStream(BufferedReader br, Socket sk, int userId) {
        try {
            if (!ObjectUtils.isEmpty(br)) {
                br.close();
            }
            if (!ObjectUtils.isEmpty(sk)) {
                sk.close();
                sk = null;
            }
        } catch (IOException e) {

        }
        Global.scoketMap.remove(userId);
    }
}