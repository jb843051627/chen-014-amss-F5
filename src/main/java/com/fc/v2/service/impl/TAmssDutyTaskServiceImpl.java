package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TAmssDutyLogMapper;
import com.fc.v2.mapper.auto.TAmssDutyTaskMapper;
import com.fc.v2.mapper.auto.TAmssDutyTeamMapper;
import com.fc.v2.mapper.auto.TAmssMachCardMapper;
import com.fc.v2.model.auto.TAmssDutyLog;
import com.fc.v2.model.auto.TAmssDutyTask;
import com.fc.v2.model.auto.TAmssDutyTeam;
import com.fc.v2.model.auto.TAmssMachCard;
import com.fc.v2.model.custom.DutyRoundResult;
import com.fc.v2.service.ITAmssDutyTaskService;

/**
 * 三夏三秋检修复查与上门服务派单条目 Service业务层处理（scheduling-job 形状：周期执行，无补条/撤条入口）。
 *
 * 全站唯一倒计时口径：开口日 = dueAt（到场当天末尾一刻）所在自然日 往前倒 amount 个自然日；
 * 挑单与回写都按它，压到开口当天即算该派。要挪口径只许改 amount，不得后推 dueAt。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TAmssDutyTaskServiceImpl implements ITAmssDutyTaskService {

    private static final Logger log = LoggerFactory.getLogger(TAmssDutyTaskServiceImpl.class);

    /** 条目情形 0待派 1已派 2挂起(派不出去) */
    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DISPATCHED = 1;
    private static final int STATUS_HELD = 2;

    /** 事项类别 1检修复查 2上门维修技术服务 3跨区作业服务点值守 */
    private static final int TYPE_RECHECK = 1;
    private static final int TYPE_ONSITE = 2;
    private static final int TYPE_DUTY_POINT = 3;

    /** 常规期上门两个可派时段（24小时制，左闭右开）：九至十一、十五至十八 */
    private static final int WIN1_FROM = 9;
    private static final int WIN1_TO = 11;
    private static final int WIN2_FROM = 15;
    private static final int WIN2_TO = 18;

    /** 班组情形 0当班 1整季停摆 */
    private static final int TEAM_HALT = 1;
    /** 牌证情形 0有效 1年初已收回 */
    private static final int CARD_REVOKED = 1;

    @javax.annotation.Resource
    private TAmssDutyTaskMapper amssDutyTaskMapper;
    @javax.annotation.Resource
    private TAmssDutyTeamMapper amssDutyTeamMapper;
    @javax.annotation.Resource
    private TAmssMachCardMapper amssMachCardMapper;
    @javax.annotation.Resource
    private TAmssDutyLogMapper amssDutyLogMapper;

    @Override
    public TAmssDutyTask selectTAmssDutyTaskById(Long id) {
        return this.amssDutyTaskMapper.selectById(id);
    }

    @Override
    public List<TAmssDutyTask> selectList(QueryWrapper<TAmssDutyTask> queryWrapper) {
        return this.amssDutyTaskMapper.selectList(queryWrapper);
    }

    @Override
    public List<TAmssDutyLog> listLogsByItem(String itemNo) {
        return this.amssDutyLogMapper.selectList(new QueryWrapper<TAmssDutyLog>()
                .eq("item_no", itemNo)
                .orderByAsc("action_time", "id"));
    }

    /**
     * 日期比较统一走<b>墙钟字符串</b>（按本机时区格式化）：JDBC 的 serverTimezone 与本机
     * 时区不对称，直接拿 java.util.Date 比会整体偏移，使“恰好开口/恰好次日”这类边界用例错判。
     * 挑单与回写都走这里，全站一个口径。
     */
    private static final ThreadLocal<SimpleDateFormat> DAY_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    private static String dayKey(Date d) {
        return DAY_FMT.get().format(d).replace("-", "");
    }

    /** 日历日比较用的 sql.date，与 {@link #dayKey} 同一格式化口径。 */
    private static java.sql.Date sqlDay(Date d) {
        return java.sql.Date.valueOf(DAY_FMT.get().format(d));
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v.intValue();
    }

    /** 开口日（00:00:00）= 到场日往前倒 amount 个自然日。挑单、回写都认这一个算法。 */
    private Date openAt(TAmssDutyTask r) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(r.getDueAt());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int ahead = r.getAmount() == null ? 0 : Math.max(0, r.getAmount().setScale(0, BigDecimal.ROUND_DOWN).intValue());
        cal.add(Calendar.DATE, -ahead);
        return cal.getTime();
    }

    /** 是否已开口：at 已压到开口日当天（含）——压到当天算该派，全站一个口径。 */
    private boolean opened(TAmssDutyTask r, Date at) {
        return r.getDueAt() != null && dayKey(openAt(r)).compareTo(dayKey(at)) <= 0;
    }

    /**
     * 上门时辰：常规期上门维修技术服务只认九至十一、十五至十八两段，段外按住等下轮；
     * 三夏三秋重点时期照值守班次走，不受两段约束；检修复查、服务点值守不按上门时段卡。
     */
    private boolean inDispatchWindow(TAmssDutyTask r, Date at) {
        if (nz(r.getBusySeason()) == 1) {
            return true;
        }
        if (r.getItemType() == null || r.getItemType().intValue() != TYPE_ONSITE) {
            return true;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(at);
        int h = cal.get(Calendar.HOUR_OF_DAY);
        return (h >= WIN1_FROM && h < WIN1_TO) || (h >= WIN2_FROM && h < WIN2_TO);
    }

    private Map<String, TAmssDutyTeam> loadTeams() {
        Map<String, TAmssDutyTeam> map = new HashMap<>();
        for (TAmssDutyTeam t : this.amssDutyTeamMapper.selectList(new QueryWrapper<TAmssDutyTeam>().eq("del_flag", 0))) {
            map.put(t.getTeamNo(), t);
        }
        return map;
    }

    private Map<String, TAmssMachCard> loadCards() {
        Map<String, TAmssMachCard> map = new HashMap<>();
        for (TAmssMachCard c : this.amssMachCardMapper.selectList(new QueryWrapper<TAmssMachCard>().eq("del_flag", 0))) {
            map.put(c.getCardNo(), c);
        }
        return map;
    }

    private boolean teamHalted(String teamNo, Map<String, TAmssDutyTeam> teams) {
        if (teamNo == null || teamNo.trim().isEmpty()) {
            return false;
        }
        TAmssDutyTeam t = teams.get(teamNo);
        return t != null && nz(t.getStatus()) == TEAM_HALT;
    }

    private boolean cardRevoked(String cardNo, Map<String, TAmssMachCard> cards) {
        if (cardNo == null || cardNo.trim().isEmpty()) {
            return false;
        }
        TAmssMachCard c = cards.get(cardNo);
        return c != null && nz(c.getStatus()) == CARD_REVOKED;
    }

    /** 同事项同班组同动作当日是否已落过头一条（第二遍不留，先落那条也不动）。 */
    private boolean sameDayLogExists(String itemNo, String teamNo, Date at, int action) {
        return this.amssDutyLogMapper.selectCount(new QueryWrapper<TAmssDutyLog>()
                .eq("item_no", itemNo)
                .eq("team_no", teamNo)
                .eq("action", action)
                .eq("action_day", sqlDay(at))).intValue() > 0;
    }

    private Set<String> teamsEverAssigned(String itemNo) {
        Set<String> set = new HashSet<>();
        for (TAmssDutyLog l : this.amssDutyLogMapper.selectList(new QueryWrapper<TAmssDutyLog>()
                .eq("item_no", itemNo).eq("action", TAmssDutyLog.ACTION_ASSIGN))) {
            set.add(l.getTeamNo());
        }
        return set;
    }

    /**
     * 挑一个当班且没接过这件事的班组：优先所属班组，再按字号顺。
     * 一轮之内同一件事只过一遍，绝不再派给已经接过它的班组（收回必派别家）。
     */
    private String pickTeam(TAmssDutyTask r, Map<String, TAmssDutyTeam> teams) {
        List<TAmssDutyTeam> active = new ArrayList<>();
        for (TAmssDutyTeam t : teams.values()) {
            if (nz(t.getStatus()) != TEAM_HALT) {
                active.add(t);
            }
        }
        active.sort((a, b) -> a.getTeamNo().compareTo(b.getTeamNo()));
        Set<String> history = teamsEverAssigned(r.getItemNo());
        String preferred = r.getTeamNo();
        for (TAmssDutyTeam t : active) {
            if (t.getTeamNo().equals(preferred) && !history.contains(t.getTeamNo())) {
                return t.getTeamNo();
            }
        }
        for (TAmssDutyTeam t : active) {
            if (!history.contains(t.getTeamNo())) {
                return t.getTeamNo();
            }
        }
        return null;
    }

    /**
     * 派出：先落流水再改条目（半截失败时流水仍在，下轮不会又派回同一个班组）；
     * 头一回的班组/时刻只写一次，后面任何重派不覆盖。
     */
    private boolean dispatch(TAmssDutyTask r, String teamNo, Date at) {
        // 同一事项对同一班组当日送两遍：库里只留前一遍
        if (sameDayLogExists(r.getItemNo(), teamNo, at, TAmssDutyLog.ACTION_ASSIGN)) {
            return false;
        }
        TAmssDutyLog l = new TAmssDutyLog();
        l.setItemNo(r.getItemNo());
        l.setTeamNo(teamNo);
        l.setAction(TAmssDutyLog.ACTION_ASSIGN);
        l.setActionTime(at);
        l.setActionDay(sqlDay(at));
        this.amssDutyLogMapper.insert(l);

        r.setStatus(STATUS_DISPATCHED);
        r.setAssignedTeamNo(teamNo);
        r.setAssignTime(at);
        if (r.getFirstTeamNo() == null) {
            r.setFirstTeamNo(teamNo);
            r.setFirstAssignTime(at);
        }
        this.amssDutyTaskMapper.updateById(r);
        return true;
    }

    /** 收回：先留收回痕迹再改条目，头一回派给谁/几点派出（first*）不动。 */
    private void recall(TAmssDutyTask r, Date at) {
        if (!sameDayLogExists(r.getItemNo(), r.getAssignedTeamNo(), at, TAmssDutyLog.ACTION_RECALL)) {
            TAmssDutyLog l = new TAmssDutyLog();
            l.setItemNo(r.getItemNo());
            l.setTeamNo(r.getAssignedTeamNo());
            l.setAction(TAmssDutyLog.ACTION_RECALL);
            l.setActionTime(at);
            l.setActionDay(sqlDay(at));
            l.setReason("派出超过当日未回话，收回重派别家");
            this.amssDutyLogMapper.insert(l);
        }
        r.setStatus(STATUS_WAIT);
        r.setAssignedTeamNo(null);
        r.setAssignTime(null);
        this.amssDutyTaskMapper.updateById(r);
    }

    /** 挂起：当场写清缘故；排在后面的条目照旧挨个轮到，不叫一件卡住一整轮。 */
    private boolean holdIfUnsendable(TAmssDutyTask r, Map<String, TAmssDutyTeam> teams,
                                     Map<String, TAmssMachCard> cards, DutyRoundResult res) {
        String reason = null;
        if (teamHalted(r.getTeamNo(), teams)) {
            reason = "所属班组[" + r.getTeamNo() + "]本季整班停摆，派不出去";
        } else if (cardRevoked(r.getMachCardNo(), cards)) {
            reason = "机具牌证[" + r.getMachCardNo() + "]年初已收回，派不出去";
        }
        if (reason == null) {
            return false;
        }
        r.setStatus(STATUS_HELD);
        r.setHoldReason(reason);
        this.amssDutyTaskMapper.updateById(r);
        res.getHeldItems().add(r.getItemNo() + "：" + reason);
        return true;
    }

    @Override
    public List<TAmssDutyTask> listDue(Date at) {
        List<TAmssDutyTask> due = new ArrayList<>();
        Map<String, TAmssDutyTeam> teams = loadTeams();
        Map<String, TAmssMachCard> cards = loadCards();
        for (TAmssDutyTask r : this.amssDutyTaskMapper.selectList(
                new QueryWrapper<TAmssDutyTask>().eq("del_flag", 0).orderByAsc("id"))) {
            if (nz(r.getStatus()) != STATUS_WAIT || !opened(r, at) || !inDispatchWindow(r, at)) {
                continue;
            }
            // 已能判定派不出去的，不算可派
            if (teamHalted(r.getTeamNo(), teams) || cardRevoked(r.getMachCardNo(), cards)) {
                continue;
            }
            due.add(r);
        }
        return due;
    }

    // 不加整轮事务：各条写库各自提交，某一条半截失败只跳它自己，不连累这一轮（流水先落，痕迹不丢）。
    @Override
    public int runOnce(Date at) {
        return runRound(at).getAssignedCount();
    }

    @Override
    public DutyRoundResult runOnce() {
        return runRound(new Date());
    }

    /**
     * 一轮派单的唯一算账处：翻出哪些条目、收回几条、挂起几条、派出几条、按住几条，
     * 全部在这里算清写回；单条失败跳过继续，不连累这一轮其他条目。
     * 包内可见，便于以指定时刻做口径单测；对外只暴露 {@link #runOnce(Date)} 与 {@link #runOnce()}。
     */
    DutyRoundResult runRound(Date at) {
        DutyRoundResult res = new DutyRoundResult();
        res.setAt(at);

        Map<String, TAmssDutyTeam> teams = loadTeams();
        Map<String, TAmssMachCard> cards = loadCards();

        // 本轮范围：①开口已到的待派条目；②昨日及以前派出、当日没回话该收回的已派条目
        List<TAmssDutyTask> scope = new ArrayList<>();
        for (TAmssDutyTask r : this.amssDutyTaskMapper.selectList(
                new QueryWrapper<TAmssDutyTask>().eq("del_flag", 0).orderByAsc("id"))) {
            int st = nz(r.getStatus());
            boolean recallDue = st == STATUS_DISPATCHED && r.getAssignTime() != null
                    && dayKey(r.getAssignTime()).compareTo(dayKey(at)) < 0;
            boolean openDue = st == STATUS_WAIT && opened(r, at);
            if (recallDue || openDue) {
                scope.add(r);
            }
        }

        if (scope.isEmpty()) {
            res.setIdle(true);
            res.setMessage("本轮无事");
            return res;
        }
        for (TAmssDutyTask r : scope) {
            res.getScopeItemNos().add(r.getItemNo());
        }

        for (TAmssDutyTask r : scope) {
            try {
                // 第一步：领单超过当日没回话的，先收回留痕，回到待派
                if (nz(r.getStatus()) == STATUS_DISPATCHED && r.getAssignTime() != null
                        && dayKey(r.getAssignTime()).compareTo(dayKey(at)) < 0) {
                    String fromTeam = r.getAssignedTeamNo();
                    recall(r, at);
                    res.setRecalledCount(res.getRecalledCount() + 1);
                    res.getRecalledItems().add(r.getItemNo() + "：收回自[" + fromTeam + "]");
                }

                // 第二步：待派的——派不出去先挂起，时辰不对按住，时辰对了派出去
                if (nz(r.getStatus()) == STATUS_WAIT) {
                    if (holdIfUnsendable(r, teams, cards, res)) {
                        continue;
                    }
                    if (!inDispatchWindow(r, at)) {
                        res.getWaitingItems().add(r.getItemNo() + "：日子已到但落在上门时段之外，按住等下轮");
                        continue;
                    }
                    String teamNo = pickTeam(r, teams);
                    if (teamNo == null) {
                        res.getWaitingItems().add(r.getItemNo() + "：当班班组都已接过此事项，按住等下轮");
                        continue;
                    }
                    if (dispatch(r, teamNo, at)) {
                        res.setAssignedCount(res.getAssignedCount() + 1);
                        res.getAssignedItems().add(r.getItemNo() + "：派给[" + teamNo + "]");
                    }
                }
            } catch (Exception e) {
                // 单条失败跳过继续，不叫一件卡住一整轮
                log.error("派单条目[{}]本轮处理失败，跳过：{}", r.getItemNo(), e.getMessage(), e);
            }
        }

        // 跑完回一屏：摆的都是本轮翻出条目的末态账
        int endWait = 0;
        int endDispatched = 0;
        int endHeld = 0;
        for (TAmssDutyTask r : scope) {
            int st = nz(r.getStatus());
            if (st == STATUS_HELD) {
                endHeld++;
            } else if (st == STATUS_DISPATCHED) {
                endDispatched++;
            } else {
                endWait++;
            }
        }
        res.setHeldCount(endHeld);
        res.setEndHeldCount(endHeld);
        res.setEndDispatchedCount(endDispatched);
        res.setWaitingCount(endWait);
        res.setEndWaitingCount(endWait);
        res.setMessage(String.format(
                "本轮到期%d条：当日待派%d条、已派%d条、挂起%d条；本轮新派出%d条、收回%d条。",
                scope.size(), endWait, endDispatched, endHeld, res.getAssignedCount(), res.getRecalledCount()));
        return res;
    }
}
