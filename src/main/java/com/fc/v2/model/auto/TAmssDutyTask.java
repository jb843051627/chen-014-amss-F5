package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 三夏三秋检修复查与上门服务派单条目对象 t_amss_duty_task
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_amss_duty_task")
@ApiModel(value = "TAmssDutyTask", description = "三夏三秋检修复查与上门服务派单条目")
public class TAmssDutyTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 检修复查与上门服务派单事项号 */
    @TableField("item_no")
    @ApiModelProperty(value = "检修复查与上门服务派单事项号")
    private String itemNo;

    /** 本项应当到场那天的最后一刻 */
    @TableField("due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本项应当到场那天的最后一刻")
    private Date dueAt;

    /** 单条提前派单的自然日数 */
    @TableField("amount")
    @ApiModelProperty(value = "单条提前派单的自然日数")
    private BigDecimal amount;

    /** 派单事由与受派班组记要 */
    @TableField("content")
    @ApiModelProperty(value = "派单事由与受派班组记要")
    private String content;

    /** 条目情形 0待派 1已派 2挂起(派不出去) */
    @TableField("status")
    @ApiModelProperty(value = "条目情形 0待派 1已派 2挂起(派不出去)")
    private Integer status;

    /** 时辰口径 0普通(九至十一/十五至十八两段) 1重点时期(照值守班次) */
    @TableField("time_mode")
    @ApiModelProperty(value = "时辰口径 0普通 1重点时期")
    private Integer timeMode;

    /** 本条所属班组编号(停摆即挂起) */
    @TableField("own_team_no")
    @ApiModelProperty(value = "本条所属班组编号")
    private String ownTeamNo;

    /** 关联农机牌证卡号(命中已缴回即挂起) */
    @TableField("machine_card_no")
    @ApiModelProperty(value = "关联农机牌证卡号")
    private String machineCardNo;

    /** 本次受派班组编号 */
    @TableField("team_no")
    @ApiModelProperty(value = "本次受派班组编号")
    private String teamNo;

    /** 本次受派班组名称 */
    @TableField("team_name")
    @ApiModelProperty(value = "本次受派班组名称")
    private String teamName;

    /** 本次派成当日(同班组当日去重只认日期) */
    @TableField("dispatched_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "本次派成当日")
    private java.sql.Date dispatchedDate;

    /** 头一回受派班组编号(收回再派不得覆盖) */
    @TableField("first_team_no")
    @ApiModelProperty(value = "头一回受派班组编号")
    private String firstTeamNo;

    /** 头一回受派班组名称(收回再派不得覆盖) */
    @TableField("first_team_name")
    @ApiModelProperty(value = "头一回受派班组名称")
    private String firstTeamName;

    /** 头一回派出时刻(收回再派不得覆盖) */
    @TableField("first_send_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "头一回派出时刻")
    private Date firstSendAt;

    /** 班组回话时刻(空即未回话,过当日可收回) */
    @TableField("responded_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "班组回话时刻")
    private Date respondedAt;

    /** 收回重派次数 */
    @TableField("reassign_count")
    @ApiModelProperty(value = "收回重派次数")
    private Integer reassignCount;

    /** 收回缘故与历次留痕 */
    @TableField("recover_remark")
    @ApiModelProperty(value = "收回缘故与历次留痕")
    private String recoverRemark;

    /** 挂起缘故(当场写清) */
    @TableField("hang_remark")
    @ApiModelProperty(value = "挂起缘故")
    private String hangRemark;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public Date getDueAt() {
        return dueAt;
    }

    public void setDueAt(Date dueAt) {
        this.dueAt = dueAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getTimeMode() {
        return timeMode;
    }

    public void setTimeMode(Integer timeMode) {
        this.timeMode = timeMode;
    }

    public String getOwnTeamNo() {
        return ownTeamNo;
    }

    public void setOwnTeamNo(String ownTeamNo) {
        this.ownTeamNo = ownTeamNo;
    }

    public String getMachineCardNo() {
        return machineCardNo;
    }

    public void setMachineCardNo(String machineCardNo) {
        this.machineCardNo = machineCardNo;
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

    public java.sql.Date getDispatchedDate() {
        return dispatchedDate;
    }

    public void setDispatchedDate(java.sql.Date dispatchedDate) {
        this.dispatchedDate = dispatchedDate;
    }

    public String getFirstTeamNo() {
        return firstTeamNo;
    }

    public void setFirstTeamNo(String firstTeamNo) {
        this.firstTeamNo = firstTeamNo;
    }

    public String getFirstTeamName() {
        return firstTeamName;
    }

    public void setFirstTeamName(String firstTeamName) {
        this.firstTeamName = firstTeamName;
    }

    public Date getFirstSendAt() {
        return firstSendAt;
    }

    public void setFirstSendAt(Date firstSendAt) {
        this.firstSendAt = firstSendAt;
    }

    public Date getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Date respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Integer getReassignCount() {
        return reassignCount;
    }

    public void setReassignCount(Integer reassignCount) {
        this.reassignCount = reassignCount;
    }

    public String getRecoverRemark() {
        return recoverRemark;
    }

    public void setRecoverRemark(String recoverRemark) {
        this.recoverRemark = recoverRemark;
    }

    public String getHangRemark() {
        return hangRemark;
    }

    public void setHangRemark(String hangRemark) {
        this.hangRemark = hangRemark;
    }
}
