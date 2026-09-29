package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 一轮派单的回屏账：这一轮翻出哪些条目、派出几条、收回几条、挂起几条、按住几条，
 * 一律由业务服务层的执行方法算清再返回，屏上的数必须与它一致。
 * 整轮无到期条目时 idle=true，message="本轮无事"，不当故障。
 *
 * @author fuce
 * @date 2026-09-15
 */
public class DutyRoundResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本轮起算时刻 */
    private Date at;

    /** 是否无事轮（本轮没有任何到期条目） */
    private boolean idle;

    /** 回屏一句话 */
    private String message;

    /** 本轮翻出的到期事项号（待派开口的 + 到点该收回的） */
    private List<String> scopeItemNos = new ArrayList<>();

    /** 本轮新派出条数（含收回后重派） */
    private int assignedCount;

    /** 本轮收回条数 */
    private int recalledCount;

    /** 本轮新挂起条数 */
    private int heldCount;

    /** 本轮翻出但仍待派条数（日子到了却落在两个上门时段之外，先按住等下轮） */
    private int waitingCount;

    /** 本轮派出明细 */
    private List<String> assignedItems = new ArrayList<>();

    /** 本轮收回明细 */
    private List<String> recalledItems = new ArrayList<>();

    /** 本轮挂起明细（事项号 + 缘故） */
    private List<String> heldItems = new ArrayList<>();

    /** 本轮按住等下轮的明细 */
    private List<String> waitingItems = new ArrayList<>();

    /** 跑完一轮后，本轮翻出条目里末态仍是待派的条数 */
    private int endWaitingCount;

    /** 跑完一轮后，本轮翻出条目里末态为已派的条数（含收回后重派） */
    private int endDispatchedCount;

    /** 跑完一轮后，本轮翻出条目里末态为挂起的条数 */
    private int endHeldCount;

    public int getEndWaitingCount() {
        return endWaitingCount;
    }

    public void setEndWaitingCount(int endWaitingCount) {
        this.endWaitingCount = endWaitingCount;
    }

    public int getEndDispatchedCount() {
        return endDispatchedCount;
    }

    public void setEndDispatchedCount(int endDispatchedCount) {
        this.endDispatchedCount = endDispatchedCount;
    }

    public int getEndHeldCount() {
        return endHeldCount;
    }

    public void setEndHeldCount(int endHeldCount) {
        this.endHeldCount = endHeldCount;
    }

    public Date getAt() {
        return at;
    }

    public void setAt(Date at) {
        this.at = at;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getScopeItemNos() {
        return scopeItemNos;
    }

    public void setScopeItemNos(List<String> scopeItemNos) {
        this.scopeItemNos = scopeItemNos;
    }

    public int getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(int assignedCount) {
        this.assignedCount = assignedCount;
    }

    public int getRecalledCount() {
        return recalledCount;
    }

    public void setRecalledCount(int recalledCount) {
        this.recalledCount = recalledCount;
    }

    public int getHeldCount() {
        return heldCount;
    }

    public void setHeldCount(int heldCount) {
        this.heldCount = heldCount;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    public List<String> getAssignedItems() {
        return assignedItems;
    }

    public void setAssignedItems(List<String> assignedItems) {
        this.assignedItems = assignedItems;
    }

    public List<String> getRecalledItems() {
        return recalledItems;
    }

    public void setRecalledItems(List<String> recalledItems) {
        this.recalledItems = recalledItems;
    }

    public List<String> getHeldItems() {
        return heldItems;
    }

    public void setHeldItems(List<String> heldItems) {
        this.heldItems = heldItems;
    }

    public List<String> getWaitingItems() {
        return waitingItems;
    }

    public void setWaitingItems(List<String> waitingItems) {
        this.waitingItems = waitingItems;
    }
}
