package com.bcd.base.redis.schedule.aop;

import com.bcd.base.redis.schedule.anno.ClusterFailedSchedule;
import com.bcd.base.redis.schedule.handler.impl.ClusterFailedScheduleHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Created by bcd on 2018/2/12.
 */
@Aspect
@Component
public class ClusterFailedScheduleAopConfig {

    /**
     * 定时任务
     */
    @Pointcut("@annotation(com.bcd.base.redis.schedule.anno.ClusterFailedSchedule)")
    public void methodSchedule(){
    }

    /**
     * 定时任务 环绕通知
     */
    @Around("methodSchedule()")
    public void doAroundSchedule(ProceedingJoinPoint joinPoint){
        //1、获取aop执行的方法
        ClusterFailedScheduleHandler handler=null;
        try {
            Method method=getAopMethod(joinPoint);
            ClusterFailedSchedule anno= method.getAnnotation(ClusterFailedSchedule.class);
            handler= new ClusterFailedScheduleHandler(anno);
            boolean flag=handler.doBeforeStart();
            if(flag){
                Object[] args = joinPoint.getArgs();
                joinPoint.proceed(args);
                handler.doOnSuccess();
            }
        } catch (Throwable throwable) {
            if(handler!=null){
                handler.doOnFailed();
            }
            throwable.printStackTrace();
        }
    }


    private Method getAopMethod(ProceedingJoinPoint joinPoint) throws Exception{
        //拦截的实体类
        Object target = joinPoint.getTarget();
        //拦截的方法名称
        String methodName = joinPoint.getSignature().getName();
        //拦截的放参数类型
        Class[] parameterTypes = ((MethodSignature)joinPoint.getSignature()).getMethod().getParameterTypes();
        return target.getClass().getMethod(methodName, parameterTypes);

    }


}
