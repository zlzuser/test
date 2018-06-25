package com.clou.ess.controller;


import cn.hutool.core.util.ObjectUtil;
import com.clou.ess.entity.StDocAgc;
import com.clou.ess.entity.StDocStation;
import com.clou.ess.model.ChartModel;
import com.clou.ess.model.GainCurveModel;
import com.clou.ess.model.ResultBody;
import com.clou.ess.service.StDocAgcService;
import com.clou.ess.service.StDocStationService;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 首页控制器
 * </p>
 *
 * @author Tanzl
 * @since 2018-03-12
 */
@RestController
public class StationController {

    @Autowired
    private StDocStationService stationService;
    @Autowired
    private StDocAgcService     agcService;

    /**
     * 电站列表
     *
     * @param userId
     * @return
     */
    @GetMapping(value = "/station", headers = "api_version=1.0")
    public ResultBody getStationList(@RequestHeader("user_id") Integer userId) {
        List<StDocStation> stationList = stationService.selectStationList(userId);
        if (!CollectionUtils.isEmpty(stationList)) {
            return ResultBody.isOk(stationList).msg("电站列表查询成功！");
        }
        return ResultBody.isFail("未查询到电站列表数据！");

    }

    /**
     * 电站概况接口
     *
     * @param userId    用户ID
     * @param stationId 电站ID
     * @return
     */
    @GetMapping(value = "/station/overview", headers = "api_version=1.0", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody<StDocStation> stationOverview(@RequestHeader("user_id") Integer userId, @RequestParam("station_id") Integer stationId) {
        StDocStation station = stationService.selectStationOverviewById(stationId);
        if (null != station) {
            Double totalRevenue = stationService.selectTotalRevenueById(stationId, userId);
            if (ObjectUtil.isNotNull(totalRevenue)) {
                station.setTotalRevenue(String.valueOf(totalRevenue));
            }
            return ResultBody.isOk(station).msg("查询成功!");
        }
        return ResultBody.isFail("未查询到此电站概况数据.");
    }

    /**
     * 电站收益曲线
     *
     * @param stationId 电站ID
     * @param dataTime  日期
     * @param dataType  查询周期(DAY, MONTH, YEAR)
     * @return
     */
    @GetMapping(value = "/curve/gain", headers = "api_version=1.0", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody stationGain(@RequestParam("station_id") Integer stationId,
                                  @RequestParam("data_time") String dataTime,
                                  @RequestParam("data_type") String dataType) {
        if ("day".equals(dataType)) {
            GainCurveModel gainCurveModels = stationService.selectElectricityByDay(stationId, dataTime);
            if (!ObjectUtils.isEmpty(gainCurveModels)) {
                return ResultBody.isOk(gainCurveModels).msg("查询成功!");
            }
            return ResultBody.isFail("无数据.");
        } else {
            List<ChartModel> chartModels = stationService.selectGainOfYearOrMonth(dataType, stationId, dataTime);
            if (!CollectionUtils.isEmpty(chartModels)) {
                Map<String, List<ChartModel>> resultMap = Maps.newHashMap();
                resultMap.put("chartData", chartModels);
                return ResultBody.isOk(resultMap).msg("查询成功!");
            }
        }
        return ResultBody.isFail("未查询到此电站收益曲线数据.");
    }


    /**
     * AGC列表
     *
     * @param stationId 电站ID
     * @return
     */
    @GetMapping(value = "/station/agcs", headers = "api_version=1.0")
    public ResultBody<List<StDocAgc>> getAgcs(@RequestParam("station_id") Integer stationId) {
        List<StDocAgc> agcs = agcService.selectAgcList(stationId);
        if (!CollectionUtils.isEmpty(agcs)) {
            return ResultBody.isOk(agcs).msg("查询成功!");
        }
        return ResultBody.isOk().msg("未查询到此电站AGC列表.");
    }
}

