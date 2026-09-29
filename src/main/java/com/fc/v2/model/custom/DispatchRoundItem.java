package com.fc.v2.model.custom;

import java.util.Date;

/**
 * 一轮派单中单条事项的处置账：屏幕上一行一件，派给谁、什么时刻派、为什么按住/挂起/收回都在此。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class DispatchRoundItem implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /** 处置动作 dispatched派出 held按住等下轮 hung挂起 recalled收回 recovered收回后重派 skipped已处理不重派 */
    private String action;
    /** 事项号 */
    private String itemNo;
    /** 受派班组编号 */
    private String teamNo;
    /** 受派班组名称 */
    private String teamName;
    /** 派出时刻 */
    private Date sendAt;
    /** 说法（挂起缘故/按住缘故/收回缘故） */
    private String reason;

    public DispatchRoundItem() {
    }

    public DispatchRoundItem(String action, String itemNo, String teamNo, String teamName, Date sendAt, String reason) {
        this.action = action;
        this.itemNo = itemNo;
        this.teamNo = teamNo;
        this.teamName = teamName;
        this.sendAt = sendAt;
        this.reason = reason;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public String getTeamNo() {
        return teamNo;
    }

    public void setTeamNo(String teamNo) {
        this.teamNo = teamNo;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Date getSendAt() {
        return sendAt;
    }

    public void setSendAt(Date sendAt) {
        this.sendAt = sendAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
