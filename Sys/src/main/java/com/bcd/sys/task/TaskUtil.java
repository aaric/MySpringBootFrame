package com.bcd.sys.task;

import com.bcd.base.config.redis.RedisUtil;
import com.bcd.base.exception.BaseRuntimeException;
import com.bcd.base.util.IPUtil;
import com.bcd.sys.bean.TaskBean;
import com.bcd.sys.bean.UserBean;
import com.bcd.sys.service.TaskService;
import com.bcd.sys.util.ShiroUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
@Component
public class TaskUtil {

    private static RedisTemplate<String,Map> redisTemplate;

    private static JdbcTemplate jdbcTemplate;

    private static TaskService taskService;

    @Autowired
    public void init(RedisConnectionFactory redisConnectionFactory,JdbcTemplate jdbcTemplate,TaskService taskService){
        TaskUtil.redisTemplate= RedisUtil.newString_JacksonBeanRedisTemplate(redisConnectionFactory,Map.class);
        TaskUtil.jdbcTemplate=jdbcTemplate;
        TaskUtil.taskService=taskService;
    }

    /**
     * 注册任务
     * @param name 任务名称
     * @param consumer 任务执行方法
     * @return
     */
    public static TaskBean registerTask(String name,Consumer<TaskBean> consumer){
        //1、构造任务实体
        UserBean userBean= ShiroUtil.getCurrentUser();
        TaskBean taskBean=new TaskBean(name);
        if(userBean!=null){
            taskBean.setCreateUserName(userBean.getUsername());
            taskBean.setCreateUserId(userBean.getId());
        }
        taskBean.setCreateTime(new Date());
        taskBean.setCreateIp(IPUtil.getIpAddress());
        taskBean.setStatus(TaskStatus.WAITING.getStatus());
        //2、保存任务实体
        TaskUtil.taskService.save(taskBean);
        //3、执行任务
        Future future= CommonConst.SYS_TASK_POOL.submit(new SysTaskRunnable(taskBean,consumer,taskService));
        CommonConst.SYS_TASK_ID_TO_FUTURE_MAP.put(taskBean.getId(),future);
        return taskBean;
    }

    /**
     * 终止任务(无论是正在执行中还是等待中)
     * 原理:
     * 1、将id通过redis channel推送到各个服务器
     * 2、各个服务器获取到要终止的任务id,检查是否在当前服务器中正在执行此任务
     * 3、通过调用Future cancel()方法来终止正在执行的任务,并将通过redis channel推送到发起请求的服务器
     * 4、接收到结果后,由请求服务器更新任务状态到 TaskStatus.STOPPED
     *
     * 注意:
     * 如果是执行中的任务被结束,虽然已经调用Future cancel()但是并不会马上结束,具体原理参考Thread interrupt()
     *
     * @param mayInterruptIfRunning 是否打断正在运行的任务(true表示打断wait或者sleep的任务;false表示只打断在等待中的任务)
     * @param ids
     *
     * @return 结果数组;true代表终止成功;false代表终止失败(可能已经取消或已经完成)
     */
    public static Boolean[] stopTask(boolean mayInterruptIfRunning,Long ...ids){
        if(ids==null||ids.length==0){
            return new Boolean[0];
        }
        //1、生成当前停止任务请求随机编码
        String code= RandomStringUtils.randomAlphanumeric(32);
        //2、构造当前请求的空结果集并加入到全局map
        ConcurrentHashMap<Long,Boolean> resultMap=new ConcurrentHashMap<>();
        CommonConst.SYS_TASK_CODE_TO_RESULT_MAP.put(code,resultMap);
        //3、锁住此次请求的结果map,等待,便于本服务器其他线程收到结果时唤醒
        //3.1、定义退出循环标记
        boolean isFinish=false;
        synchronized (resultMap){
            //3.2、构造请求数据,推送给其他服务器停止任务
            Map<String,Object> dataMap=new HashMap<>();
            dataMap.put("code",code);
            dataMap.put("ids",ids);
            dataMap.put("mayInterruptIfRunning",mayInterruptIfRunning);
            TaskUtil.redisTemplate.convertAndSend(CommonConst.STOP_SYS_TASK_CHANNEL,dataMap);
            try {
                //3.3、设置任务等待超时时间,默认为30s,如果在规定时间内还没有收到所有服务器通知,就不进行等待了,主要是为了解决死循环问题
                long t=30*1000L;
                while(!isFinish&&t>0){
                    long startTs=System.currentTimeMillis();
                    //3.4、等待其他线程唤醒
                    resultMap.wait(t);
                    //3.5、唤醒后,如果检测到接收到的结果集与请求停止任务数量相等,则表示结果已经完整,结束循环
                    if(resultMap.size()==ids.length){
                        isFinish=true;
                    }else{
                        t-=(System.currentTimeMillis()-startTs);
                    }
                }

                //3.6、停止成功的任务更新其任务状态
                List<Long> stopIdList=new ArrayList<>();
                resultMap.forEach((k,v)->{
                    if(v){
                        stopIdList.add(k);
                    }
                });
                if(!stopIdList.isEmpty()) {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("status", TaskStatus.STOPPED.getStatus());
                    paramMap.put("ids", stopIdList);
                    int count = new NamedParameterJdbcTemplate(TaskUtil.jdbcTemplate).update(
                            "update t_sys_task set status=:status,finish_time=now() where id in (:ids)", paramMap);
                }
                //3.7、根据返回的数据构造结果集(结果集不一定准确,因为有可能在规定时间之内没有收到结果,会判定为终止失败)
                return Arrays.stream(ids).map(id->resultMap.getOrDefault(id,false)).toArray(len->new Boolean[len]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw BaseRuntimeException.getException(e);
            }
        }
    }
}
