package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TAmssDutyTaskMapper;
import com.fc.v2.mapper.auto.TAmssDutyTeamMapper;
import com.fc.v2.mapper.auto.TAmssOutCardMapper;
import com.fc.v2.model.auto.TAmssDutyTask;
import com.fc.v2.model.auto.TAmssDutyTeam;
import com.fc.v2.model.auto.TAmssOutCard;
import com.fc.v2.model.custom.DispatchRound;

/**
 * 派单轮次规则单测（不启 Spring、不连库）：倒计时、时辰两段/值守班次、挂起、
 * 不重派、当日同班组去重、超时收回留痕、本轮无事，一个口径逐个数清。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TAmssDutyTaskServiceImplTest {

    @Mock
    private TAmssDutyTaskMapper taskMapper;
    @Mock
    private TAmssDutyTeamMapper teamMapper;
    @Mock
    private TAmssOutCardMapper outCardMapper;

    @InjectMocks
    private TAmssDutyTaskServiceImpl service;

    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date at;
    private Date todayEnd;
    private Date tomorrowEnd;

    private List<TAmssDutyTask> tasks;
    private List<TAmssDutyTeam> teams;

    @BeforeEach
    public void setUp() throws Exception {
        at = fmt.parse(fmt.format(new Date())); // 今天此刻，先由各用例改点
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        todayEnd = fmt.parse(today + " 23:59:59");
        tomorrowEnd = fmt.parse(tomorrow() + " 23:59:59");

        tasks = new ArrayList<TAmssDutyTask>();
        when(taskMapper.selectList(any())).thenAnswer(inv -> tasks);
        when(taskMapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TAmssDutyTask t : tasks) {
                if (id.equals(t.getId())) {
                    return t;
                }
            }
            return null;
        });
        when(taskMapper.updateById(any())).thenAnswer(inv -> {
            // service 改的就是同批对象本身；模拟落库成功
            return 1;
        });
        teams = new ArrayList<TAmssDutyTeam>();
        when(teamMapper.selectList(any())).thenAnswer(inv -> teams);
        when(outCardMapper.selectOne(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            // 真实 MyBatis 绑定参数前会渲染 SQL 段，渲染后参数映射才落值，mock 里照做
            w.getCustomSqlSegment();
            for (Object v : w.getParamNameValuePairs().values()) {
                for (TAmssOutCard c : cards) {
                    if (c.getBizNo().equals(v)) {
                        return c;
                    }
                }
            }
            return null;
        });
    }

    private final List<TAmssOutCard> cards = new ArrayList<TAmssOutCard>();

    private String tomorrow() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_MONTH, 1);
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }

    private Date at(int hh, int mm) throws Exception {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return fmt.parse(String.format("%s %02d:%02d:00", today, hh, mm));
    }

    private Date daysAgo(int days) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_MONTH, -days);
        return new java.sql.Date(zero(c).getTime());
    }

    private Date zero(java.util.Calendar c) {
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private TAmssDutyTeam team(long id, String no, String name, String season, String s, String e, int status) {
        TAmssDutyTeam t = new TAmssDutyTeam();
        t.setId(id);
        t.setTeamNo(no);
        t.setTeamName(name);
        t.setSeason(season);
        t.setShiftStart(s);
        t.setShiftEnd(e);
        t.setStatus(status);
        t.setDelFlag(0);
        return t;
    }

    private TAmssDutyTask task(long id, String itemNo, Date dueAt, String lead, int status,
                               String ownTeam, String cardNo, int timeMode) {
        TAmssDutyTask t = new TAmssDutyTask();
        t.setId(id);
        t.setItemNo(itemNo);
        t.setDueAt(dueAt);
        t.setAmount(lead == null ? null : new BigDecimal(lead));
        t.setStatus(status);
        t.setOwnTeamNo(ownTeam);
        t.setMachineCardNo(cardNo);
        t.setTimeMode(timeMode);
        t.setReassignCount(0);
        t.setDelFlag(0);
        return t;
    }

    private void standardTeams() {
        teams.add(team(1, "T01", "一班", null, null, null, 0));
        teams.add(team(2, "T02", "二班", null, null, null, 0));
    }

    // 1) 晌午段内、倒计时开口：派出，写本次与头一回，账对得上
    @Test
    public void dispatchesInMorningWindow() throws Exception {
        standardTeams();
        tasks.add(task(1, "X001", todayEnd, "1", 0, "T01", null, 0));
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertFalse(r.isIdle());
        assertEquals(1, r.getDueCount());
        assertEquals(1, r.getSentCount());
        assertEquals(1, r.getDispatchedCount());
        assertEquals(0, r.getWaitCount());
        assertEquals(0, r.getHungCount());
        TAmssDutyTask t = tasks.get(0);
        assertEquals(1, t.getStatus().intValue());
        assertEquals("T01", t.getTeamNo());
        assertEquals("T01", t.getFirstTeamNo());
        assertNotNull(t.getFirstSendAt());
        assertNotNull(t.getDispatchedDate());
        // 原签名同为成功派出条数（此处由同账直接核对）
        assertEquals(1, r.getSentCount());
        // 下一轮再跑：已派不回待派、当日不收回，本轮无事，一条都不再派
        DispatchRound again = service.runOnce(at(10, 30), true);
        assertTrue(again.isIdle());
        assertEquals(0, again.getSentCount());
    }

    // 2) 日子到了时辰落在两段之外：先按住，仍是待派
    @Test
    public void holdsOutsideWindow() throws Exception {
        standardTeams();
        tasks.add(task(1, "X001", todayEnd, "1", 0, "T01", null, 0));
        DispatchRound r = service.runOnce(at(12, 30), true);
        assertEquals(0, r.getSentCount());
        assertEquals(1, r.getHeldCount());
        assertEquals(1, r.getWaitCount());
        assertEquals(0, tasks.get(0).getStatus().intValue());
        assertNull(tasks.get(0).getTeamNo());
    }

    // 3) 倒计时还没开口：本轮无事，不是故障
    @Test
    public void idleWhenNotOpenYet() throws Exception {
        standardTeams();
        // 七日后到场、提前 1 日开口：剩 7 > 1，本轮翻不到
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_MONTH, 7);
        Date due = fmt.parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()) + " 23:59:59");
        tasks.add(task(1, "X005", due, "1", 0, "T01", null, 0));
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertTrue(r.isIdle());
        assertEquals("本轮无事", r.getMessage());
        assertEquals(0, r.getDueCount());
    }

    // 4) 牌证年初已收回：挂起并写缘故；同轮后面的条目照常派出（一件不卡一轮）
    @Test
    public void hangsOnRevokedCardAndDoesNotBlockOthers() throws Exception {
        standardTeams();
        TAmssOutCard revoked = new TAmssOutCard();
        revoked.setBizNo("CARD-REVOKE-001");
        revoked.setStatus(2); // 已了结=牌证已收回
        cards.add(revoked);
        TAmssDutyTask hung = task(1, "X002", todayEnd, "1", 0, "T01", "CARD-REVOKE-001", 0);
        TAmssDutyTask ok = task(2, "X001", todayEnd, "1", 0, "T01", null, 0);
        tasks.add(hung);
        tasks.add(ok);
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertEquals(2, r.getDueCount());
        assertEquals(1, r.getHungCount());
        assertEquals(1, r.getSentCount());
        assertEquals(2, hung.getStatus().intValue());
        assertTrue(hung.getHangRemark().contains("CARD-REVOKE-001"));
        assertEquals(1, ok.getStatus().intValue());
        assertEquals("T01", ok.getTeamNo());
    }

    // 5) 所属班组本季整班停摆：挂起
    @Test
    public void hangsWhenOwnTeamSuspended() throws Exception {
        teams.add(team(5, "T05", "停摆班", null, null, null, 1));
        tasks.add(task(1, "X003", todayEnd, "1", 0, "T05", null, 0));
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertEquals(1, r.getHungCount());
        assertEquals(2, tasks.get(0).getStatus().intValue());
        assertTrue(tasks.get(0).getHangRemark().contains("整班停摆"));
    }

    // 6) 重点时期照值守班次：13 点不在普通两段内，但在值守班次内，照样派给值守班
    @Test
    public void peakModeFollowsDutyShift() throws Exception {
        teams.add(team(3, "T03", "三夏甲班", "SX", "08:00", "20:00", 0));
        tasks.add(task(1, "X004", todayEnd, "1", 0, null, null, 1));
        DispatchRound r = service.runOnce(at(13, 0), true);
        assertEquals(1, r.getSentCount());
        assertEquals("T03", tasks.get(0).getTeamNo());

        // 同刻普通条目必须按住
        tasks.clear();
        tasks.add(task(2, "X008", todayEnd, "1", 0, null, null, 0));
        DispatchRound r2 = service.runOnce(at(13, 0), true);
        assertEquals(0, r2.getSentCount());
        assertEquals(1, r2.getHeldCount());
    }

    // 7) 重点时期不在班次内：按住
    @Test
    public void peakModeHoldsOffShift() throws Exception {
        teams.add(team(3, "T03", "三夏甲班", "SX", "08:00", "20:00", 0));
        tasks.add(task(1, "X004", todayEnd, "1", 0, null, null, 1));
        DispatchRound r = service.runOnce(at(7, 30), true);
        assertEquals(0, r.getSentCount());
        assertEquals(1, r.getHeldCount());
    }

    // 8) 班组一轮可接多件（农忙产能）；真正要防的是"一件两班"，在第 11 例按事项级守
    @Test
    public void teamCanTakeMultipleItemsPerRound() throws Exception {
        standardTeams();
        for (long i = 1; i <= 3; i++) {
            tasks.add(task(i, "X" + (100 + i), todayEnd, "1", 0, null, null, 0));
        }
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertEquals(3, r.getSentCount());
        assertEquals(0, r.getHeldCount());
        // 不同事项不互相挡，班组按固定序挑（T01 排第一），三件都派得出去，产能不被错杀
        for (TAmssDutyTask t : tasks) {
            assertEquals("T01", t.getTeamNo());
            assertEquals(1, t.getStatus().intValue());
        }
    }

    // 8b) 同一轮内同一事项两条待派：只派先轮到的一条，第二句按住，一个班都不许接
    @Test
    public void oneItemNotTwoTeamsWithinRound() throws Exception {
        standardTeams();
        tasks.add(task(1, "XDUP", todayEnd, "1", 0, null, null, 0));
        tasks.add(task(2, "XDUP", todayEnd, "1", 0, null, null, 0));
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertEquals(1, r.getSentCount());
        assertEquals(1, r.getHeldCount());
        assertEquals("T01", tasks.get(0).getTeamNo());
        assertNull(tasks.get(1).getTeamNo());
        assertEquals(0, tasks.get(1).getStatus().intValue());
    }

    // 9) 领单过当日没回话：收回重派别家；头一回的班组与时刻不被覆盖，留痕在
    @Test
    public void recallsOverdueAndKeepsFirstTrace() throws Exception {
        standardTeams();
        TAmssDutyTask t = task(1, "X007", tomorrowEnd, "2", 1, null, null, 0);
        t.setTeamNo("T01");
        t.setTeamName("一班");
        t.setDispatchedDate(new java.sql.Date(daysAgo(1).getTime()));
        t.setFirstTeamNo("T01");
        t.setFirstTeamName("一班");
        Date firstSend = fmt.parse("2026-09-20 09:30:00");
        t.setFirstSendAt(firstSend);
        tasks.add(t);
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertEquals(1, r.getRecalledCount());
        assertEquals(1, r.getSentCount());
        // 重派别家
        assertEquals("T02", t.getTeamNo());
        // 头一回不覆盖
        assertEquals("T01", t.getFirstTeamNo());
        assertEquals(firstSend, t.getFirstSendAt());
        // 收回痕迹
        assertTrue(t.getRecoverRemark().contains("收回"));
        assertTrue(t.getRecoverRemark().contains("原派班组 T01"));
        assertEquals(1, t.getReassignCount().intValue());
        assertEquals(1, t.getStatus().intValue());
    }

    // 10) 当日派成的当日不收回（还没过当日）；已派条目下轮不再翻出来重派
    @Test
    public void dispatchedTodayNotRecalledOrRePulled() throws Exception {
        standardTeams();
        TAmssDutyTask t = task(1, "X009", tomorrowEnd, "2", 1, null, null, 0);
        t.setTeamNo("T01");
        t.setDispatchedDate(new java.sql.Date(zero(java.util.Calendar.getInstance()).getTime()));
        t.setFirstTeamNo("T01");
        t.setFirstSendAt(at(9, 0));
        tasks.add(t);
        DispatchRound r = service.runOnce(at(16, 0), true);
        assertTrue(r.isIdle());
        assertEquals("T01", t.getTeamNo());
        assertEquals(0, r.getRecalledCount());
    }

    // 11) 同一事项当日库里已有先落一条（哪怕录成第二行）：第二句整句不留、不派任何班组，
    //     先落那条不动——防止同一件事被两个班组各接一遍
    @Test
    public void sameItemSameDayKeepsFirstOnly() throws Exception {
        standardTeams();
        TAmssDutyTask first = task(1, "X010", todayEnd, "1", 1, null, null, 0);
        first.setTeamNo("T01");
        first.setDispatchedDate(new java.sql.Date(zero(java.util.Calendar.getInstance()).getTime()));
        first.setFirstTeamNo("T01");
        first.setFirstSendAt(at(9, 0));
        TAmssDutyTask second = task(2, "X010", todayEnd, "1", 0, null, null, 0);
        tasks.add(first);
        tasks.add(second);
        DispatchRound r = service.runOnce(at(10, 0), true);
        // first 不进到期清单（已派且当日不收回），second 开口但整句按住，一个班都不许接
        assertEquals(0, r.getSentCount());
        assertEquals(1, r.getHeldCount());
        assertNull(second.getTeamNo());
        assertEquals(0, second.getStatus().intValue());
        assertEquals("T01", first.getTeamNo()); // 先落那条不动
    }

    // 12) 上轮挂起、这轮缘故消了（班组恢复当班）：落回待派照常派出
    @Test
    public void hungRowClearsWhenReasonGone() throws Exception {
        standardTeams(); // T01 当班
        TAmssDutyTask t = task(1, "X011", todayEnd, "1", 2, "T01", null, 0);
        t.setHangRemark("旧挂起缘故");
        tasks.add(t);
        DispatchRound r = service.runOnce(at(10, 0), true);
        assertEquals(1, r.getSentCount());
        assertEquals(1, t.getStatus().intValue());
        assertNull(t.getHangRemark());
    }

    // 13) 倒计时压到当天：amount=0 当天才开口；提前一天不开口
    @Test
    public void countdownBoundarySameDayOnly() throws Exception {
        standardTeams();
        tasks.add(task(1, "X012", todayEnd, "0", 0, null, null, 0));
        DispatchRound today = service.runOnce(at(10, 0), true);
        assertEquals(1, today.getSentCount());

        tasks.clear();
        tasks.add(task(2, "X013", tomorrowEnd, "0", 0, null, null, 0));
        DispatchRound before = service.runOnce(at(10, 0), true);
        assertTrue(before.isIdle());
    }

    // 14) listDue 不翻已派，挂起仍翻得着
    @Test
    public void listDueExcludesDoneKeepsHung() throws Exception {
        standardTeams();
        TAmssDutyTask done = task(1, "X1", todayEnd, "1", 1, null, null, 0);
        done.setTeamNo("T01");
        done.setDispatchedDate(new java.sql.Date(zero(java.util.Calendar.getInstance()).getTime()));
        TAmssDutyTask wait = task(2, "X2", todayEnd, "1", 0, null, null, 0);
        TAmssDutyTask hung = task(3, "X3", todayEnd, "1", 2, null, null, 0);
        tasks.addAll(Arrays.asList(done, wait, hung));
        List<TAmssDutyTask> due = service.listDue(at(12, 0));
        assertEquals(2, due.size());
    }

    // 15) 重载 verbose=false：计数与一句话照填，逐条明细不回填；原签名返回成功派出条数
    @Test
    public void nonVerboseOverloadFillsCountsOnly() throws Exception {
        standardTeams();
        tasks.add(task(1, "X020", todayEnd, "1", 0, "T01", null, 0));
        DispatchRound quiet = service.runOnce(at(10, 0), false);
        assertEquals(1, quiet.getSentCount());
        assertEquals(1, quiet.getDispatchedCount());
        assertTrue(quiet.getItems().isEmpty());

        DispatchRound loud = service.runOnce(at(10, 30), false);
        assertTrue(loud.isIdle()); // 已派不回待派
    }
}
