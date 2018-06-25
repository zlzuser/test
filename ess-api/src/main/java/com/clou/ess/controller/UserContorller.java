package com.clou.ess.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.toolkit.IdWorker;
import com.clou.ess.entity.CacheElement;
import com.clou.ess.entity.SUser;
import com.clou.ess.entity.StAppFeedback;
import com.clou.ess.model.ResultBody;
import com.clou.ess.model.UserModel;
import com.clou.ess.service.SUserService;
import com.clou.ess.service.StAppFeedbackService;
import com.clou.ess.util.CommonUtil;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 登陆控制器
 */
@RestController
public class UserContorller {


    @Autowired
    CacheManager cacheManager;

    @Autowired
    private SUserService userService;

    @Autowired
    private StAppFeedbackService stAppFeedbackService;

    @Value("${web.upload-path}")
    private String webUploadPath;

    /**
     * 登陆
     *
     * @param uname 用户名
     * @param pwd   密码
     * @return
     */
    @GetMapping(value = "/login", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody<UserModel> login(@RequestParam String uname, @RequestParam String pwd, @RequestParam(required = false) String cid) {
        Cache cache = cacheManager.getCache("userLock");
        CacheElement cacheElement = new CacheElement();
        long time = System.currentTimeMillis();
        CacheElement oldElement = cache.get(uname)!=null?(CacheElement)cache.get(uname).getObjectValue():null;
        if(cache.get(uname)!=null){
            if(oldElement.getCount()>3){
                if((time-oldElement.getDatetime())/1000<300){
                    long miao = 300-(time-oldElement.getDatetime())/1000;
                    return ResultBody.isFail().msg("密码多次输入错误,请等待"+miao+"秒重新操作");
                }
            }
        }
        SUser user = userService.selectOne(new EntityWrapper<SUser>()
                .orNew("USER_NO = {0}", uname).or("USER_MOBILE = {0}", uname));

        if (null != user) {
            UserModel userModel = new UserModel()
                    .userId(user.getUserId())
                    .userName(user.getUserName());
            String role = userService.selectRoles(user.getUserId());
            if (StringUtils.isNotEmpty(role)) {
                userModel.incomeRole(role);
            } else {
                return ResultBody.isFail("没有陆楚楚登陆权限!");
            }
            // 校验密码
            if (pwd.equals(user.getUserPwd())) {
                String token = IdWorker.get32UUID();
                userModel.token(token);
                // 修改用户token令牌
                user.setLoginCode(token);
                user.setClientId(cid);
                boolean flag = userService.updateUser(user);
                // 判断token是否修改成功
                if (flag) {
                    return ResultBody.isOk(userModel).msg("登陆成功！");
                } else {
                    return ResultBody.isFail("Token修改失败!");
                }
            } else {
                if(cache.get(uname)!=null){
                    int count = oldElement.getCount();
                    if(count<=3){
                        cache.remove(uname);
                        count += 1;
                        cacheElement.setCount(count).setDatetime(time);
                        Element unamecache = new Element(uname,cacheElement);
                        cache.put(unamecache);
                    }else{
                        cache.remove(uname);
                    }
                }else{
                    cache.put(new Element(uname,cacheElement.setCount(1).setDatetime(time)));
                }
                return ResultBody.isFail("密码错误!");
            }
        } else {
            return ResultBody.isFail("账户不存在!");
        }
    }

    /**
     * 退出登录
     *
     * @param userId 用户ID
     */
    @GetMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResultBody logout(@RequestHeader("user_id") Integer userId) {
        // 清空ClientId和Token
        boolean flag = userService.updateClientIdAndToken(userId);
        if (flag) {
            return ResultBody.isOk().msg("退出成功!");
        }
        return ResultBody.isOk().msg("退出失败!");
    }

    /**
     * 修改密码
     *
     * @param userId  用户ID
     * @param old_pwd 旧密码
     * @param new_pwd 新密码
     * @return
     */
    @RequestMapping(value = "/password/update", headers = "api_version=1.0")
    public ResultBody setPassword(@RequestHeader("user_id") String userId, @RequestParam String old_pwd,
                                  @RequestParam String new_pwd) {
        SUser user = userService.selectOne(new EntityWrapper<SUser>().eq("USER_ID", userId));
        String pwd = user.getUserPwd();
        if (user != null) {
            if (pwd.equals(old_pwd)) {
                user.setUserPwd(new_pwd);
                boolean flag = userService.updateUser(user);
                if (flag) {
                    return ResultBody.isOk().msg("密码修改成功！");
                }
                return ResultBody.isFail("密码修改失败！");
            } else {
                return ResultBody.isFail("旧密码输入有误！");
            }
        } else {
            return ResultBody.isFail("账户不存在!");
        }
    }

    /**
     * 用户注册
     * @param uname
     * @param psw
     * @return
     */
    @RequestMapping(value="/register",headers = "api_version=1.0")
    public ResultBody register(@RequestParam("uname") String uname,@RequestParam("psw") String psw){
        if(userService.validateUserExist(uname)>0){
            return ResultBody.isFail("用户名已经存在！");
        }
        SUser user = new SUser();
        user.setUserPwd(psw);
        user.setUserName(uname);
        boolean register = userService.registerUser(user);
        if(register){
            UserModel userModel = new UserModel()
                    .userId(user.getUserId())
                    .userName(user.getUserName());
            String role = userService.selectRoles(user.getUserId());
            if (StringUtils.isNotEmpty(role)) {
                userModel.incomeRole(role);
            } else {
                return ResultBody.isFail("没有陆楚楚登陆权限!");
            }
            String token = IdWorker.get32UUID();
            userModel.token(token);
            // 修改用户token令牌
            user.setLoginCode(token);
            boolean flag = userService.updateUser(user);
            // 判断token是否修改成功
            if (flag) {
                return ResultBody.isOk(userModel).msg("注册用户成功！");
            } else {
                return ResultBody.isFail("Token修改失败!");
            }
        }else{
            return ResultBody.isFail().msg("注册用户失败！");
        }
    }

    /**
     * 意见反馈
     *
     * @param userId  用户ID
     * @param content 反馈内容
     * @param attr    附件
     * @return
     */
    @PostMapping(value = "/feedback", headers = "api_version=1.0", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultBody feedback(@RequestHeader("user_id") Integer userId,
                               @RequestPart(value = "attr", required = false) MultipartFile attr,
                               @RequestParam("fback_content") String content) {

        if (StringUtils.isBlank(content)) {
            return ResultBody.isFail().msg("反馈内容不能为空！");
        }
        StAppFeedback stAppFeedback = new StAppFeedback();
        stAppFeedback.setUserId(userId);
        stAppFeedback.setFbackContent(content);
        if (attr != null) {
            if (attr.getContentType().contains("image/")) {
                String path = "feedback" + File.separator + "images" + File.separator;
                //获取文件名
                String fileName = attr.getOriginalFilename();
                //新的文件名
                String newName = CommonUtil.rename(fileName);
                String diratorPath = path.concat(String.valueOf(userId)).concat(File.separator);
                //文件完整路径
                String filePath = System.getProperty("user.dir").concat(File.separator).concat(webUploadPath).concat(File.separator).concat(diratorPath);
                File destFile = new File(filePath, newName);
                //判断文件夹是否存在
                if (!destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }
                try {
                    attr.transferTo(destFile);
                    String data = diratorPath.replaceAll("\\\\", "/") + newName;
                    stAppFeedback.setAttr(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    return ResultBody.isFail().msg("反馈失败！");
                }
            } else {
                return ResultBody.isFail().msg("上传文件格式不正确！");
            }
        }
        boolean flag = stAppFeedbackService.setAppletSizeMessage(stAppFeedback);
        if (flag) {
            return ResultBody.isOk().msg("反馈成功！");
        }
        return ResultBody.isOk().msg("反馈失败！");
    }

}
