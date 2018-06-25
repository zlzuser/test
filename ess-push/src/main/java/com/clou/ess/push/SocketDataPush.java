package com.clou.ess.push;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.clou.ess.actor.InfoConsumer;
import com.clou.ess.entity.StDocEventLevel;
import com.clou.ess.entity.StDocPcs;
import com.clou.ess.entity.StPushRecord;
import com.clou.ess.model.*;
import com.clou.ess.service.PcsMongoService;
import com.clou.ess.service.StDocEventLevelService;
import com.clou.ess.service.StDocStationService;
import com.clou.ess.service.StUserEventSubscribeService;
import com.clou.ess.util.CommonUtil;
import com.clou.ess.util.Global;
import com.clou.ess.util.SpringContextUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gexin.fastjson.JSON;
import com.gexin.fastjson.serializer.SerializerFeature;
import com.gexin.rp.sdk.base.IPushResult;
import com.gexin.rp.sdk.base.impl.ListMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/19 11:54
 */
public class SocketDataPush extends DataPush implements InfoConsumer {

    private final Logger logger = LoggerFactory.getLogger(SocketDataPush.class);

    private SocketDataPush() {
    }

    private static SocketDataPush instance = new SocketDataPush();

    public static SocketDataPush getInstance() {
        return instance;
    }

