package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.mapper.auto.TAmssDutyLogMapper;
import com.fc.v2.mapper.auto.TAmssDutyTaskMapper;
import com.fc.v2.mapper.auto.TAmssDutyTeamMapper;
import com.fc.v2.mapper.auto.TAmssMachCardMapper;
import com.fc.v2.model.auto.TAmssDutyLog;
import com.fc.v2.model.auto.TAmssDutyTask;
import com.fc.v2.model.auto.TAmssDutyTeam;
import com.fc.v2.model.auto.TAmssMachCard;
import com.fc.v2.model.custom.DutyRoundResult;

/**
 * 派单轮次口径单测：倒计时唯一口径、两个上门时段、挂起、当日不重派、超时收回重派、无事轮。
 * 不起 Spring 容器，四个 Mapper 全 Mock；详情断言走包内 runRound，旧 int 签名也单独验。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TAmssDutyTaskServiceImplTest {

    @Mock
    private TAmssDutyTaskMapper taskMapper;
    @Mock
    private TAmssDutyTeamMapper teamMapper;
    @Mock
    private TAmssMachCardMapper cardMapper;
    @Mock
    private TAmssDutyLogMapper logMapper;

    @InjectMocks
    private TAmssDutyTaskServiceImpl service;

    private List<TAmssDutyTask> tasks = new ArrayList<>();
    private List<TAmssDutyTeam> teams = new ArrayList<>();
    private List<TAmssMachCard> cards = new ArrayList<>();
    private List<TAmssDutyLog> logs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(taskMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> tasks);
        when(teamMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> teams);
        when(cardMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> cards);
        when(logMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> logs);
        when(logMapper.selectCount(any(Wrapper.class))).thenReturn(0);
        when(taskMapper.updateById(any())).thenReturn(1);
        when(logMapper.insert(any())).thenReturn(1);

        teams.add(team("T01", 0));
        teams.add(team("T02", 0));
        cards.add(card("G0001", 0));
    }

    // ---------- 构造工具 ----------

    private static Date at(int dayOffsetFromToday, int hour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DATE, dayOffsetFromToday);
        return c.getTime();
    }

    /** 到场日末尾一刻 23:59:59 */
    private static Date dueEnd(int dayOffsetFromToday) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DATE, dayOffsetFromToday);
        return c.getTime();
    }

    private static TAmssDutyTeam team(String no, int status) {
        TAmssDutyTeam t = new TAmssDutyTeam();
        t.setTeamNo(no);
        t.setTeamName(no);
        t.setStatus(status);
        t.setDelFlag(0);
        return t;
    }

    private static TAmssMachCard card(String no, int status) {
        TAmssMachCard c = new TAmssMachCard();
        c.setCardNo(no);
        c.setStatus(status);
        c.setDelFlag(0);
        return c;
    }

    private TAmssDutyTask recheck(String no, int dueOffset, double amount, int status) {
        TAmssDutyTask r = new TAmssDutyTask();
        r.setId((long) no.hashCode());
        r.setItemNo(no);
        r.setItemType(1);
        r.setBusySeason(0);
        r.setDueAt(dueEnd(dueOffset));
        r.setAmount(new BigDecimal(amount));
        r.setTeamNo("T01");
        r.setMachCardNo("G0001");
        r.setContent("检修复查");
        r.setStatus(status);
        r.setDelFlag(0);
        return r;
    }

    private TAmssDutyTask onsite(String no, int busy, int dueOffset, double amount, int status) {
        TAmssDutyTask r = recheck(no, dueOffset, amount, status);
        r.setItemType(2);
        r.setBusySeason(busy);
        r.setContent("上门维修技术服务");
        return r;
    }

    // ---------- 倒计时唯一口径 ----------

    /** 开口日 = 到场日往前倒 amount 日；恰好压到开口当天算该派。 */
    @Test
    void openBoundary_exactOpenDay_dispatches() {
        tasks.add(recheck("IT-01", 2, 2, 0));
        DutyRoundResult res = service.runRound(at(0, 10));
        assertEquals(1, res.getAssignedCount());
        assertEquals(1, res.getEndDispatchedCount());
        assertEquals("T01", tasks.get(0).getAssignedTeamNo());
    }

    /** 还差一天才开口，本轮不动，也不是故障——只是无事。 */
    @Test
    void notOpenYet_idle() {
        tasks.add(recheck("IT-02", 3, 2, 0));
        DutyRoundResult res = service.runRound(at(0, 10));
        assertTrue(res.isIdle());
        assertEquals("本轮无事", res.getMessage());
        verify(taskMapper, never()).updateById(any());
    }

    /** amount=0，压到到场当天，当然该派。 */
    @Test
    void dueToday_zeroAhead_dispatches() {
        tasks.add(recheck("IT-03", 0, 0, 0));
        DutyRoundResult res = service.runRound(at(0, 10));
        assertEquals(1, res.getAssignedCount());
    }

    // ---------- 两个上门时段 ----------

    /** 常规期上门，13 点落在九至十一、十五至十八两段之外：按住等下轮，仍是待派。 */
    @Test
    void onsiteOutsideWindow_heldBack() {
        tasks.add(onsite("IT-10", 0, 0, 0, 0));
        DutyRoundResult res = service.runRound(at(0, 13));
        assertEquals(0, res.getAssignedCount());
        assertEquals(0, tasks.get(0).getStatus().intValue());
        assertEquals(1, res.getEndWaitingCount());
        assertTrue(res.getWaitingItems().get(0).contains("时段之外"));
    }

    @Test
    void onsiteInsideWindow_dispatches() {
        tasks.add(onsite("IT-11", 0, 0, 0, 0));
        assertEquals(1, service.runRound(at(0, 10)).getAssignedCount());
    }

    /** 三夏三秋重点期照值守班次走，13 点也不受两段约束。 */
    @Test
    void busySeason_ignoresWindow() {
        tasks.add(onsite("IT-12", 1, 0, 0, 0));
        assertEquals(1, service.runRound(at(0, 13)).getAssignedCount());
    }

    /** 检修复查不是上门类，时段外照常派。 */
    @Test
    void recheck_notGatedByWindow() {
        tasks.add(recheck("IT-13", 0, 0, 0));
        assertEquals(1, service.runRound(at(0, 13)).getAssignedCount());
    }

    /** 时段边界：九含、十一不含、十五含、十八不含。 */
    @Test
    void windowBoundaries() {
        tasks.add(onsite("IT-14", 0, 0, 0, 0));
        assertEquals(1, service.runRound(at(0, 9)).getAssignedCount());

        tasks.clear();
        tasks.add(onsite("IT-15", 0, 0, 0, 0));
        assertEquals(0, service.runRound(at(0, 11)).getAssignedCount());

        tasks.clear();
        tasks.add(onsite("IT-16", 0, 0, 0, 0));
        assertEquals(1, service.runRound(at(0, 15)).getAssignedCount());

        tasks.clear();
        tasks.add(onsite("IT-17", 0, 0, 0, 0));
        assertEquals(0, service.runRound(at(0, 18)).getAssignedCount());
    }

    // ---------- 挂起 ----------

    /** 所属班组整季停摆：挂起并写清缘故，页面仍翻得着（状态 2，不删除）。 */
    @Test
    void teamHalted_heldWithReason() {
        tasks.add(recheck("IT-20", 0, 0, 0));
        tasks.get(0).setTeamNo("T03");
        teams.add(team("T03", 1));
        DutyRoundResult res = service.runRound(at(0, 10));
        assertEquals(2, tasks.get(0).getStatus().intValue());
        assertTrue(tasks.get(0).getHoldReason().contains("整班停摆"));
        assertEquals(1, res.getEndHeldCount());
        verify(logMapper, never()).insert(any());
    }

    /** 机具牌证年初已收回：挂起。 */
    @Test
    void cardRevoked_heldWithReason() {
        tasks.add(recheck("IT-21", 0, 0, 0));
        tasks.get(0).setMachCardNo("G0002");
        cards.add(card("G0002", 1));
        DutyRoundResult res = service.runRound(at(0, 10));
        assertEquals(2, tasks.get(0).getStatus().intValue());
        assertTrue(tasks.get(0).getHoldReason().contains("已收回"));
        assertEquals(1, res.getEndHeldCount());
    }

    /** 一件挂起不能卡住后面的条目。 */
    @Test
    void heldItem_doesNotBlockRound() {
        TAmssDutyTask held = recheck("IT-22", 0, 0, 0);
        held.setTeamNo("T03");
        teams.add(team("T03", 1));
        tasks.add(held);
        tasks.add(recheck("IT-23", 0, 0, 0));
        DutyRoundResult res = service.runRound(at(0, 10));
        assertEquals(1, res.getAssignedCount());
        assertEquals(1, res.getEndHeldCount());
    }

    // ---------- 派出去不许回待派 / 收回重派 ----------

    /** 今天刚派、当日还没过：下轮不得翻出来重派，一笔都不许写。 */
    @Test
    void dispatchedToday_notRetouched() {
        TAmssDutyTask r = recheck("IT-30", 0, 0, 1);
        r.setAssignedTeamNo("T01");
        r.setFirstTeamNo("T01");
        r.setAssignTime(at(0, 9));
        r.setFirstAssignTime(at(0, 9));
        tasks.add(r);
        DutyRoundResult res = service.runRound(at(0, 10));
        assertTrue(res.isIdle());
        verify(taskMapper, never()).updateById(any());
        verify(logMapper, never()).insert(any());
    }

    /** 昨日派出当日没回话：收回留痕、重派别家；头回班组/时刻不被覆盖。 */
    @Test
    void overdueRecall_reassignOther_keepFirst() {
        Date yesterday = at(-1, 10);
        TAmssDutyTask r = recheck("IT-31", 0, 0, 1);
        r.setAssignedTeamNo("T01");
        r.setFirstTeamNo("T01");
        r.setAssignTime(yesterday);
        r.setFirstAssignTime(yesterday);
        tasks.add(r);
        // 流水里记着头一遍派给 T01，pickTeam 据此不再派回 T01
        TAmssDutyLog firstAssign = new TAmssDutyLog();
        firstAssign.setItemNo("IT-31");
        firstAssign.setTeamNo("T01");
        firstAssign.setAction(TAmssDutyLog.ACTION_ASSIGN);
        logs.add(firstAssign);

        DutyRoundResult res = service.runRound(at(0, 10));

        assertEquals(1, res.getRecalledCount());
        assertEquals(1, res.getAssignedCount());
        assertEquals("T02", r.getAssignedTeamNo(), "收回后必须派别家");
        assertEquals("T01", r.getFirstTeamNo(), "头一回派给谁不能被覆盖");
        assertEquals(yesterday, r.getFirstAssignTime(), "头一回几点派出不能被覆盖");
        assertNull(r.getHoldReason());
        // 一笔收回 + 一笔重派
        verify(logMapper, times(2)).insert(any(TAmssDutyLog.class));
    }

    /** 同事项同班组当日第二遍：库里只留前一遍，第二遍不落。 */
    @Test
    void sameItemSameTeamSameDay_secondAssignDropped() {
        tasks.add(recheck("IT-32", 0, 0, 0));
        teams.removeIf(t -> t.getTeamNo().equals("T02"));
        // 当日给 T01 的派出流水已存在（selectCount 命中即视为头一条已落）
        when(logMapper.selectCount(any(Wrapper.class))).thenReturn(1);
        DutyRoundResult res = service.runRound(at(0, 10));
        assertEquals(0, res.getAssignedCount());
        assertEquals(0, tasks.get(0).getStatus().intValue());
        verify(logMapper, never()).insert(any());
    }

    // ---------- 轮次健壮性 ----------

    /** 单条落库失败跳过，不连累同轮其他条目。 */
    @Test
    void oneFailure_skipsAndContinues() {
        tasks.add(recheck("IT-40", 0, 0, 0));
        tasks.add(recheck("IT-41", 0, 0, 0));
        when(taskMapper.updateById(any())).thenAnswer(inv -> {
            TAmssDutyTask r = inv.getArgument(0);
            if ("IT-40".equals(r.getItemNo())) {
                throw new RuntimeException("模拟落库失败");
            }
            return 1;
        });
        DutyRoundResult res = service.runRound(at(0, 10));
        assertEquals(1, res.getAssignedCount());
        assertTrue(res.getAssignedItems().get(0).startsWith("IT-41"));
    }

    /** 一条待派、一条已派(今日)、一条未开口：只有待派那条在本轮账上，其余两不碰。 */
    @Test
    void mixedRound_onlyDueTouched() {
        tasks.add(recheck("IT-50", 0, 0, 0));
        TAmssDutyTask dispatched = recheck("IT-51", 0, 0, 1);
        dispatched.setAssignedTeamNo("T02");
        dispatched.setAssignTime(at(0, 8));
        dispatched.setFirstTeamNo("T02");
        dispatched.setFirstAssignTime(at(0, 8));
        tasks.add(dispatched);
        tasks.add(recheck("IT-52", 9, 2, 0));

        DutyRoundResult res = service.runRound(at(0, 10));
        assertFalse(res.isIdle());
        assertEquals(1, res.getScopeItemNos().size());
        assertEquals("IT-50", res.getScopeItemNos().get(0));
        assertEquals(1, res.getEndDispatchedCount());
        assertEquals("T02", dispatched.getAssignedTeamNo());
    }

    @Test
    void emptyTable_idle() {
        tasks.clear();
        DutyRoundResult res = service.runRound(at(0, 10));
        assertTrue(res.isIdle());
        assertEquals("本轮无事", res.getMessage());
        assertEquals(0, res.getAssignedCount());
    }

    /** 旧 int 返回签名仍可用，返回本轮新派出条数。 */
    @Test
    void legacyIntSignature_returnsAssignedCount() {
        tasks.add(recheck("IT-60", 0, 0, 0));
        int ok = service.runOnce(at(0, 10));
        assertEquals(1, ok);
    }
}
