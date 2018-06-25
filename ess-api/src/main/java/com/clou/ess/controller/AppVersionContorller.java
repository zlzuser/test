package com.clou.ess.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.clou.ess.entity.AppVersionRecord;
import com.clou.ess.model.ResultBody;
import com.clou.ess.service.AppVersionRecordService;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 安卓APP版本控制
 */
@Controller
@RequestMapping("/apk")
public class AppVersionContorller {

    @Autowired
    AppVersionRecordService appVersionRecordService;

    @Value("${web.upload-path}")
    private String webUploadPath;

    @RequestMapping("/upload")
    public String welcome() {
        return "apkupload";
    }

    /**
     * 检查更新
     *
     * @return
     */
    @GetMapping(value = "/verion/update", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResultBody checkUpdates() {
        AppVersionRecord appVersionRecord = appVersionRecordService.selectOne(new EntityWrapper<AppVersionRecord>()
                .eq("APP_TYPE", "02")
                .orderBy("RECORD_ID", false));
        if (!ObjectUtils.isEmpty(appVersionRecord)) {
            return ResultBody.isOk(appVersionRecord);
        }
        return ResultBody.isFail("当前已是最新版本");
    }

    /**
     * 上传APK文件
     *
     * @param file APK文件
     * @return
     */
    @PostMapping(value = "/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResultBody uploadApk(@RequestPart MultipartFile file,
                                @RequestParam("updata_log") String updataLog,
                                @RequestParam(value = "is_install", required = false, defaultValue = "F_NO") String isInstall) {
        if ("application/vnd.android.package-archive".equalsIgnoreCase(file.getContentType())) {
            String temp = "apk".concat(File.separator);
            // 获取APK文件名
            String fileName = file.getOriginalFilename();
            // 文件路径
            String filePath = System.getProperty("user.dir").concat(File.separator).concat(webUploadPath).concat(File.separator).concat(temp);
            try {
                File dest = new File(filePath, fileName);
                if (!dest.getParentFile().exists()) {
                    dest.getParentFile().mkdirs();
                }
                // 上传到指定目录
                file.transferTo(dest);

                // 读取APK文件信息
                ApkFile apkFile = new ApkFile(dest);
                ApkMeta apkMeta = apkFile.getApkMeta();
                int versionCode = apkMeta.getVersionCode().intValue();
                String versionName = apkMeta.getVersionName();
                apkFile.close();

                // 重命名
                String newName = "lcc_v" + versionCode + "_android_" + versionName + "_release.apk";
                File tempFile = new File(filePath, fileName);
                if (tempFile.exists()) {
                    File apkNewFile = new File(filePath, newName);
                    tempFile.renameTo(apkNewFile);
                }
                // 将反斜杠转换为正斜杠
                String data = temp.replaceAll("\\\\", "/") + newName;

                AppVersionRecord appVersionRecord = new AppVersionRecord();
                appVersionRecord.setAppType("02");
                appVersionRecord.setVersionCode(versionCode);
                appVersionRecord.setVersionName(versionName);
                appVersionRecord.setDownloadUrl(data);
                appVersionRecord.setUpdataLog(updataLog);
                appVersionRecord.setIsInstall(isInstall);
                appVersionRecord.setReleaseTime(new Date());
                boolean flag = appVersionRecordService.insert(appVersionRecord);
                if (flag) {
                    return ResultBody.isOk().msg("上传成功!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                return ResultBody.isFail("上传异常!");
            }
        } else {
            return ResultBody.isFail("文件类型不正确，请上传APK文件!");
        }
        return ResultBody.isFail("上传失败");
    }
}
