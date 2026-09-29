package com.fc.v2.common.quartz.task;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.model.custom.DispatchRound;
import com.fc.v2.service.ITAmssDutyTaskService;
import com.fc.v2.service.ITSysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 *测试类
 * @CLASSNAME   V2Task
 * @Description 定时调度具体工作类
 * @Auther Jan  橙寂
 * @DATE 2019/9/2 0002 15:33
 */
@Component("v2Task")
public class V2Task {

	private static final Logger log = LoggerFactory.getLogger(V2Task.class);

	@Autowired
    private ITSysUserService userService;

	@Autowired
	private ITAmssDutyTaskService amssDutyTaskService;
    /**
     * 无参的任务
     */
    public void runTask1()
    {
        System.out.println("正在执行定时任务，无参方法");
    }

    /**
     * 有参任务
     * 目前仅执行常见的数据类型  Integer Long  带L  string  带 ''  bool Double 带 d
     * @param a
     * @param b
     */
    public void runTask2(Integer a,Long b,String c,Boolean d,Double e)
    {
    	List<TSysUser> list=  userService.selectTSysUserList(new QueryWrapper<TSysUser>());
    	System.err.println("用户查询num："+list.size());
        System.out.println("正在执行定时任务，带多个参数的方法"+a+"   "+b+" "+c+"  "+d+" "+e+"执行时间:"+DateUtil.now());
    }

    /**
     * 三夏三秋派单轮次（周期任务唯一入口；界面侧无补条/撤条口子）。
     * 调度目标配置形如：v2Task.dispatchDutyTask()
     * 翻出、派出、收回、挂起全在业务服务层执行方法内算清再写回，成没成只看返回。
     */
    public void dispatchDutyTask()
    {
        DispatchRound round = amssDutyTaskService.runOnce(new Date(), true);
        // 整轮无事是正常口径，按 info 记，绝不当故障告警
        if (round.isIdle()) {
            log.info("派单轮次 {}：本轮无事", DateUtil.now());
            return;
        }
        log.info("派单轮次 {}：{}（待派{} 已派{} 挂起{}）", DateUtil.now(), round.getMessage(),
                Integer.valueOf(round.getWaitCount()), Integer.valueOf(round.getDispatchedCount()),
                Integer.valueOf(round.getHungCount()));
    }
}
