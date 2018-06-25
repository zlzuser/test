package com.clou.ess.controller;

import com.clou.ess.entity.StDocStation;
import com.clou.ess.entity.StationsIncome;
import com.clou.ess.model.ResultBody;
import com.clou.ess.service.StDocStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/6/4 15:49
 */
@RestController
@RequestMapping("/station")
public class RunOverviewController {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String endtime = sdf.format(date);


    @Autowired
    StDocStationService stDocStationService;

    @RequestMapping("/runview")
    public ResultBody getRunViewData() {
        List<StDocStation> stationList = stDocStationService.countStationTypeAndCapacity(endtime);
        float sumcapacity = 0f;
        float yftg_capacity = 0f;
        float tp_capacity = 0f;
        float gc_capacity = 0f;
        float fgc_capacity = 0f;
        float fc_capacity = 0f;
        Map<String, Float> map = new HashMap<>();
        for (StDocStation stDocStation : stationList) {
            float capacity = stDocStation.getStationCapacity().floatValue();
            String type = stDocStation.getStationType();
            if ("YFTG".equals(type)) {
                yftg_capacity += capacity;
            } else if ("TP".equals(type)) {
                tp_capacity += capacity;
            } else if ("GC".equals(type)) {
                gc_capacity += capacity;
            } else if ("FC".equals(type)) {
                fc_capacity += capacity;
            } else if ("FGC".equals(type)) {
                fgc_capacity += capacity;
            }
            sumcapacity += capacity;
        }
        map.put("yftg_capacity", yftg_capacity);
        map.put("tp_capacity", tp_capacity);
        map.put("gc_capacity", gc_capacity);
        map.put("fc_capacity", fc_capacity);
        map.put("fgc_capacity", fgc_capacity);
        map.put("sum_capacity", sumcapacity);
        return ResultBody.isOk().body(map);
    }


    @RequestMapping("/incomelist")
    public ResultBody getIncomeData(){
        List<StationsIncome> stationIncomeList = stDocStationService.getAllStationIncome(endtime);
        float sum_income = 0f;
        float yftg_income = 0f;
        float tp_income = 0f;
        float gc_income = 0f;
        float fgc_income = 0f;
        float fc_income = 0f;
        Map<String, Float> map = new HashMap<>();
        for(StationsIncome stationsIncome : stationIncomeList){
            String stationtype = stationsIncome.getStationType();
            float income = stationsIncome.getIncome();
            if ("YFTG".equals(stationtype)) {
                yftg_income += income;
            } else if ("GC".equals(stationtype)) {
                gc_income += income;
            } else if ("TP".equals(stationtype)) {
                tp_income += income;
            } else if ("FC".equals(stationtype)) {
                fgc_income += income;
            } else if ("FGC".equals(stationtype)) {
                fc_income += income;
            }
            sum_income += income;
        }
        map.put("yftg_income", yftg_income);
        map.put("tp_income", tp_income);
        map.put("gc_income", gc_income);
        map.put("fgc_income", fgc_income);
        map.put("fc_income", fc_income);
        map.put("sum_income", sum_income);
        return ResultBody.isOk().body(map);
    }

    @RequestMapping("/stationtype")
    public ResultBody getStationType(){
        List<StationsIncome> stationIncomeList = stDocStationService.getAllStationIncome(endtime);
        int sum_count = 0;
        int yftg_count = 0;
        int tp_count = 0;
        int gc_count = 0;
        int fgc_count = 0;
        int fc_count = 0;
        Map<String, Integer> map = new HashMap<>();
        for(StationsIncome stationsIncome : stationIncomeList){
            String stationtype = stationsIncome.getStationType();
            if ("YFTG".equals(stationtype)) {
                yftg_count += 1;
            } else if ("GC".equals(stationtype)) {
                gc_count += 1;
            } else if ("TP".equals(stationtype)) {
                tp_count += 1;
            } else if ("FC".equals(stationtype)) {
                fgc_count += 1;
            } else if ("FGC".equals(stationtype)) {
                fc_count += 1;
            }
            sum_count += 1;
        }
        map.put("yftg_count", yftg_count);
        map.put("tp_count", tp_count);
        map.put("gc_count", gc_count);
        map.put("fgc_count", fgc_count);
        map.put("fc_count", fc_count);
        map.put("sum_count", sum_count);
        return ResultBody.isOk().body(map);
    }


}
