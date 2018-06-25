package com.clou.ess.controller;

import com.clou.ess.service.GlobalEhchace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/5/16 17:29
 */
@RestController
@RequestMapping("/ehcache")
public class Testcache {

    @Autowired
    private GlobalEhchace globalEhchace;

    @GetMapping(value="/input",headers = "api_version=1.0")
    public void testInputEhcache(@RequestHeader("user_id") Integer userId){
        Map<String, List<Map<String, Object>>> pushMap = new HashMap<>();
        List<Map<String, Object>> stationPushList = new ArrayList<>();
        Map<String, Object> stationPushs = new HashMap<>();
        stationPushs.put("aaa","dasdada");
        stationPushList.add(stationPushs);
        pushMap.put("STATIONS_BMS",stationPushList);
        globalEhchace.inputByUserid(String.valueOf(userId),pushMap);
        System.out.println("***************插入缓存*****************");
    }

    @GetMapping(value="/delete",headers = "api_version=1.0")
    public void testDeleteEhcache(@RequestHeader("user_id") Integer userId){
        globalEhchace.deleteByUserId(String.valueOf(userId));
        System.out.println("***************删除缓存***************");
    }

}
