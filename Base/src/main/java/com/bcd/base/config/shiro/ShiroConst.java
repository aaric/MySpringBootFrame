package com.bcd.config.shiro;


import com.bcd.base.message.ErrorMessage;
import com.bcd.config.define.MessageDefine;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.session.UnknownSessionException;

import java.util.HashMap;

/**
 * Created by Administrator on 2017/7/26.
 */
public class ShiroConst {
    public final static HashMap<String, ErrorMessage> EXCEPTION_ERROR_MESSAGE_MAP = new HashMap<>();

    static {
        //配置shiro的异常对应的ErrorMessage
        EXCEPTION_ERROR_MESSAGE_MAP.put(UnknownAccountException.class.getName(), MessageDefine.ERROR_SHIRO_UNKNOWN_ACCOUNT);
        EXCEPTION_ERROR_MESSAGE_MAP.put(IncorrectCredentialsException.class.getName(), MessageDefine.ERROR_SHIRO_INCORRECT_CREDENTIALS);
        EXCEPTION_ERROR_MESSAGE_MAP.put(DisabledAccountException.class.getName(), MessageDefine.ERROR_SHIRO_DISABLED_ACCOUNT);
        EXCEPTION_ERROR_MESSAGE_MAP.put(AuthenticationException.class.getName(), MessageDefine.ERROR_SHIRO_AUTHENTICATION);
        EXCEPTION_ERROR_MESSAGE_MAP.put(UnauthenticatedException.class.getName(), MessageDefine.ERROR_SHIRO_UNAUTHENTICATED);
        EXCEPTION_ERROR_MESSAGE_MAP.put(ExpiredCredentialsException.class.getName(), MessageDefine.ERROR_SHIRO_EXPIRED_CREDENTIALS);
        EXCEPTION_ERROR_MESSAGE_MAP.put(AuthorizationException.class.getName(), MessageDefine.ERROR_SHIRO_AUTHORIZATION);
        EXCEPTION_ERROR_MESSAGE_MAP.put(UnauthorizedException.class.getName(), MessageDefine.ERROR_SHIRO_AUTHORIZATION);
        EXCEPTION_ERROR_MESSAGE_MAP.put(UnknownSessionException.class.getName(), MessageDefine.ERROR_SHIRO_UNKNOWNSESSIONEXCEPTION);
    }
}