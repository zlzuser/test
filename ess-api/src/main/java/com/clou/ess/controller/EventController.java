package com.clou.ess.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.clou.ess.entity.StUserEventSubscribe;
import com.clou.ess.model.EventModel;
import com.clou.ess.model.ResultBody;
import com.clou.ess.service.EventMongoService;
import com.clou.ess.service.StUserEventSubscribeService;
import com.clou.ess.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 事件接口
 * Created by Admin on 2018-03-27.
 */
@RestController
@RequestMapping("/event")
public class EventController {

    @Autowired
    EventMongoService eventMongoService;
    @Autowired
    StUserEventSubscribeService eventSubscribeService;

    /**
     * 当前事件
     *
     * @param userId     用户ID
     * @param deviceType 设备类型
     * @param eventLevel 事件等级
     * @param offset     第几页
     * @param limit      每页的记录数
     * @return
     */
    @GetMapping(value = "/current", headers = "api_version=1.0", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody<List<EventModel>> currentEvents(@RequestHeader("user_id") Integer userId,
                                                      @RequestParam("device_type") String deviceType,
                                                      @RequestParam("event_level") Integer eventLevel,
                                                      @RequestParam Integer offset,
                                                      @RequestParam Integer limit) {

        long beginTime = DateTime.of(DateTime.now().toString("yyyy-MM-dd 00:00:00.000"), DatePattern.NORM_DATETIME_MS_PATTERN).getTime();
        String startTime = String.valueOf(beginTime);
        String endTime = CommonUtil.getNowTime();
        return eventMongoService.findEvents(userId, deviceType, eventLevel, startTime, endTime, offset, limit);
    }

    /**
     * 历史事件
     *
     * @param userId     用户ID
     * @param deviceType 设备类型(-1: 全部)
     * @param eventLevel 事件等级(-1: 全部)
     * @param startTime  起始时间
     * @param endTime    结束时间
     * @param offset     第几页
     * @param limit      每页的记录数
     * @return
     */
    @GetMapping(value = "/query", headers = "api_version=1.0", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody<List<EventModel>> historicalEvents(@RequestHeader("user_id") Integer userId,
                                                         @RequestParam("device_type") String deviceType,
                                                         @RequestParam("event_level") Integer eventLevel,
                                                         @RequestParam("start_time") String startTime,
                                                         @RequestParam("end_time") String endTime,
                                                         @RequestParam Integer offset,
                                                         @RequestParam Integer limit) {
        long endDate = DateTime.of(new DateTime(Long.parseLong(endTime)).toString("yyyy-MM-dd 23:59:59.999"), DatePattern.NORM_DATETIME_MS_PATTERN).getTime();
        endTime = String.valueOf(endDate);
        return eventMongoService.findEvents(userId, deviceType, eventLevel, startTime, endTime, offset, limit);
    }

    /**
     * 获取事件订阅列表
     *
     * @param userId 用户ID
     * @return
     */
    @GetMapping(value = "/subscribe", headers = "api_version=1.0", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody<Map<String, Object>> getEventSubscribe(@RequestHeader("user_id") Integer userId) {
        Map<String, Object> resultMap = new HashMap<>();
        Set<String> deviceTypes = new HashSet<>();
        Set<Integer> eventLevels = new HashSet<>();
        Set<String> eventTypes = new HashSet<>();
        resultMap.put("deviceType", deviceTypes);
        resultMap.put("eventLevel", eventLevels);
        resultMap.put("eventType", eventTypes);
        List<StUserEventSubscribe> subscribes = eventSubscribeService.selectList(new EntityWrapper<StUserEventSubscribe>().eq("USER_ID", userId));
        if (!CollectionUtils.isEmpty(subscribes)) {
            for (StUserEventSubscribe subscribe : subscribes) {
                deviceTypes.add(subscribe.getDeviceType());
                eventLevels.add(subscribe.getEventLevel());
                eventTypes.add(subscribe.getEventType());
            }
        }
        return ResultBody.isOk(resultMap).msg("查询订阅事件成功!");
    }

    /**
     * 事件订阅
     *
     * @param userId 用户ID
     * @return
     */
    @PostMapping(value = "/subscribe", headers = "api_version=1.0", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody eventSubscribe(@RequestHeader("user_id") Integer userId, @RequestBody(required = false) Map<String, Object> params) {
        List<StUserEventSubscribe> subscribes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(params)) {
            List<String> deviceType = new ArrayList<>();
            List<Integer> eventLevel = new ArrayList<>();
            List<String> eventType = new ArrayList<>();
            if (!ObjectUtils.isEmpty(params.get("device_type"))) {
                deviceType = (List<String>) params.get("device_type");
            }
            if (!ObjectUtils.isEmpty(params.get("event_level"))) {
                eventLevel = (List<Integer>) params.get("event_level");
            }
            if (!ObjectUtils.isEmpty(params.get("event_type"))) {
                eventType = (List<String>) params.get("event_type");
            }
            for (int i = 0; i < deviceType.size(); i++) {
                for (int j = 0; j < eventLevel.size(); j++) {
                    for (int k = 0; k < eventType.size(); k++) {
                        StUserEventSubscribe subscribe = new StUserEventSubscribe(userId, deviceType.get(i), eventType.get(k), eventLevel.get(j), "F_YES");
                        subscribes.add(subscribe);
                    }
                }
            }
        }
        boolean flag = eventSubscribeService.updateBatch(userId, subscribes);
        if (flag) {
            return ResultBody.isOk().msg("订阅成功!");
        }
        return ResultBody.isFail().msg("订阅失败!");
    }

}
