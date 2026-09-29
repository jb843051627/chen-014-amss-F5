package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TAmssDutyTaskMapper;
import com.fc.v2.mapper.auto.TAmssDutyTeamMapper;
import com.fc.v2.mapper.auto.TAmssOutCardMapper;
import com.fc.v2.model.auto.TAmssDutyTask;
import com.fc.v2.model.auto.TAmssDutyTeam;
import com.fc.v2.model.auto.TAmssOutCard;
import com.fc.v2.model.custom.DispatchRound;
import com.fc.v2.model.custom.DispatchRoundItem;
import com.fc.v2.service.ITAmssDutyTaskService;

/**
 * 三夏三秋检修复查与上门服务派单条目 Service业务层处理（scheduling-job 形状：周期执行）。
 *
 * 全站一个口径：
 * 1) 倒计时——从 due_at（到场当天末尾一刻）按自然日往前倒，剩余日数落在 [0, amount] 才开口；
 *    挑单与回写都用这一种数法，要挪口径只准改 amount，不准把 due_at 往后推。
 * 2) 时辰——普通时期上门只认 09:00-11:00、15:00-18:00 两段，日子到了时辰不到先按住等下轮；
 *    三夏三秋重点时期照值守班次走，不受两段约束。
 * 3) 挂起——所属班组本季整班停摆、或农机牌证年初已收回，当场写清挂起缘故；挂起不挡后面的条目。
 * 4) 不重派——已派不回待派；同一事项另有一条已派在办的，第二句整句不留（不许借另一个班的手造成一件两班），
 *    先落那条不动；改口径另起新事项，旧条不改。
 * 5) 收回——领单过了当日没回话，收回重派别家并留痕，头一回派给谁、几点派出不可覆盖。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TAmssDutyTaskServiceImpl implements ITAmssDutyTaskService {

    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DONE = 1;
    private static final int STATUS_HUNG = 2;

    private static final int MODE_NORMAL = 0;
    private static final int MODE_PEAK = 1;

    /** 普通时期上门两段：[09:00,11:00)、[15:00,18:00)，单位：分钟 */
    private static final int WIN1_FROM = 9 * 60;
    private static final int WIN1_TO = 11 * 60;
    private static final int WIN2_FROM = 15 * 60;
    private static final int WIN2_TO = 18 * 60;

    /** 收回留痕里历次受派班组的解析式（头一回另有 first_team_no 列，不在这里） */
    private static final Pattern PREV_TEAM_PATTERN = Pattern.compile("原派班组 ([0-9A-Za-z_\\-]+)");

    @javax.annotation.Resource
    private TAmssDutyTaskMapper amssDutyTaskMapper;
    @javax.annotation.Resource
    private TAmssDutyTeamMapper amssDutyTeamMapper;
    @javax.annotation.Resource
    private TAmssOutCardMapper amssOutCardMapper;

    @Override
    public TAmssDutyTask selectTAmssDutyTaskById(Long id) {
        return this.amssDutyTaskMapper.selectById(id);
    }

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）：JDBC 的 serverTimezone 与本机
     * 时区不对称，直接把 java.util.Date 作参数会整体偏移，使"恰好到期"这类边界用例错判。
     */
    private static String ts(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    /** 当天 00:00:00（本机时区，与库内墙钟口径一致） */
    private static Date dayFloor(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 全站唯一的倒计时数法：从 due_at 所在当天末尾往前倒到 at 所在当天，相差的完整自然日数。
     * 0 即压到当天；挑单、回写都走这一处，不许另立口径。
     */
    private static long daysLeft(Date dueAt, Date at) {
        long diff = dayFloor(dueAt).getTime() - dayFloor(at).getTime();
        return diff / (24L * 3600L * 1000L);
    }

    private static int minuteOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    private static int hhmm(String s) {
        if (s == null || s.length() < 5) {
            return -1;
        }
        String[] p = s.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    /** 普通时期时辰：只认晌午前、傍晚前两段 */
    private static boolean inNormalWindow(Date at) {
        int m = minuteOfDay(at);
        return (m >= WIN1_FROM && m < WIN1_TO) || (m >= WIN2_FROM && m < WIN2_TO);
    }

    /** 重点时期时辰：此刻落在该班组值守班次内（不跨夜，跨夜班次另行排） */
    private static boolean onShift(TAmssDutyTeam t, Date at) {
        int from = hhmm(t.getShiftStart());
        int to = hhmm(t.getShiftEnd());
        if (from < 0 || to < 0) {
            return false;
        }
        int m = minuteOfDay(at);
        return m >= from && m < to;
    }

    /** 倒计时开口：剩余日数 ≤ amount（含压到当天的 0 与已过期的负数，过期未派更要开口）；amount 缺省按 0 */
    private static boolean open(TAmssDutyTask r, Date at) {
        if (r.getDueAt() == null) {
            return false;
        }
        long left = daysLeft(r.getDueAt(), at);
        int lead = r.getAmount() == null ? 0 : r.getAmount().setScale(0, BigDecimal.ROUND_DOWN).intValue();
        return left <= lead;
    }

    @Override
    public List<TAmssDutyTask> listDue(Date at) {
        List<TAmssDutyTask> due = new ArrayList<TAmssDutyTask>();
        if (at == null) {
            return due;
        }
        List<TAmssDutyTask> all = this.amssDutyTaskMapper.selectList(
                new QueryWrapper<TAmssDutyTask>().eq("del_flag", 0));
        for (TAmssDutyTask r : all) {
            // 已派的不回待派，下轮不再翻出来；待派与挂起（仍翻得着、可重新核对）按倒计时开口翻出
            Integer st = r.getStatus();
            if (st != null && st == STATUS_DONE) {
                continue;
            }
            if (open(r, at)) {
                due.add(r);
            }
        }
        return due;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int runOnce(Date at) {
        return runOnce(at, true).getSentCount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DispatchRound runOnce(Date at, boolean verbose) {
        DispatchRound round = new DispatchRound();
        round.setAt(at);
        if (at == null) {
            round.setIdle(true);
            round.setMessage("本轮无事");
            return round;
        }

        List<TAmssDutyTask> due = listDue(at);
        // 另翻已派条目：领单过了当日没回话的要收回（已派不回待派，唯有这一条带痕迹的路）
        List<TAmssDutyTask> all = this.amssDutyTaskMapper.selectList(
                new QueryWrapper<TAmssDutyTask>().eq("del_flag", 0));
        List<TAmssDutyTask> recoverable = new ArrayList<TAmssDutyTask>();
        for (TAmssDutyTask r : all) {
            if (r.getStatus() != null && r.getStatus() == STATUS_DONE && needRecover(r, at)) {
                recoverable.add(r);
            }
        }

        round.setDueCount(due.size() + recoverable.size());
        // 整轮无到期条目（含无超期未回话）即无事，不是故障
        if (due.isEmpty() && recoverable.isEmpty()) {
            round.setIdle(true);
            round.setMessage("本轮无事");
            return round;
        }
        round.setIdle(false);

        List<TAmssDutyTeam> teams = this.amssDutyTeamMapper.selectList(
                new QueryWrapper<TAmssDutyTeam>().eq("del_flag", 0).orderByAsc("team_no"));

        // 事项级在办账：已有一条"已派在办"的事项（不论派给哪个班、不论隔不隔日），
        // 第二句一律按住——一轮之内同一条不许两个班组接，也不许借隔日重派再造一件两班。
        java.util.Map<String, Long> activeItemRow = new java.util.HashMap<String, Long>();
        for (TAmssDutyTask q : all) {
            if (q.getStatus() != null && q.getStatus() == STATUS_DONE && q.getItemNo() != null) {
                activeItemRow.put(q.getItemNo(), q.getId());
            }
        }
        Set<String> sentItemNosThisRound = new HashSet<String>();

        int sent = 0;
        int held = 0;
        int recalled = 0;

        // 1) 先收回超期未回话的，再当场重派别家
        for (TAmssDutyTask r : recoverable) {
            String prevTeam = r.getTeamNo();
            String prevName = r.getTeamName();
            String prevDate = r.getDispatchedDate() == null ? "" : r.getDispatchedDate().toString();
            int cnt = (r.getReassignCount() == null ? 0 : r.getReassignCount()) + 1;
            String trace = "[" + ts(at) + " 收回：原派班组 " + prevTeam
                    + "（" + prevName + "）派于 " + prevDate + " 超过当日未回话，第" + cnt + "次收回]";
            r.setRecoverRemark(r.getRecoverRemark() == null ? trace : r.getRecoverRemark() + trace);
            r.setReassignCount(cnt);
            r.setTeamNo(null);
            r.setTeamName(null);
            r.setDispatchedDate(null);
            r.setStatus(STATUS_WAIT);
            this.amssDutyTaskMapper.updateById(r);
            recalled++;
            detail(round, verbose, new DispatchRoundItem("recalled", r.getItemNo(),
                    prevTeam, prevName, r.getFirstSendAt(), trace));

            // 收回后同样先看挂起、再看时辰，口径与新到期条目一致
            String hangReason = hangReason(r, teams);
            if (hangReason != null) {
                r.setStatus(STATUS_HUNG);
                r.setHangRemark(hangReason);
                this.amssDutyTaskMapper.updateById(r);
                detail(round, verbose, new DispatchRoundItem("hung", r.getItemNo(),
                        null, null, null, hangReason));
                continue;
            }
            if (!timeOk(r, at, teams)) {
                detail(round, verbose, new DispatchRoundItem("held", r.getItemNo(),
                        null, null, null, "已收回，时辰不在上门两段/值守班次内，先按住等下轮"));
                held++;
                continue;
            }
            // 同一事项已有先落的在办派单（不论班组、不论隔日）：第二句整句不留，先落那条不动
            if (itemAlreadyActive(r, activeItemRow, sentItemNosThisRound)) {
                detail(round, verbose, new DispatchRoundItem("held", r.getItemNo(),
                        null, null, null, "同一事项已派在办，第二句不留，先落那条不动"));
                held++;
                continue;
            }
            TAmssDutyTeam picked = pickTeam(r, teams, at);
            if (picked != null) {
                assign(r, picked, at);
                sentItemNosThisRound.add(r.getItemNo());
                sent++;
                detail(round, verbose, new DispatchRoundItem("recovered", r.getItemNo(),
                        picked.getTeamNo(), picked.getTeamName(), at, trace));
            } else {
                detail(round, verbose, new DispatchRoundItem("held", r.getItemNo(),
                        null, null, null, "已收回，本轮无别家班组可接，等下轮"));
                held++;
            }
        }

        // 2) 到期条目挨个轮到：挂起判定 → 时辰判定 → 派班；一件卡住不挡后面
        for (TAmssDutyTask r : due) {
            String hangReason = hangReason(r, teams);
            if (hangReason != null) {
                r.setStatus(STATUS_HUNG);
                r.setHangRemark(hangReason);
                this.amssDutyTaskMapper.updateById(r);
                detail(round, verbose, new DispatchRoundItem("hung", r.getItemNo(),
                        null, null, null, hangReason));
                continue;
            }
            if (r.getStatus() != null && r.getStatus() == STATUS_HUNG) {
                // 上轮挂起、这轮缘故消了：落回待派继续走
                r.setStatus(STATUS_WAIT);
                r.setHangRemark(null);
            }
            if (!timeOk(r, at, teams)) {
                held++;
                detail(round, verbose, new DispatchRoundItem("held", r.getItemNo(),
                        null, null, null, "日子到了，时辰不在上门两段/值守班次内，先按住等下轮"));
                continue;
            }
            // 同一事项已有先落的在办派单（不论班组、不论隔日）：第二句整句不留，先落那条不动
            if (itemAlreadyActive(r, activeItemRow, sentItemNosThisRound)) {
                held++;
                detail(round, verbose, new DispatchRoundItem("held", r.getItemNo(),
                        null, null, null, "同一事项已派在办，第二句不留，先落那条不动"));
                continue;
            }
            TAmssDutyTeam picked = pickTeam(r, teams, at);
            if (picked == null) {
                held++;
                detail(round, verbose, new DispatchRoundItem("held", r.getItemNo(),
                        null, null, null, "本轮无可用班组，先按住等下轮"));
                continue;
            }
            assign(r, picked, at);
            sentItemNosThisRound.add(r.getItemNo());
            sent++;
            detail(round, verbose, new DispatchRoundItem("dispatched", r.getItemNo(),
                    picked.getTeamNo(), picked.getTeamName(), at, null));
        }

        // 3) 收口：摆的都是本轮的账——到期翻出的 + 收回的这一批，按落库后的情形再点一遍
        Set<Long> scopeIds = new HashSet<Long>();
        for (TAmssDutyTask r : due) {
            scopeIds.add(r.getId());
        }
        for (TAmssDutyTask r : recoverable) {
            scopeIds.add(r.getId());
        }
        int wait = 0;
        int done = 0;
        int hungCnt = 0;
        for (TAmssDutyTask r : all) {
            if (!scopeIds.contains(r.getId())) {
                continue;
            }
            TAmssDutyTask fresh = this.amssDutyTaskMapper.selectById(r.getId());
            int st = fresh.getStatus() == null ? STATUS_WAIT : fresh.getStatus();
            if (st == STATUS_DONE) {
                done++;
            } else if (st == STATUS_HUNG) {
                hungCnt++;
            } else {
                wait++;
            }
        }
        round.setWaitCount(wait);
        round.setDispatchedCount(done);
        round.setHungCount(hungCnt);
        round.setSentCount(sent);
        round.setHeldCount(held);
        round.setRecalledCount(recalled);
        round.setMessage("本轮到期" + round.getDueCount() + "条：新派" + sent + "条，按住" + held
                + "条，收回" + recalled + "条；收盘待派" + wait + "、已派" + done + "、挂起" + hungCnt);
        return round;
    }

    /** 逐条处置账只在 verbose 轮次回填；计数与一句话无论何种调用都填 */
    private static void detail(DispatchRound round, boolean verbose, DispatchRoundItem item) {
        if (verbose) {
            round.getItems().add(item);
        }
    }

    /** 领单超过当日没回话：已派、未回话、派成日早于今天 */
    private static boolean needRecover(TAmssDutyTask r, Date at) {
        if (r.getRespondedAt() != null || r.getDispatchedDate() == null) {
            return false;
        }
        return dayFloor(r.getDispatchedDate()).before(dayFloor(at));
    }

    /** 挂起缘故：所属班组本季整班停摆，或农机牌证年初已收回；无缘故返回 null */
    private String hangReason(TAmssDutyTask r, List<TAmssDutyTeam> teams) {
        if (r.getOwnTeamNo() != null && !r.getOwnTeamNo().trim().isEmpty()) {
            for (TAmssDutyTeam t : teams) {
                if (r.getOwnTeamNo().equals(t.getTeamNo())
                        && t.getStatus() != null && t.getStatus() == 1) {
                    return "所属班组 " + r.getOwnTeamNo() + "（" + t.getTeamName() + "）本季整班停摆，转挂起";
                }
            }
        }
        if (r.getMachineCardNo() != null && !r.getMachineCardNo().trim().isEmpty()) {
            TAmssOutCard card = this.amssOutCardMapper.selectOne(new QueryWrapper<TAmssOutCard>()
                    .eq("del_flag", 0).eq("biz_no", r.getMachineCardNo()));
            if (card != null && card.getStatus() != null && card.getStatus() == 2) {
                return "农机牌证 " + r.getMachineCardNo() + " 年初已收回，转挂起";
            }
        }
        return null;
    }

    /** 时辰判定：重点时期照值守班次（此刻有班次即开口），普通时期只认两段 */
    private boolean timeOk(TAmssDutyTask r, Date at, List<TAmssDutyTeam> teams) {
        if (r.getTimeMode() != null && r.getTimeMode() == MODE_PEAK) {
            for (TAmssDutyTeam t : teams) {
                if (t.getStatus() != null && t.getStatus() == 1) {
                    continue;
                }
                if (t.getSeason() != null && !t.getSeason().trim().isEmpty() && onShift(t, at)) {
                    return true;
                }
            }
            return false;
        }
        return inNormalWindow(at);
    }

    /**
     * 同一事项是否已有先落的在办派单：
     * 库里另有一条已派行（排除本条自身），或本轮刚派出过同一事项，都算第二句——整句不留。
     */
    private static boolean itemAlreadyActive(TAmssDutyTask r, java.util.Map<String, Long> activeItemRow,
                                             Set<String> sentItemNosThisRound) {
        if (r.getItemNo() == null) {
            return false;
        }
        if (sentItemNosThisRound.contains(r.getItemNo())) {
            return true;
        }
        Long activeId = activeItemRow.get(r.getItemNo());
        return activeId != null && !activeId.equals(r.getId());
    }

    /**
     * 挑班（全部候选一次排定，确定序）：
     * 停摆的不要；重点时期只要值守班且此刻当班，普通时期只要普通班组；
     * 收回重派的，历次受派班组（含头一回）一律不再派——重派只派别家。
     * 同一事项"一件两班"的去重在调用方按事项级统一处理（见 {@link #itemAlreadyActive}），
     * 班组一轮可接多件，不靠"一班一轮一件"限产。
     */
    private TAmssDutyTeam pickTeam(TAmssDutyTask r, List<TAmssDutyTeam> teams, Date at) {
        Set<String> prev = previousTeams(r);
        boolean peak = r.getTimeMode() != null && r.getTimeMode() == MODE_PEAK;
        for (TAmssDutyTeam t : teams) {
            if (t.getStatus() != null && t.getStatus() == 1) {
                continue;
            }
            boolean isPeakTeam = t.getSeason() != null && !t.getSeason().trim().isEmpty();
            if (peak) {
                if (!isPeakTeam || !onShift(t, at)) {
                    continue;
                }
            } else if (isPeakTeam) {
                continue;
            }
            if (prev.contains(t.getTeamNo())) {
                continue;
            }
            return t;
        }
        return null;
    }

    /** 头一回 + 收回留痕里出现过的所有受派班组 */
    private static Set<String> previousTeams(TAmssDutyTask r) {
        Set<String> prev = new HashSet<String>();
        if (r.getFirstTeamNo() != null) {
            prev.add(r.getFirstTeamNo());
        }
        if (r.getRecoverRemark() != null) {
            Matcher m = PREV_TEAM_PATTERN.matcher(r.getRecoverRemark());
            while (m.find()) {
                prev.add(m.group(1));
            }
        }
        return prev;
    }

    /** 落派：写本次受派班组与派成日；头一回的班组/时刻只在首派时写，收回再派不覆盖 */
    private void assign(TAmssDutyTask r, TAmssDutyTeam t, Date at) {
        r.setTeamNo(t.getTeamNo());
        r.setTeamName(t.getTeamName());
        r.setDispatchedDate(new java.sql.Date(dayFloor(at).getTime()));
        r.setStatus(STATUS_DONE);
        if (r.getFirstTeamNo() == null) {
            r.setFirstTeamNo(t.getTeamNo());
            r.setFirstTeamName(t.getTeamName());
            r.setFirstSendAt(at);
        }
        this.amssDutyTaskMapper.updateById(r);
    }
}
