package com.clou.ess.controller;

import cn.hutool.core.util.NumberUtil;
import com.clou.ess.entity.SUser;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证controller是单例还是多例，并且对非静态成员变量的影响
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/30 15:58
 */
@RestController
@RequestMapping("/demo")
@Scope("prototype")
public class Singleton {
    //静态的
    private static int st = 0;
    //非静态
    private int index = 0;
    @RequestMapping("/show")
    public String toShow(ModelMap model) {
        SUser user = new SUser();
        user.setUserName("testuname");
        user.setUserId(23);
        model.put("user", user);
        return "/lsh/ch5/show";
    }
    @RequestMapping("/test")
    public String test() {
        return st++ + " | " + index++;
    }


    public static void main(String[] args) {
        String ss = "";
        System.out.println(NumberUtil.round(54523,2));
    }

}
