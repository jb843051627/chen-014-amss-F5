package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TAmssDutyLog;
import com.fc.v2.model.auto.TAmssDutyTask;
import com.fc.v2.model.custom.DutyRoundResult;

/**
 * 三夏三秋检修复查与上门服务派单条目 Service接口（scheduling-job 形状：周期执行，无增删改查入口）
 *
 * 倒计时全站唯一口径：开口日 = 到场日（dueAt 当天末尾一刻所在日）往前倒 amount 个自然日；
 * 到了开口日当天即算“还剩 amount 日”，压到当天算该派。挑单、回写都走这一个口径，
 * 口径要挪只许改 amount，谁也不许后推 dueAt。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITAmssDutyTaskService {

    /** 按主键回查条目 */
    TAmssDutyTask selectTAmssDutyTaskById(Long id);

    /** 只读底账列表（页面台账用，不含任何补条/撤条口子） */
    List<TAmssDutyTask> selectList(QueryWrapper<TAmssDutyTask> queryWrapper);

    /** 一件事项的派出/收回流水（头一条不动，按时刻先后排） */
    List<TAmssDutyLog> listLogsByItem(String itemNo);

    /**
     * 该时刻可处理的待派条目：已开口（倒计时口径）+ 落在上门时段内（常规期上门类）+ 尚未处理。
     * 与 {@link #runOnce(Date)} 同一套挑单口径，不另搞一套。
     */
    List<TAmssDutyTask> listDue(Date at);

    /**
     * 执行一次（原有入参与返回不换）：收回超时未回话的、挂起派不出去的、派出开口到期的，
     * 算清写回后返回本轮新派出条数；单条失败跳过继续。
     */
    int runOnce(Date at);

    /**
     * 同名重载，加一种用法：无参，以当前时刻起算，返回本轮整屏账。
     * 叫醒一轮派单的只有周期任务那一侧，调的就是它；屏上的数与它返回的必须一致。
     * 整轮无到期条目时返回 idle=true、message="本轮无事"，不当故障。
     */
    DutyRoundResult runOnce();
}
