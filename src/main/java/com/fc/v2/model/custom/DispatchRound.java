package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 一轮派单回屏的账：当日待派、已派、挂起各几条，摆的都是本轮算清的数，
 * 与业务服务层执行方法的返回逐数一致。整轮无到期条目即"本轮无事"，不是故障。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class DispatchRound implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /** 本轮执行时刻 */
    private java.util.Date at;
    /** 本轮翻出（到期开口）的条目数 */
    private int dueCount;
    /** 本轮收口时当日待派条数 */
    private int waitCount;
    /** 本轮收口时已派条数 */
    private int dispatchedCount;
    /** 本轮收口时挂起条数 */
    private int hungCount;
    /** 本轮新派出条数 */
    private int sentCount;
    /** 本轮按住（日子到了时辰不到）条数 */
    private int heldCount;
    /** 本轮收回重派条数 */
    private int recalledCount;
    /** 是否本轮无事（无到期条目） */
    private boolean idle;
    /** 给屏幕的一句话 */
    private String message;
    /** 逐条处置账 */
    private List<DispatchRoundItem> items = new ArrayList<DispatchRoundItem>();

    public java.util.Date getAt() {
        return at;
    }

    public void setAt(java.util.Date at) {
        this.at = at;
    }

    public int getDueCount() {
        return dueCount;
    }

    public void setDueCount(int dueCount) {
        this.dueCount = dueCount;
    }

    public int getWaitCount() {
        return waitCount;
    }

    public void setWaitCount(int waitCount) {
        this.waitCount = waitCount;
    }

    public int getDispatchedCount() {
        return dispatchedCount;
    }

    public void setDispatchedCount(int dispatchedCount) {
        this.dispatchedCount = dispatchedCount;
    }

    public int getHungCount() {
        return hungCount;
    }

    public void setHungCount(int hungCount) {
        this.hungCount = hungCount;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    public int getHeldCount() {
        return heldCount;
    }

    public void setHeldCount(int heldCount) {
        this.heldCount = heldCount;
    }

    public int getRecalledCount() {
        return recalledCount;
    }

    public void setRecalledCount(int recalledCount) {
        this.recalledCount = recalledCount;
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

    public List<DispatchRoundItem> getItems() {
        return items;
    }

    public void setItems(List<DispatchRoundItem> items) {
        this.items = items;
    }
}
