package com.clou.ess.interceptor;

import com.clou.ess.exception.BootException;
import com.clou.ess.service.SUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 验证token拦截器
 */
@Component
public class TokenInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private SUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("user_id");
        String token  = request.getHeader("token");

        // 免登陆账户
        if (StringUtils.isNotEmpty(userId) && "137".equals(userId)) {
            return true;
        }

        // 校验参数是否为Empty
        if (StringUtils.isBlank(userId)) {
            throw new MissingServletRequestParameterException("user_id", "header");
        }
        if (StringUtils.isBlank(token)) {
            throw new MissingServletRequestParameterException("token", "header");
        }

        if (!checkToken(userId, token)) {
            throw new BootException(-1, "登陆过期,请重新登陆!");
        }
        return true;
    }

    /**
     * 校验token
     *
     * @param userId 用户ID
     * @param token  token码
     * @return
     */
    public Boolean checkToken(String userId, String token) {
        String _token = userService.selectToken(Integer.parseInt(userId));
        if (StringUtils.isNoneBlank(_token) && _token.equals(token)) {
            return true;
        }
        return false;
    }
}
