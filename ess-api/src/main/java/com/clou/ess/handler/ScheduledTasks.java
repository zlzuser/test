package com.clou.ess.handler;

import com.clou.ess.entity.StPushRecord;
import com.clou.ess.service.StPushRecordService;
import com.clou.ess.util.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PreDestroy;
import java.util.List;

/**
 * 定时任务
 */
@Component
public class ScheduledTasks {
    private final Logger              logger = LoggerFactory.getLogger(ScheduledTasks.class);
    @Autowired
    private       StPushRecordService pushRecordService;

    /**
     * 定时批量保存推送记录
     */
    @Scheduled(fixedRate = 50000)
    public void executeFixedRate() {
        savePushRecor();
    }

    /**
     * 服务器销毁之前保存推送记录
     * 建议使用 kill [pid] 正常关闭服务，不建议使用kill -9 pid! 因为kill -9 强制关闭不会调用onDestroy造成数据丢失。
     */
    @PreDestroy
    public void onDestroy() {
        savePushRecor();
        logger.info("陆楚楚服务正常关闭了.");
    }

    /**
     * 批量保存推送记录
     */
    public void savePushRecor() {
        List<StPushRecord> pushRecords = Global.pushRecords;
        if (!CollectionUtils.isEmpty(pushRecords)) {
            pushRecordService.insertBatch(pushRecords);
            Global.pushRecords.clear();
        }
    }
}
