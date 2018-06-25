package com.clou.ess.util;

import com.clou.ess.entity.StPushRecord;
import com.clou.ess.model.CommonModel;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/19 10:17
 */
public class Global {

    /**
     * 数据时效性
     */
    public static final Integer timeLiness = -2;

    /**
     * ServerSocket服务端口
     */
    public static final Integer port = 6375;

    /**
     * AppID
     */
    public static final String appId = "ndEHWndini8qqeuMT9pZl2";

    /**
     * AppKey
     */
    public static final String appKey = "gkMVbJQigY6dS1iARW9sH6";

    /**
     * AppMasterSecret
     */
    public static final String masterSecret = "p8UiHJIPpW8N0bFhy1jXF1";

    /**
     * 客户端连接
     */
    public static final ConcurrentHashMap<Integer, Socket> scoketMap = new ConcurrentHashMap<>();

    /**
     * 用户数据项
     */
    public static final ConcurrentHashMap<Integer, Map<String, List<Map<String, Object>>>> dateMap = new ConcurrentHashMap<Integer, Map<String, List<Map<String, Object>>>>();

    /**
     * 用户辅助数据项datalist
     */
    public static final ConcurrentHashMap<Integer, Map<String, List<CommonModel>>> dataList = new ConcurrentHashMap<>();

    /**
     * 推送记录
     */
    public static CopyOnWriteArrayList<StPushRecord> pushRecords = new CopyOnWriteArrayList<>();

}
