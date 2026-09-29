package com.fc.v2.common.quartz.task;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.model.custom.DutyRoundResult;
import com.fc.v2.service.ITAmssDutyTaskService;
import com.fc.v2.service.ITSysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

	private static final Logger logger = LoggerFactory.getLogger(V2Task.class);

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
     * 三夏三秋派单轮次。叫醒一轮派单的只有周期任务这一侧：界面不留补条口子、不留撤条把手。
     * 本轮翻出哪些、派出几条、收回几条，全在业务服务层执行方法里算清写回；
     * 这里只认它返回的一屏账并落日志，不另算。整轮无到期即“本轮无事”，不当故障。
     * 调度配置 invokeTarget：v2Task.dutyDispatchRound()
     */
    public void dutyDispatchRound()
    {
        DutyRoundResult result = amssDutyTaskService.runOnce();
        logger.info("派单轮次@{}：{}", DateUtil.now(), result.getMessage());
        for (String line : result.getAssignedItems()) {
            logger.info("派单轮次-派出 {}", line);
        }
        for (String line : result.getRecalledItems()) {
            logger.info("派单轮次-收回 {}", line);
        }
        for (String line : result.getHeldItems()) {
            logger.info("派单轮次-挂起 {}", line);
        }
        for (String line : result.getWaitingItems()) {
            logger.info("派单轮次-按住 {}", line);
        }
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
}
