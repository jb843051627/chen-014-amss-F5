package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TAmssDutyTask;
import com.fc.v2.model.custom.DispatchRound;

/**
 * 三夏三秋检修复查与上门服务派单条目 Service接口（scheduling-job 形状：周期执行，无增删改查入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITAmssDutyTaskService {

    /** 按主键回查条目 */
    TAmssDutyTask selectTAmssDutyTaskById(Long id);

    /** 该时刻可处理的条目（倒计时开口窗口内 + 尚未处理；含时辰不到先按住的与应当挂起的） */
    List<TAmssDutyTask> listDue(Date at);

    /**
     * 执行一次，返回**成功派出条数**；单条失败跳过继续。
     * 原有入参与返回不换；要看本轮整账（待派/已派/挂起/收回/无事）用同名重载 {@link #runOnce(Date, boolean)}。
     */
    int runOnce(Date at);

    /**
     * 同一个方法名下的重载：verbose=true 时返回本轮整账；verbose=false 与 {@link #runOnce(Date)} 完全同口径，
     * 整账只填成功派出条数（其余计数不保证）。屏幕上的数与本方法返回必须一致。
     */
    DispatchRound runOnce(Date at, boolean verbose);
}
