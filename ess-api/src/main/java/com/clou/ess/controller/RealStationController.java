package com.clou.ess.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.clou.ess.entity.*;
import com.clou.ess.model.*;
import com.clou.ess.service.*;
import com.clou.ess.util.CommonUtil;
import com.clou.ess.util.Global;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription : 实时数据
 * @since :2018/3/19 14:51
 */
@RestController
@RequestMapping("/real")
public class RealStationController {

    @Autowired
    StDocStationService stDocStationService;

    @Autowired
    StDocBoxService stDocBoxService;

    @Autowired
    BmsMongoService bmsMongoService;

    @Autowired
    PcsMongoService pcsMongoService;

    @Autowired
    ClusterMongoService clusterMongoService;

    @Autowired
    PackageMongoService packageMongoService;

    @Autowired
    MeterRunningMongoService meterRunningMongoService;

    @Autowired
    private StDocAgcService agcService;

    /**
     * 实时电站列表
     *
     * @param userId 用户ID
     * @return
     */
    @GetMapping(value = "/stations", headers = "api_version=1.0")
    public ResultBody getStationsRealData(@RequestHeader("user_id") Integer userId) {
        // 存放实时数据
        Map<String, List<Map<String, Object>>> pushMap = new HashMap<>();
        List<Map<String, Object>> stationPushList = new ArrayList<>();
        Map<String, Object> stationPushs = null;
        List<Map<String, Object>> bmslist = new ArrayList<>();
        Map<String, Object> bmsMap = null;

        // 查询用户电站
        List<StDocStation> lsitStation = stDocStationService.getRealStaInfoOfUser(userId);
        List<RealStationModel> returnList = new ArrayList<>();
        List<StDocBms> bmsOfStationList = null;
        if (lsitStation != null && lsitStation.size() != 0) {
            for (StDocStation stDocStation : lsitStation) {
                RealStationModel realStationModel = new RealStationModel();
                // 站可充电量=堆1可充电量+堆2可充电量 + .....
                Double chargeAmount = 0.0;
                // 站可放电量=堆1可放电量+堆2可放电量 + .....
                Double dischargeAmount = 0.0;
                // 站soc=[堆1soc*（堆1可充电量+堆1可放电量）+堆2soc*（堆2可充电量+堆2可放电量）]/[(堆1可充电量+堆1可放电量)+(堆2可充电量+堆2可放电量)]
                Double totalSoc = 0.0;
                Integer stationId = stDocStation.getStationId();
                String stationType = stDocStation.getStationType();
                realStationModel.setStationId(stationId);
                realStationModel.setStationCode(stDocStation.getStationCode());
                realStationModel.setStationName(stDocStation.getStationName());
                realStationModel.setStationType(stDocStation.getStationType());
                realStationModel.setStationStatus("正常");

                bmsOfStationList = stDocStationService.selectBmsOfStation(userId, stationId);
                if ("TP".equals(stationType)) {
                    for (StDocBms bms : bmsOfStationList) {
                        Integer bmsId = bms.getBmsId();
                        String   bmsCode  = bms.getBmsCode();
                        BmsModel bmsModel = bmsMongoService.findOne(bmsId,bmsCode, CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
                    }
                    //加上mogodb查询最终调频电站数据

                } else if ("YFTG".equals(stationType)) {
                    if (!CollectionUtils.isEmpty(bmsOfStationList)) {
                        double socs = 0d, totalDl = 0d;
                        for (StDocBms bms : bmsOfStationList) {
                            Integer bmsId = bms.getBmsId();
                            String bmsCode = bms.getBmsCode();
                            BmsModel bmsModel = bmsMongoService.findOne(bmsId, bmsCode, CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
                            if (bmsModel != null) {
                                chargeAmount += bmsModel.getUsed_energy();
                                dischargeAmount += bmsModel.getLeft_energy();
                                // 计算SOC
                                double dl = NumberUtil.add(bmsModel.getUsed_energy(), bmsModel.getLeft_energy());
                                socs += NumberUtil.mul(bmsModel.getSoc().doubleValue(), dl);
                                totalDl += dl;
                            }

                            // 推送数据文档
                            bmsMap = new HashMap<>();
                            bmsMap.put("STATION_ID", stationId);
                            bmsMap.put("BMS_ID", bms.getBmsId());
                            bmsMap.put("BMS_CODE", bms.getBmsCode());
                            bmsMap.put("BMS_NAME", bms.getBmsName());
                            bmslist.add(bmsMap);
                        }
                        // 电站总SOC
                        if (totalDl != 0) {
                            totalSoc = NumberUtil.div(socs, totalDl);
                        }
                        // 移峰填谷充放电状态
                        Set<Integer> cfStatus = new HashSet<>();
                        List<StDocPcs> pcsList = stDocStationService.selectPcsCodeByStationId(stationId);
                        if (!CollectionUtils.isEmpty(pcsList)) {
                            for (StDocPcs pcs : pcsList) {
                                PcsModel pcsModel = pcsMongoService.findOne(pcs.getPcsId(), pcs.getPcsCode(), CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
                                if (null != pcsModel) {
                                    Integer isCharge = CommonUtil.isCharge(pcsModel.getStatus());
                                    if (NumberUtil.isNumber(String.valueOf(isCharge))) {
                                        cfStatus.add(isCharge);
                                    }
                                }
                            }
                        }

                        // 校验是否有数据
                        if (chargeAmount == 0 && dischargeAmount == 0 && totalSoc == 0) {
                            realStationModel.setStationStatus("无数据");
                        } else {
                            //加上mogodb查询最终移峰填谷电站数据
                            realStationModel.setChargeAmount(chargeAmount);
                            realStationModel.setDischargeAmount(dischargeAmount);
                            realStationModel.setTotalSoc(totalSoc);
                        }

                        // 如果冲放电状态有多个就存在有充有放
                        if (cfStatus.size() > 1) {
                            realStationModel.setChargeDischargeStatus("有充有放");
                        } else if (cfStatus.size() == 1) {
                            realStationModel.setChargeDischargeStatus((ArrayUtil.get(cfStatus.toArray(), 0) == 0) ? "正在放电" : "正在充电");
                        } else {
                            realStationModel.setChargeDischargeStatus("####");
                        }
                    }
                    returnList.add(realStationModel);
                }

                // 推送数据文档
                stationPushs = new HashMap<>();
                stationPushs.put("STATION_ID", stDocStation.getStationId());
                stationPushs.put("STATION_CODE", stDocStation.getStationCode());
                stationPushs.put("STATION_NAME", stDocStation.getStationName());
                stationPushs.put("STATION_TYPE", stDocStation.getStationType());
                stationPushList.add(stationPushs);
            }
            // 存放用户推送数据项
            pushMap.put("STATIONS_BMS", bmslist);
            pushMap.put("STATIONS", stationPushList);
            Global.dateMap.put(userId, pushMap);
            return ResultBody.isOk().body(returnList).msg("电站列表数据查询成功！");
        } else {
            return ResultBody.isFail().msg("无电站列表数据！");
        }

    }

    /**
     * 电站视图
     *
     * @param userId    用户ID
     * @param stationId 电站ID
     * @return
     */
    @GetMapping(value = "/station", headers = "api_version=1.0")
    public ResultBody getStationRealData(@RequestHeader("user_id") Integer userId,
                                         @RequestParam("station_id") Integer stationId,
                                         @RequestParam(name = "agc_id", required = false) Integer agcId) {
        Map<String, List<Map<String, Object>>> pushMap = new HashMap<>();
        Map<String, Object> returnMap = new HashMap<>();
        String msg = "电站";
        if (ObjectUtil.isNull(agcId)) {
            List<Map<String, Object>> resultList = stDocStationService.selectStationView(stationId, userId);
            if (resultList != null && resultList.size() != 0) {
                returnMap.put("dataTime", new Date());
                returnMap.put("dataList", resultList);

                // 存放用户推送数据项
                pushMap.put("STATION", resultList);
                Global.dateMap.put(userId, pushMap);

                return ResultBody.isOk(returnMap).msg(msg + "视图查询成功！");
            }
        } else if (ObjectUtil.isNotNull(agcId)) {
            msg = "AGC";
            List<CommonModel> commonModels = agcService.selectListByAgcId(agcId);
            if (!CollectionUtils.isEmpty(commonModels)) {
                returnMap.put("dataTime", new Date());
                returnMap.put("dataList", commonModels);
                return ResultBody.isOk(returnMap).msg(msg + "视图查询成功！");
            }
        }
        return ResultBody.isFail("无" + msg + "视图数据！");
    }

    /**
     * 电池箱视图
     *
     * @param userId 用户ID
     * @param boxId  电池箱ID
     * @return
     */
    @GetMapping(value = "/box", headers = "api_version=1.0")
    public ResultBody getBoxRealData(@RequestHeader("user_id") Integer userId,
                                     @RequestParam("box_id") Integer boxId) {
        Map<String, List<Map<String, Object>>> pushMap = new HashMap<>();
        Map<String, List<CommonModel>> pushList = new HashMap<>();
        BaseBoxModel boxModel = null;
        List<CommonModel> dataList = new ArrayList<>();
        List<Map<String, Object>> boxList = stDocBoxService.selectBoxViewInfo(userId, boxId);
        if (boxList != null && boxList.size() != 0) {
            Map<String, Object> boxMap = boxList.get(0);
            String stationType = boxMap.get("STATION_TYPE") + "";
            String[] bmsInfo = null;
            String[] meterInfo = null;
            if (boxMap.get("BMS_INFO") != null) {
                bmsInfo = (boxMap.get("BMS_INFO") + "").split(";");
                for (int i = 0; i < bmsInfo.length; i++) {
                    String[] bms = bmsInfo[i].split(",");
                    String bmsId = bms[0];
                    String bmsCode = bms[1];
                    String bmsName = bms[2];
                    dataList.add(new CommonModel(bmsId, bmsCode, bmsName, "bms", "0"));
                }
            }
            if (boxMap.get("METER_INFO") != null) {
                meterInfo = (boxMap.get("METER_INFO") + "").split(";");
                for (int j = 0; j < meterInfo.length; j++) {
                    String[] meter = meterInfo[j].split(",");
                    String meterId = meter[0];
                    String meterCode = meter[1];
                    String meterName = meter[2];
                    dataList.add(new CommonModel(meterId, meterCode, meterName, "meter", "0"));
                }
            }

            double humidity = RandomUtils.nextDouble(45, 50);
            double temperature = RandomUtils.nextDouble(20, 30);
            if ("TP".equals(stationType)) {
                boxModel = new FMBoxModel(new Date(), "0", humidity, temperature, dataList);
            } else if ("YFTG".equals(stationType)) {
                boxModel = new BaseBoxModel(new Date(), "0", humidity, temperature, dataList);
            }
            // 存放用户推送数据项
            pushMap.put("BOX", boxList);
            pushList.put("BOX_LIST", dataList);
            Global.dateMap.put(userId, pushMap);
            Global.dataList.put(userId, pushList);
            return ResultBody.isOk().body(boxModel).msg("电池箱数据查询成功！");
        } else {
            return ResultBody.isFail().msg("无电池箱数据！");
        }

    }

    /**
     * 电池堆视图
     *
     * @param userId  用户ID
     * @param bmsId   电池堆ID
     * @param bmsCode 电池堆编码
     * @return
     */
    @GetMapping(value = "/bms", headers = "api_version=1.0")
    public ResultBody getBmsRealData(@RequestHeader("user_id") Integer userId,
                                     @RequestParam("bms_id") Integer bmsId,
                                     @RequestParam("bms_code") String bmsCode) {
        Map<String, List<Map<String, Object>>> pushMap = new HashMap<>();
        Map<String, List<CommonModel>> pushList = new HashMap<>();
        List<Map<String, Object>> bmsList = stDocBoxService.selectBmsViewInfo(userId, bmsId);
        List<StDocCluster> clusterOfBms = stDocBoxService.getClusterOfBms(userId, bmsId);
        String voltage = "";
        Double vMax = 0.0;
        Double vMin = 0.0;
        String tmp = "";
        Double tMax = 0.0;
        Double tMin = 0.0;
        if (clusterOfBms != null && clusterOfBms.size() != 0) {
            List<Map<String, Object>> clusterList = new ArrayList<>();

            for (StDocCluster stDocCluster : clusterOfBms) {
                Map<String, Object> cluster = new HashMap<>();
                String clusterCode = stDocCluster.getClusterCode();
                Map<String, Object> map = clusterMongoService.findOneVoltageAndCurrentByCluster(stDocCluster.getClusterId(), clusterCode, CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
                if (map != null) {
                    voltage += map.get("cell_v_data") + ";";
                    tmp += map.get("temp_data") + ";";
                }
                // 推送数据项
                cluster.put("CLUSTER_ID", stDocCluster.getClusterId());
                cluster.put("CLUSTER_CODE", stDocCluster.getClusterCode());
                cluster.put("CLUSTER_NAME", stDocCluster.getClusterName());
                clusterList.add(cluster);
            }
            pushMap.put("CLUSTEROFBMS", clusterList);
        }
        vMax = CommonUtil.getStringArrayMax(voltage.split(";"));
        vMin = CommonUtil.getStringArrayMin(voltage.split(";"));
        tMax = CommonUtil.getStringArrayMax(tmp.split(";"));
        tMin = CommonUtil.getStringArrayMin(tmp.split(";"));
        List<CommonModel> dataList = null;
        if (bmsList != null && bmsList.size() != 0) {
            dataList = new ArrayList<>();
            Map<String, Object> listInfo = bmsList.get(0);
            CommonModel commonModel = null;
            String[] pcsInfo = null;
            String[] clusterInfo = null;
            String[] meterInfo = null;
            if (listInfo.get("PCS_INFO") != null) {
                pcsInfo = (listInfo.get("PCS_INFO") + "").split(";");
                for (int i = 0; i < pcsInfo.length; i++) {
                    commonModel = new CommonModel();
                    String[] pcs = pcsInfo[i].split(",");
                    String pcsId = pcs[0];
                    String pcsCode = pcs[1];
                    String pcsName = pcs[2];
                    commonModel.setId(pcsId);
                    commonModel.setCode(pcsCode);
                    commonModel.setName(pcsName);
                    commonModel.setType("pcs");
                    dataList.add(commonModel);
                }
            }
            if (listInfo.get("CLUSTER_INFO") != null) {
                clusterInfo = (listInfo.get("CLUSTER_INFO") + "").split(";");
                for (int i = 0; i < clusterInfo.length; i++) {
                    commonModel = new CommonModel();
                    String[] cluster = clusterInfo[i].split(",");
                    String clusterId = cluster[0];
                    String clusterCode = cluster[1];
                    String clusterName = cluster[2];
                    commonModel.setId(clusterId);
                    commonModel.setCode(clusterCode);
                    commonModel.setName(clusterName);
                    commonModel.setType("cluster");
                    dataList.add(commonModel);
                }
            }
            if (listInfo.get("METER_INFO") != null) {
                meterInfo = (listInfo.get("METER_INFO") + "").split(";");
                for (int i = 0; i < meterInfo.length; i++) {
                    commonModel = new CommonModel();
                    String[] meter = meterInfo[i].split(",");
                    String meterId = meter[0];
                    String meterCode = meter[1];
                    String meterName = meter[2];
                    commonModel.setId(meterId);
                    commonModel.setCode(meterCode);
                    commonModel.setName(meterName);
                    commonModel.setType("meter");
                    dataList.add(commonModel);
                }
            }
            // 存放用户推送数据项
            pushMap.put("BMS", bmsList);
            pushList.put("BMS_LIST", dataList);
            Global.dateMap.put(userId, pushMap);
            Global.dataList.put(userId, pushList);

            BmsModel bmsModel = bmsMongoService.findOne(bmsId, bmsCode, CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
            if (bmsModel != null) {
                bmsModel.setDataList(dataList);
                bmsModel.setDiffPressure(vMax - vMin);
                bmsModel.setDiffTemperature(tMax - tMin);
                bmsModel.setMaximumVoltage(vMax);
                bmsModel.setMinimumVoltage(vMin);
                bmsModel.setMaximumTemperature(tMax);
                bmsModel.setMinimumTemperature(tMin);
                return ResultBody.isOk().body(bmsModel).msg("电池堆数据查询成功！");
            } else {
                return ResultBody.isFail().msg("无电池堆数据！");
            }
        } else {
            return ResultBody.isFail().msg("无电池堆数据！");
        }
    }

    /**
     * PCS视图
     *
     * @param userId  用户ID
     * @param pcsId   PCS ID
     * @param pcsCode PCS编码
     * @return
     */
    @GetMapping(value = "/pcs", headers = "api_version=1.0")
    public ResultBody getPcsRealData(@RequestHeader("user_id") Integer userId,
                                     @RequestParam("pcs_id") Integer pcsId,
                                     @RequestParam("pcs_code") String pcsCode) {
        // 存放用户推送数据项
        Global.dateMap.put(userId, stDocBoxService.setPcsViewInfo(pcsId, pcsCode));
        // 查询历史数据
        PcsModel pcsModel = pcsMongoService.findOne(pcsId, pcsCode, CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
        if (pcsModel != null) {
            // 解析PCS状态码
            CommonUtil.getPcsStatus(pcsModel);
            //mogodb查询成功
            return ResultBody.isOk().body(pcsModel).msg("PCS视图数据查询成功！");
        } else {
            return ResultBody.isFail().msg("无PCS视图数据！");
        }
    }

    /**
     * 电池簇视图
     *
     * @param userId      用户ID
     * @param clusterId   电池簇ID
     * @param clusterCode 电池簇编码
     * @return
     */
    @GetMapping(value = "/cluster", headers = "api_version=1.0")
    public ResultBody getClusterRealData(@RequestHeader("user_id") Integer userId,
                                         @RequestParam("cluster_id") Integer clusterId,
                                         @RequestParam("cluster_code") String clusterCode) {
        List<CommonModel> dataList = null;
        CommonModel dataModel = null;
        Map<String, List<CommonModel>> pushList = new HashMap<>();
        List<Map<String, Object>> packageInfo = stDocBoxService.getPackageInfo(userId, clusterId);
        if (packageInfo != null && packageInfo.size() != 0) {
            // 存放用户推送数据项
            Global.dateMap.put(userId, Collections.singletonMap("PACKAGE", packageInfo));

            Double vMax = 0.0;
            Double vMin = 0.0;
            Double tMax = 0.0;
            Double tMin = 0.0;
            dataList = new ArrayList<>();
            String[] packArray = (packageInfo.get(0).get("PACKAGE_INFO") + "").split(";");
            for (int i = 0; i < packArray.length; i++) {
                dataModel = new CommonModel();
                String[] pack = packArray[i].split(",");
                String id = pack[0];
                String code = pack[1];
                String name = pack[2];
                String sort_num = pack[3];
                dataModel.setId(id);
                dataModel.setCode(code);
                dataModel.setName(name);
                dataList.add(dataModel);
                PackageModel packageModel = packageMongoService.findOnePackage(clusterId, clusterCode, Integer.valueOf(sort_num), CommonUtil.getBeforeTime(), CommonUtil.getNowTime());

                if (packageModel != null) {
                    // 取最大最小电压
                    if (packageModel.getCell_v_data() != null) {
                        double min = CommonUtil.getStringArrayMin(packageModel.getCell_v_data().split(";"));
                        double max = CommonUtil.getStringArrayMax(packageModel.getCell_v_data().split(";"));
                        if (i == 0) {
                            vMin = min;
                        } else {
                            if (vMin > min) {
                                vMin = min;
                            }
                        }
                        if (vMax < max) {
                            vMax = max;
                        }
                    }
                    // 取最大最小温度
                    if (packageModel.getTemp_data() != null) {
                        double min = CommonUtil.getStringArrayMin(packageModel.getTemp_data().split(";"));
                        double max = CommonUtil.getStringArrayMax(packageModel.getTemp_data().split(";"));
                        if (i == 0) {
                            tMin = min;
                        } else {
                            if (tMin > min) {
                                tMin = min;
                            }
                        }
                        if (tMax < max) {
                            tMax = max;
                        }
                    }
                }
            }
            // 存放用户推送数据项
            pushList.put("PACKAGE_LIST", dataList);
            Global.dateMap.put(userId, Collections.singletonMap("PACKAGE", packageInfo));
            Global.dataList.put(userId, pushList);

            ClusterModel clusterModel = clusterMongoService.findOneCluster(clusterId, clusterCode, CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
            if (clusterModel != null) {
                //查询mogodb数据成功
                clusterModel.setDiffPressure(vMax - vMin);
                clusterModel.setDiffTemperature(tMax - tMin);
                clusterModel.setMaximumVoltage(vMax);
                clusterModel.setMinimumVoltage(vMin);
                clusterModel.setMaximumTemperature(tMax);
                clusterModel.setMinimumTemperature(tMin);
                clusterModel.setDataList(dataList);
                return ResultBody.isOk().body(clusterModel).msg("电池簇数据查询成功！");
            } else {
                return ResultBody.isFail().msg("无电池簇数据！");
            }
        } else {
            return ResultBody.isFail().msg("无电池簇数据！");
        }
    }

    /**
     * 电池包视图
     *
     * @param userId      用户ID
     * @param packageId   电池包ID
     * @param clusterCode 电池簇编码
     * @param queryType   查询类型
     * @return
     */
    @GetMapping(value = "/package", headers = "api_version=1.0")
    public ResultBody getPackageRealData(@RequestHeader("user_id") Integer userId,
                                         @RequestParam("cluster_id") Integer clusterId,
                                         @RequestParam("cluster_code") String clusterCode,
                                         @RequestParam("package_id") Integer packageId,
                                         @RequestParam("query_type") String queryType) {
        Integer innerSort = 1;
        String packageCode = null;
        String stationCode = null;
        List<StDocPackage> stDocPackageList = stDocBoxService.getSortOfPackage(packageId);
        if (stDocPackageList.get(0) != null) {
            innerSort = stDocPackageList.get(0).getInnerSort();
            packageCode = stDocPackageList.get(0).getPackageCode();
            stationCode = stDocPackageList.get(0).getStationCode();
        }

        // 存放用户推送数据项
        Global.dateMap.put(userId, stDocBoxService.setPackageViewInfo(stationCode, packageId, packageCode, clusterCode, queryType));
        // 查询历史数据
        PackageModel clusterPackageModel = packageMongoService.findOnePackage(clusterId, clusterCode, innerSort, CommonUtil.getBeforeTime(), CommonUtil.getNowTime());
        BatteriesModel batteriesModel = new BatteriesModel();
        List<ChartModel> charts = new ArrayList<>();

        // 1、电压;2、温度
        if ("1".equals(queryType)) {
            if (clusterPackageModel != null) {
                batteriesModel.setDataTime(clusterPackageModel.getData_date());
                batteriesModel.setPackageCode(packageCode);
                batteriesModel.setTotalVoltage(clusterPackageModel.getVoltage());
                //mogodb查询成功
                String[] vData = clusterPackageModel.getCell_v_data().split(";");
                for (int i = 0; i < vData.length; i++) {
                    charts.add(new ChartModel(String.valueOf(i + 1), vData[i]));
                }

                batteriesModel.setMin("2.85");
                batteriesModel.setRete("3.20");
                batteriesModel.setMax("3.55");
                batteriesModel.setCharts(charts);
                return ResultBody.isOk().body(batteriesModel).msg("电池包视图电压数据查询成功！");
            } else {
                return ResultBody.isFail().msg("无电池包视图电压数据！");
            }
        } else if ("2".equals(queryType)) {
            if (clusterPackageModel != null) {
                batteriesModel.setDataTime(clusterPackageModel.getData_date());
                batteriesModel.setPackageCode(packageCode);
                //mogodb查询成功
                String[] tData = clusterPackageModel.getTemp_data().split(";");
                for (int i = 0; i < tData.length; i++) {
                    charts.add(new ChartModel(String.valueOf(i + 1), tData[i]));
                }
                batteriesModel.setMin("10");
                batteriesModel.setRete("30");
                batteriesModel.setMax("50");
                batteriesModel.setCharts(charts);
                return ResultBody.isOk().body(batteriesModel).msg("电池包视图电压数据查询成功！");
            } else {
                return ResultBody.isFail().msg("无电池包视图温度数据！");
            }
        } else {
            return ResultBody.isFail().msg("电池包视图查询类型输入错误！");
        }
    }

    /**
     * 电表视图
     *
     * @param userId    用户ID
     * @param meterId   电表ID
     * @param meterCode 电表编码
     * @return
     */
    @GetMapping(value = "/meter", headers = "api_version=1.0")
    public ResultBody getMeterRealData(@RequestHeader("user_id") Integer userId,
                                       @RequestParam("meter_id") Integer meterId,
                                       @RequestParam("meter_code") String meterCode) {
        // 存放用户推送数据项
        Global.dateMap.put(userId, stDocBoxService.setMeterViewInfo(meterId, meterCode));
        // Mongodb 电表数据时标间隔为4分钟以上一条数据
        String startTime = String.valueOf(DateUtil.offsetMinute(new Date(), -5).getTime());
        MeterRunningModel meterRunningModel = meterRunningMongoService.findOne(meterId, meterCode, startTime, CommonUtil.getNowTime());
        if (meterRunningModel != null) {
            //mogodb查询成功
            return ResultBody.isOk().body(meterRunningModel).msg("电表视图数据查询成功！");
        } else {
            return ResultBody.isFail().msg("无电表视图数据！");
        }
    }

    /**
     * PLC视图
     *
     * @param userId  用户ID
     * @param plcId   电表ID
     * @param plcCode 电表编码
     * @return
     */
    @GetMapping(value = "/plc", headers = "api_version=1.0")
    public ResultBody<PlcModel> getPlcView(@RequestHeader("user_id") Integer userId,
                                           @RequestParam("plc_id") Integer plcId,
                                           @RequestParam("plc_code") String plcCode) {
        return ResultBody.isOk().body(new PlcModel()).msg("PLC视图实时数据查询成功！");
    }

}
