package com.bcd.config.shiro;

import com.bcd.config.define.ErrorDefine;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义的authc的过滤器，替换shiro默认的过滤器
 * 继承此类重写 验证不通过的方法,是为了处理失败时候返回的结果集
 * 流程如下:
 * spring容器内发生异常逻辑如下:
 * @RequiresAuthentication 注解发生异常->spring的CustomExceptionHandler拦截转换为结果集->.....
 * 在spring中发生异常后,异常会在spring容器内就转换为结果集,而不会抛出到过滤器链来,所以是不会触发onAccessDenied方法
 *
 * 非spring容器发生异常逻辑如下
 * 过滤器链发生异常->调用过滤器的onAccessDenied方法处理
 *
 * 如下此应用场景:
 * Map<String, String> filterChainMap = new LinkedHashMap<String, String>();
 * filterChainMap.put("/api/**","authc, roles[admin,user], perms[file:edit]");
 * factoryBean.setFilterChainDefinitionMap(filterChainMap);
 * 此时相当于:
 * 访问/api/**
 * 必须走authc过滤器(验证用户登陆)
 * 必须走roles过滤器(验证有角色 admin,user)
 * 必须走perms过滤器(验证有权限 file:edit)
 */
@SuppressWarnings("unchecked")
public class MyAuthenticationFilter extends BasicHttpAuthenticationFilter {
    private HttpMessageConverter converter;

    public MyAuthenticationFilter(HttpMessageConverter converter) {
        this.converter=converter;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        boolean loggedIn = false; //false by default or we wouldn't be in this method
        if (isLoginAttempt(request, response)) {
            loggedIn = executeLogin(request, response);
        }
        if (!loggedIn) {
            response(response);
        }
        return loggedIn;
    }

    /**
     * 采用 spring 自带的转换器转换结果,输出结果
     * @param response
     * @throws IOException
     */
    private void response(ServletResponse response) throws IOException {
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        ServletServerHttpResponse servletServerHttpResponse=new ServletServerHttpResponse(httpResponse);
        converter.write(ErrorDefine.ERROR_SHIRO_UNAUTHENTICATED.toJsonMessage(),
                MediaType.APPLICATION_JSON_UTF8,
                servletServerHttpResponse);
    }
}
