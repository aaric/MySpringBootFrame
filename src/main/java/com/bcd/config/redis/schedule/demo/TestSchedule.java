package com.bcd.config.redis.schedule.demo;

import com.bcd.config.redis.schedule.anno.ClusterFailedSchedule;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;

//@Component
//@Configurable
//@EnableScheduling
public class TestSchedule {

    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "*/3 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    @ClusterFailedSchedule(lockId = "bcd-test",timeOut = 2000L)
    public void test1(){
        String a=null;
        System.out.println(Thread.currentThread().getId()+" exception===========test1 "+sdf.format(new Date()));
        a.toString();
        System.out.println("===========test1 "+sdf.format(new Date()));
    }

    @Scheduled(cron = "*/3 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    @ClusterFailedSchedule(lockId = "bcd-test",timeOut = 2000L)
    public void test2(){
        String a=null;
        System.out.println(Thread.currentThread().getId()+" exception===========test2 "+sdf.format(new Date()));
        a.toString();
        System.out.println("===========test2 "+sdf.format(new Date()));
    }

    @Scheduled(cron = "*/3 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    @ClusterFailedSchedule(lockId = "bcd-test",timeOut = 2000L)
    public void test3(){
        String a=null;
        System.out.println(Thread.currentThread().getId()+" exception===========test3 "+sdf.format(new Date()));
        a.toString();
        System.out.println("===========test3 "+sdf.format(new Date()));
    }

    @Scheduled(cron = "*/3 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    @ClusterFailedSchedule(lockId = "bcd-test",timeOut = 2000L)
    public void test4(){
        System.out.println(Thread.currentThread().getId()+"===========test4 "+sdf.format(new Date()));
    }
}
