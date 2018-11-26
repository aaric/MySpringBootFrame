package com.bcd.config.exception;

import com.bcd.base.message.JsonMessage;
import com.bcd.base.util.ExceptionUtil;
import com.bcd.config.exception.handler.ExceptionResponseHandler;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义异常处理器,在ExceptionInitListener中替换掉spring默认的异常处理器
 */
@SuppressWarnings("unchecked")
public class CustomExceptionHandler extends DefaultHandlerExceptionResolver {
    private ExceptionResponseHandler handler;

    public CustomExceptionHandler(ExceptionResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response, Object object, Exception exception) {
        //1、先采用默认spring的异常处理
        ModelAndView res=super.resolveException(request,response,object,exception);
        if(res!=null){
            return res;
        }
        //2、判断response
        if(response.isCommitted()){
            return new ModelAndView();
        }
        //3、打印异常
        ExceptionUtil.printException(exception);

        //4、使用异常handler处理异常
        try {
            handler.handle(response,exception);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView();
    }


    /**
     * 自定义参数验证错误信息
     * @param ex
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws IOException
     */
    @Override
    protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
        String message=ex.getBindingResult().getAllErrors().stream().map(e->e.getDefaultMessage()).filter(e->e!=null).reduce((e1,e2)->e1+","+e2).orElse("");
        JsonMessage result=JsonMessage.fail(message);
        try {
            this.handler.handle(response,result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView();
    }


}