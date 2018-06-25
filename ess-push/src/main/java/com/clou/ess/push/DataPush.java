package com.clou.ess.push;

import com.clou.ess.util.Global;
import com.clou.ess.util.SpringContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/23 11:45
 */
public class DataPush {

    private final Logger logger = LoggerFactory.getLogger(AccessGlobalSocket.class);

    ObjectMapper objectMapper;

    public DataPush() {
        this.objectMapper = (ObjectMapper) SpringContextUtil.getBean(ObjectMapper.class);
    }

    public void dataPush(Integer userId, Socket socket, String json) {
        DataOutputStream dataOutputStream = null;
        if (!ObjectUtils.isEmpty(socket) && !ObjectUtils.isEmpty(json)) {
            try {
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(json+"\r\n");
                dataOutputStream.flush();
            } catch (IOException e) {
                try {
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                } catch (IOException e1) {
                }
                Global.scoketMap.remove(userId);
                logger.error("推送数据异常:{}", e);
            }
        }
    }
}