    /**
     * @param info 传过来的消息体
     */
    @Override
    public void consumeInfo(Object info) {
        List<Map<String, Object>> infoList = new ArrayList<>();
        try {
            infoList = objectMapper.readValue(info.toString(), List.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Map<String, Object> infoMap : infoList) {
            String codeInfo = infoMap.get("code") + "";
            String[] codeInfos = codeInfo.split("\\$");
            String flag = codeInfos[0];
            try {
                // 事件用个推推送，其他用socket推送
                //System.out.println(infoMap);
                if ("event".equals(flag)) {
                    String stationCode = codeInfos[1];
                    String bmsCode = codeInfos[2];
                    pushEvent(stationCode, bmsCode, infoMap);
                } else {
                    infoDeal(flag, infoMap);
                }
            } catch (Exception e) {
                logger.error("推送数据异常:{}", e);

            }
        }
    }

    public void infoDeal(String flag, Map<String, Object> infoMap) throws ParseException, JsonProcessingException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (!CollectionUtils.isEmpty(Global.scoketMap)) {
            Iterator<Integer> iterator = Global.scoketMap.keySet().iterator();
            while (iterator.hasNext()) {
                Integer userId = iterator.next();
                Socket socket = Global.scoketMap.get(userId);
                Map<String, List<Map<String, Object>>> map = Global.dateMap.get(userId);
                if (map != null && !map.isEmpty()) {
                    if ("station".equals(flag)) {
                        if (map.get("STATIONS") != null && map.get("STATIONS").size() != 0) {//电站列表
                            List<RealStationModel> returnList = new ArrayList<>();
                            List<Map<String, Object>> stations = map.get("STATIONS");
                            for (Map<String, Object> stationMap : stations) {
                                String stationInfo = infoMap.get("code") + "";
                                String[] stationInfos = stationInfo.split("\\$");
                                String staCode = stationInfos[1];
                                String stationCode = stationMap.get("STATION_CODE") + "";
                                if (stationCode.equals(staCode)) {
                                    RealStationModel realStationModel = new RealStationModel();
                                    Double chargeAmount = 0.0;
                                    Double dischargeAmount = 0.0;
                                    Double totalSoc = 0.0;

                                    int stationId = Integer.parseInt(String.valueOf(stationMap.get("STATION_ID")));
                                    String stationName = stationMap.get("STATION_NAME") + "";
                                    String stationType = stationMap.get("STATION_TYPE") + "";//电站类型TP/YFTG

                                    //公共字段
                                    realStationModel.setStationId(stationId);
                                    realStationModel.setStationCode(stationCode);
                                    realStationModel.setStationName(stationName);
                                    realStationModel.setStationType(stationType);
                                    realStationModel.setStationStatus("正常");

                                    if ("TP".equals(stationType)) {
                                        //调频的暂时不做
                                    } else if ("YFTG".equals(stationType)) {
                                        totalSoc = NumberUtils.toDouble(String.valueOf(infoMap.get("soc")));
                                        chargeAmount = NumberUtils.toDouble(String.valueOf(infoMap.get("used_energy")));
                                        dischargeAmount = NumberUtils.toDouble(String.valueOf(infoMap.get("left_energy")));

                                        // 移峰填谷充放电状态
                                        Set<Integer> cfStatus = new HashSet<>();
                                        StDocStationService stationService = (StDocStationService) SpringContextUtil.getBean(StDocStationService.class);
                                        PcsMongoService pcsMongoService = (PcsMongoService) SpringContextUtil.getBean(PcsMongoService.class);
                                        List<StDocPcs> pcsList = stationService.selectPcsCodeByStationId(stationId);
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
                                    String json = objectMapper.writeValueAsString(returnList);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("客户端用户ID:{} ,推送电站列表数据:{}", userId, json);
                                    }

                                    dataPush(userId, socket, json);
                                }
                            }
                        }
                    } else if ("box".equals(flag)) {
                        if (map.get("BOX") != null && map.get("BOX").size() != 0) {//电池箱视图
                            String stationInfo = infoMap.get("code") + "";
                            String[] codeInfos = stationInfo.split("\\$");
                            String staCode = codeInfos[1];
                            String bCode = codeInfos[2];
                            Map<String, Object> box = map.get("BOX").get(0);
                            String stationType = box.get("STATION_TYPE") + "";
                            String stationCode = box.get("STATION_CODE") + "";
                            String boxCode = box.get("BOX_CODE") + "";
                            Map<String, Object> returnMap = null;
                            if (stationCode.equals(staCode) && boxCode.equals(bCode)) {
                                returnMap = new HashMap<>();
                                /**
                                 * 箱的实时数据不知道查哪些表
                                 */
                                returnMap.put("dataTime", infoMap.get("data_date"));
                                returnMap.put("fireSignal", 0);
                                returnMap.put("humidity", NumberUtils.toDouble(String.valueOf(infoMap.get("humid"))));
                                returnMap.put("temperature", NumberUtils.toDouble(String.valueOf(infoMap.get("temp"))));
                                if ("TP".equals(stationType)) {
                                    //调频电站暂时没有相应数据推送
                                    returnMap.put("ua", null);
                                    returnMap.put("ub", null);
                                    returnMap.put("uc", null);
                                    returnMap.put("ia", null);
                                    returnMap.put("ib", null);
                                    returnMap.put("ic", null);
                                    returnMap.put("chargeAmountMax", null);
                                    returnMap.put("dischargeAmountMax", null);
                                    returnMap.put("chargePowerMax", null);
                                    returnMap.put("dischargePowerMax", null);
                                    returnMap.put("activePower", null);
                                    returnMap.put("reactivePower", null);
                                    returnMap.put("gridFrequency", null);
                                    returnMap.put("comStatus", null);
                                } else if ("YFTG".equals(stationType)) {
                                    //移峰填谷暂时没有特殊字段
                                }
                                List<CommonModel> boxList = Global.dataList.get(userId) == null ? null : Global.dataList.get(userId).get("BOX_LIST");
                                if (boxList != null && boxList.size() != 0) {
                                    returnMap.put("dataList", boxList);
                                }
                                String json = objectMapper.writeValueAsString(returnMap);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("客户端用户ID:{} ,推送电池箱数据:{}", userId, json);
                                }
                                dataPush(userId, socket, json);
                            }
                        }
                    } else if ("bms".equals(flag)) {
                        if (map.get("BMS") != null && map.get("BMS").size() != 0) {//bms视图
                            String codeInfo = infoMap.get("code") + "";
                            String[] codeInfos = codeInfo.split("\\$");
                            String staCode = codeInfos[1];
                            String bCode = codeInfos[2];
                            Map<String, Object> bms = map.get("BMS").get(0);
                            String bmsCode = bms.get("BMS_CODE") + "";
                            String stationCode = bms.get("STATION_CODE") + "";
                            if (stationCode.equals(staCode) && bmsCode.equals(bCode)) {
                                BaseBmsModel baseBmsModel = null;
                                BmsModel bmsModel = null;
                                if (!infoMap.containsKey("vol_sub_bms")) {
                                    bmsModel = new BmsModel();
                                    bmsModel.setData_date(sdf.parse(String.valueOf(infoMap.get("data_date"))));
                                    bmsModel.setVoltage(NumberUtils.toDouble(String.valueOf(infoMap.get("voltage"))));
                                    bmsModel.setCurrent(NumberUtils.toDouble(String.valueOf(infoMap.get("current"))));
                                    bmsModel.setPower(NumberUtils.toDouble(String.valueOf(infoMap.get("power"))));
                                    bmsModel.setSoc(NumberUtils.toDouble(String.valueOf(infoMap.get("soc"))));
                                    bmsModel.setSoh(NumberUtils.toDouble(String.valueOf(infoMap.get("soh"))));
                                    bmsModel.setAll_inenergy(NumberUtils.toDouble(String.valueOf(infoMap.get("all_inenergy"))));
                                    bmsModel.setAll_outenergy(NumberUtils.toDouble(String.valueOf(infoMap.get("all_outenergy"))));
                                    bmsModel.setUsed_energy(NumberUtils.toDouble(String.valueOf(infoMap.get("used_energy"))));
                                    bmsModel.setLeft_energy(NumberUtils.toDouble(String.valueOf(infoMap.get("left_energy"))));
                                    List<CommonModel> bmsList = Global.dataList.get(userId) == null ? null : Global.dataList.get(userId).get("BMS_LIST");
                                    if (bmsList != null && bmsList.size() != 0) {
                                        bmsModel.setDataList(bmsList);
                                    }
                                } else {
                                    baseBmsModel = new BaseBmsModel();
                                    baseBmsModel.setData_date(sdf.parse(String.valueOf(infoMap.get("data_date"))));
                                    baseBmsModel.setDiffPressure(NumberUtils.toDouble(String.valueOf(infoMap.get("vol_sub_bms"))));
                                    baseBmsModel.setDiffTemperature(NumberUtils.toDouble(String.valueOf(infoMap.get("tem_sub_bms"))));
                                    baseBmsModel.setMinimumTemperature(NumberUtils.toDouble(String.valueOf(infoMap.get("tem_min_bms"))));
                                    baseBmsModel.setMaximumTemperature(NumberUtils.toDouble(String.valueOf(infoMap.get("tem_max_bms"))));
                                    baseBmsModel.setMaximumVoltage(NumberUtils.toDouble(String.valueOf(infoMap.get("vol_max_bms"))));
                                    baseBmsModel.setMinimumVoltage(NumberUtils.toDouble(String.valueOf(infoMap.get("vol_min_bms"))));
                                }
                                ObjectMapper mapper = new ObjectMapper();
                                String json = mapper.writeValueAsString(!ObjectUtils.isEmpty(bmsModel) ? bmsModel : baseBmsModel);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("客户端用户ID:{} ,推送电池堆数据:{}", userId, json);
                                }
                                dataPush(userId, socket, json);
                            }
                        }
                    } else if ("cluster".equals(flag)) {
                        if (map.get("PACKAGE") != null && map.get("PACKAGE").size() != 0) {//电池簇视图
                            String codeInfo = infoMap.get("code") + "";
                            String[] codeInfos = codeInfo.split("\\$");
                            String staCode = codeInfos[1];
                            String cCode = codeInfos[2];
                            Map<String, Object> clus = map.get("PACKAGE").get(0);
                            String stationCode = clus.get("STATION_CODE") + "";
                            String clusterCode = clus.get("CLUSTER_CODE") + "";
                            if (stationCode.equals(staCode) && clusterCode.equals(cCode)) {
                                ClusterModel clusterModel = new ClusterModel();
                                clusterModel.setData_date(sdf.parse(String.valueOf(infoMap.get("data_date"))));
                                clusterModel.setVoltage(NumberUtils.toDouble(String.valueOf(infoMap.get("voltage"))));
                                clusterModel.setCurrent(NumberUtils.toDouble(String.valueOf(infoMap.get("current"))));
                                clusterModel.setSoc(NumberUtils.toDouble(String.valueOf(infoMap.get("soc"))));
                                clusterModel.setSoh(NumberUtils.toDouble(String.valueOf(infoMap.get("soh"))));
                                clusterModel.setDiffTemperature(NumberUtils.toDouble(String.valueOf(infoMap.get("tem_sub"))));
                                clusterModel.setDiffPressure(NumberUtils.toDouble(String.valueOf(infoMap.get("vol_sub"))));
                                clusterModel.setLeft_energy(NumberUtils.toDouble(String.valueOf(infoMap.get("left_energy"))));
                                clusterModel.setUsed_energy(NumberUtils.toDouble(String.valueOf(infoMap.get("used_energy"))));
                                clusterModel.setMaximumVoltage(NumberUtils.toDouble(String.valueOf(infoMap.get("vol_max"))));
                                clusterModel.setMinimumVoltage(NumberUtils.toDouble(String.valueOf(infoMap.get("vol_min"))));
                                clusterModel.setMaximumTemperature(NumberUtils.toDouble(String.valueOf(infoMap.get("tem_max"))));
                                clusterModel.setMinimumTemperature(NumberUtils.toDouble(String.valueOf(infoMap.get("tem_min"))));
                                List<CommonModel> packageList = Global.dataList.get(userId) == null ? null : Global.dataList.get(userId).get("PACKAGE_LIST");
                                if (packageList != null && packageList.size() != 0) {
                                    clusterModel.setDataList(packageList);
                                }

                                String json = objectMapper.writeValueAsString(clusterModel);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("客户端用户ID:{} ,推送电池簇数据:{}", userId, json);
                                }
                                dataPush(userId, socket, json);
                            }
                        }
                    } else if ("pack".equals(flag)) {
                        if (map.get("CELL") != null && map.get("CELL").size() != 0) {//电池包视图
                            String codeInfo = infoMap.get("code") + "";
                            String[] codeInfos = codeInfo.split("\\$");
                            String staCode = codeInfos[1];
                            String pCode = codeInfos[2];
                            Map<String, Object> packageMap = map.get("CELL").get(0);
                            String packageCode = packageMap.get("PACKAGE_CODE") + "";
                            String stationCode = packageMap.get("STATION_CODE") + "";
                            String queryType = packageMap.get("QUERY_TYPE") + "";
                            BatteriesModel batteriesModel = new BatteriesModel();
                            List<ChartModel> charts = new ArrayList<>();
                            if (stationCode.equals(staCode) && packageCode.equals(pCode)) {
                                if ("1".equals(queryType)) {//1、电压;2、温度
                                    batteriesModel.setDataTime(DateUtil.parse(String.valueOf(infoMap.get("data_date"))));
                                    batteriesModel.setPackageCode(pCode);
                                    batteriesModel.setTotalVoltage(NumberUtils.toDouble(String.valueOf(infoMap.get("voltage"))));
                                    batteriesModel.setMin("2.85");
                                    batteriesModel.setRete("3.20");
                                    batteriesModel.setMax("3.55");
                                    if (!ObjectUtils.isEmpty(infoMap.get("cell_vol"))) {
                                        String[] voltages = String.valueOf(infoMap.get("cell_vol")).split(";");
                                        for (int i = 0; i < voltages.length; i++) {
                                            charts.add(new ChartModel(String.valueOf(i + 1), voltages[i]));
                                        }
                                    }
                                    batteriesModel.setCharts(charts);

                                    String json = objectMapper.writeValueAsString(batteriesModel);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("客户端用户ID:{} ,推送电池包电压数据:{}", userId, json);
                                    }

                                    dataPush(userId, socket, json);
                                } else if ("2".equals(queryType)) {
                                    batteriesModel.setDataTime(DateUtil.parse(Convert.convert(String.class, infoMap.get("data_date"))));
                                    batteriesModel.setPackageCode(pCode);
                                    batteriesModel.setMin("10");
                                    batteriesModel.setRete("30");
                                    batteriesModel.setMax("50");
                                    if (!ObjectUtils.isEmpty(infoMap.get("cell_tem"))) {
                                        String[] temperatures = String.valueOf(infoMap.get("cell_tem")).split(";");
                                        for (int i = 0; i < temperatures.length; i++) {
                                            charts.add(new ChartModel(String.valueOf(i + 1), temperatures[i]));
                                        }
                                    }
                                    batteriesModel.setCharts(charts);

                                    String json = objectMapper.writeValueAsString(batteriesModel);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("客户端用户ID:{} ,推送电池包温度数据:{}", userId, json);
                                    }

                                    dataPush(userId, socket, json);
                                }
                            }
                        }
                    } else if ("pcs".equals(flag)) {
                        if (map.get("PCS") != null && map.get("PCS").size() != 0) {//pcs视图
                            String codeInfo = infoMap.get("code") + "";
                            String[] codeInfos = codeInfo.split("\\$");
                            String staCode = codeInfos[1];
                            String pCode = codeInfos[2];
                            String pcsCode = map.get("PCS").get(0).get("PCS_CODE") + "";
                            String stationCode = map.get("PCS").get(0).get("STATION_CODE") + "";
                            if (stationCode.equals(staCode) && pcsCode.equals(pCode)) {
                                PcsModel pcsModel = new PcsModel();
                                pcsModel.setData_date(sdf.parse(Convert.convert(String.class, infoMap.get("data_date"))));
                                pcsModel.setStatus(Convert.convert(String.class, infoMap.get("pcs_run_status")));
                                pcsModel.setBreakerAC(String.valueOf(infoMap.get("pcs_acbr")));
                                pcsModel.setBreakerDC(String.valueOf(infoMap.get("pcs_dcbr")));
                                String discharge = Convert.convert(String.class, infoMap.get("pcs_discharge"));//放电状态信号
                                String charge = Convert.convert(String.class, infoMap.get("pcs_charge"));//充电状态信号
                                String chargeSta = null;
                                if ("0".equals(charge)) {
                                    chargeSta = "充电";
                                } else if ("0".equals(discharge)) {
                                    chargeSta = "放电";
                                }
                                switch (pcsModel.getStatus()) {
                                    case "停机状态":
                                        chargeSta = "停机";
                                        pcsModel.setBreakerAC("2");
                                        pcsModel.setBreakerDC("2");
                                        break;
                                    case "开机状态":
                                        chargeSta += "(运行)";
                                        break;
                                    case "故障锁定状态":
                                        chargeSta += "(故障)";
                                        break;
                                    case "就绪":
                                        chargeSta += "(运行)";
                                        break;
                                    default:
                                        break;
                                }
                                pcsModel.setChargeDischargeStatus(chargeSta);
                                pcsModel.setOperatingMode(Convert.convert(String.class, infoMap.get("pcs_wstat")));
                                pcsModel.setControlMode(Convert.convert(String.class, infoMap.get("pcs_cmod")));
                                pcsModel.setIa(NumberUtils.toDouble(String.valueOf(infoMap.get("ia"))));
                                pcsModel.setIb(NumberUtils.toDouble(String.valueOf(infoMap.get("ib"))));
                                pcsModel.setIc(NumberUtils.toDouble(String.valueOf(infoMap.get("ic"))));
                                pcsModel.setUa(NumberUtils.toDouble(String.valueOf(infoMap.get("ua"))));
                                pcsModel.setUb(NumberUtils.toDouble(String.valueOf(infoMap.get("ub"))));
                                pcsModel.setUc(NumberUtils.toDouble(String.valueOf(infoMap.get("uc"))));
                                pcsModel.setP_rate(NumberUtils.toDouble(String.valueOf(infoMap.get("p_rate"))));
                                pcsModel.setPz(NumberUtils.toDouble(String.valueOf(infoMap.get("pz"))));
                                pcsModel.setQz(NumberUtils.toDouble(String.valueOf(infoMap.get("qz"))));
                                pcsModel.setU_udc(NumberUtils.toDouble(String.valueOf(infoMap.get("u_udc"))));
                                pcsModel.setI_udc(NumberUtils.toDouble(String.valueOf(infoMap.get("i_udc"))));
                                pcsModel.setP_udc(NumberUtils.toDouble(String.valueOf(infoMap.get("p_udc"))));

                                String json = objectMapper.writeValueAsString(pcsModel);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("客户端用户ID:{} ,推送PCS数据:{}", userId, json);
                                }
                                dataPush(userId, socket, json);
                            }
                        }
                    } else if ("meter".equals(flag)) {
                        if (map.get("METER") != null && map.get("METER").size() != 0) {//电表视图
                            String codeInfo = infoMap.get("code") + "";
                            String[] codeInfos = codeInfo.split("\\$");
                            String staCode = codeInfos[1];
                            String mCode = codeInfos[2];
                            String meterCode = map.get("METER").get(0).get("METER_CODE") + "";
                            String stationCode = map.get("METER").get(0).get("STATION_CODE") + "";
                            if (stationCode.equals(staCode) && meterCode.equals(mCode)) {
                                MeterRunningModel meterRunningModel = new MeterRunningModel();
                                meterRunningModel.setData_date(sdf.parse(String.valueOf(infoMap.get("data_date"))));
                                meterRunningModel.setIa(NumberUtils.toDouble(String.valueOf(infoMap.get("ia"))));
                                meterRunningModel.setIb(NumberUtils.toDouble(String.valueOf(infoMap.get("ib"))));
                                meterRunningModel.setIc(NumberUtils.toDouble(String.valueOf(infoMap.get("ic"))));
                                meterRunningModel.setUa(NumberUtils.toDouble(String.valueOf(infoMap.get("ua"))));
                                meterRunningModel.setUb(NumberUtils.toDouble(String.valueOf(infoMap.get("ub"))));
                                meterRunningModel.setUc(NumberUtils.toDouble(String.valueOf(infoMap.get("uc"))));
                                meterRunningModel.setPz(NumberUtils.toDouble(String.valueOf(infoMap.get("pz"))));
                                meterRunningModel.setQz(NumberUtils.toDouble(String.valueOf(infoMap.get("qz"))));

                                String json = objectMapper.writeValueAsString(meterRunningModel);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("客户端用户ID:{} ,推送电表数据:{}", userId, json);
                                }

                                dataPush(userId, socket, json);
                            }
                        }
                    }
                /*else if(map.get("AGCS")!=null&&map.get("AGCS").size()!=0){//调频电站列表

                    //暂时没做，没有开发需求

                }else if(map.get("AGC")!=null&&map.get("AGC").size()!=0){//调频电站视图

                    //暂时没做

                }else if(map.get("PLC")!=null&&map.get("PLC").size()!=0){//plc视图*/

                    //暂时没做

                }

            }
        }
    }

    /**
     * 事件推送
     *
     * @param stationCode 电站编码
     * @param bmsCode     电池堆编码
     * @param maps        事件对象
     */
    public void pushEvent(String stationCode, String bmsCode, Map<String, Object> maps) {
        StUserEventSubscribeService subscribeService = (StUserEventSubscribeService) SpringContextUtil.getBean(StUserEventSubscribeService.class);
        StDocEventLevelService eventLevelService = (StDocEventLevelService) SpringContextUtil.getBean(StDocEventLevelService.class);
        String eventSource = String.valueOf(maps.get("source"));
        String eventMsg = String.valueOf(maps.get("event"));
        String eventType = String.valueOf(maps.get("type"));
        String deviceType = StringUtils.substringBefore(eventSource, ":");
        Date eventDate = DateUtil.parse(String.valueOf(maps.get("data_date")), DatePattern.NORM_DATETIME_PATTERN);
        EventModel eventModel = new EventModel(eventSource, null, eventType, deviceType, eventDate);
        // 查询该事件中文释义
        StDocEventLevel eventLevel = eventLevelService.selectSmsCongEvent(stationCode, bmsCode, deviceType, eventMsg);

        if (!ObjectUtils.isEmpty(eventLevel)) {
            eventModel.setEvent(eventLevel.getDescChs());
            eventModel.setEventLevel(eventLevel.getLevelCode());
            if ("happen".equals(eventType)) {
                eventModel.setType(" 发生");
            } else if ("disappear".equals(eventType)) {
                eventModel.setType(" 消失");
            }
            // 查询事件订阅用户
            List<Map<String, Object>> list = subscribeService.findSubscribeUserId(deviceType, eventType, eventLevel.getLevelCode());
            Set<Integer> users = new HashSet<>();
            if (!CollectionUtils.isEmpty(list)) {
                // 配置推送目标
                List<Target> targets = new ArrayList<>();
                for (Map<String, Object> objectMap : list) {
                    String clientId = (String) ObjectUtil.defaultIfNull(objectMap.get("CLIENT_ID"), "");
                    users.add(NumberUtils.toInt(String.valueOf(objectMap.get("USER_ID"))));
                    Target target = new Target();
                    target.setAppId(Global.appId);
                    target.setClientId(clientId);
                    targets.add(target);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("个推推送设备列表: [{}] ==> 推送事件:{}", JSON.toJSONString(targets, SerializerFeature.WriteNullStringAsEmpty), eventLevel.getDescChs());
                }

                try {
                    Map<String, Object> pushMessage = new HashMap<>();
                    // 设置通知栏标题与内容
                    String title = eventModel.getEventLevel() + "级事件 " + DateUtil.format(eventModel.getData_date(), "HH:mm") + eventModel.getType();
                    String text = eventModel.getEvent();
                    pushMessage.put("title", title);
                    pushMessage.put("text", text);
                    pushMessage.put("content", eventModel);
                    String json = objectMapper.writeValueAsString(pushMessage);

                    IGtPush push = new IGtPush(Global.appKey, Global.masterSecret, true);
                    // 通知透传模板
                    TransmissionTemplate template = transmissionTemplate(json);
                    ListMessage message = new ListMessage();
                    message.setData(template);
                    // 设置消息离线，并设置离线时间
                    message.setOffline(true);
                    // 离线有效时间，单位为毫秒，可选 24小时所属
                    message.setOfflineExpireTime(24 * 1000 * 3600);
                    // taskId用于在推送时去查找对应的message
                    String taskId = push.getContentId(message);
                    IPushResult ret = push.pushMessageToList(taskId, targets);

                    // 存储推送记录
                    Iterator itr = users.iterator();
                    while (itr.hasNext()) {
                        int userId = (int) itr.next();
                        StPushRecord pushRecord = new StPushRecord();
                        pushRecord.setAppName("陆楚楚");
                        pushRecord.setUserId(userId);
                        pushRecord.setPushTitle(title);
                        pushRecord.setPushContent(objectMapper.writeValueAsString(eventModel));
                        pushRecord.setPushTime(new Date());
                        String result = String.valueOf(ret.getResponse().get("result"));
                        if ("ok".equals(result)) {
                            pushRecord.setPushResult("成功推送给用户!");
                        } else {
                            pushRecord.setPushResult("推送失败!" + result);
                        }
                        Global.pushRecords.add(pushRecord);
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("个推推送返回码:{}, 结果说明:{}", ret.getResultCode(), ret.getResponse().toString());
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 透传模板
     *
     * @param json 透传内容
     * @return
     */
    public TransmissionTemplate transmissionTemplate(String json) {
        TransmissionTemplate template = new TransmissionTemplate();
        // 设置APPID与APPKEY
        template.setAppId(Global.appId);
        template.setAppkey(Global.appKey);
        // 透传消息设置，1为强制启动应用，客户端接收到消息后就会立即启动应用；2为等待应用启动
        template.setTransmissionType(2);
        template.setTransmissionContent(json);
        return template;
    }
}
