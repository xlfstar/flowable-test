package com.example.flowable.security;

import com.example.flowable.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BizException(401, "用户未登录");
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    public static String getRealName() {
        return getLoginUser().getUser().getRealName();
    }

    public static String getRole() {
        return getLoginUser().getRole();
    }
}
