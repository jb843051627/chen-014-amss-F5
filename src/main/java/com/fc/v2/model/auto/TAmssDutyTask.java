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
 * 一行只管一件事；倒计时口径全站唯一：从 dueAt 这一末尾刻往前倒 amount 个自然日。
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

    /** 派单事项号（一件事一行，不重号） */
    @TableField("item_no")
    @ApiModelProperty(value = "派单事项号")
    private String itemNo;

    /** 事项类别 1检修复查 2上门维修技术服务 3跨区作业服务点值守 */
    @TableField("item_type")
    @ApiModelProperty(value = "事项类别 1检修复查 2上门维修技术服务 3跨区作业服务点值守")
    private Integer itemType;

    /** 本项应当到场那天的最后一刻（倒计时从这一刻往前倒） */
    @TableField("due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本项应当到场那天的最后一刻")
    private Date dueAt;

    /** 本站定的提前开口自然日数（口径要挪只动它，不得后推截止时刻） */
    @TableField("amount")
    @ApiModelProperty(value = "提前开口自然日数")
    private BigDecimal amount;

    /** 是否三夏三秋重点时期 0常规 1重点（重点期上门按值守班次走，不受两个时段约束） */
    @TableField("busy_season")
    @ApiModelProperty(value = "是否重点时期 0常规 1重点")
    private Integer busySeason;

    /** 所属班组字号（该班整季停摆则挂起） */
    @TableField("team_no")
    @ApiModelProperty(value = "所属班组字号")
    private String teamNo;

    /** 涉事机具牌证号（牌证年初已收回则挂起） */
    @TableField("mach_card_no")
    @ApiModelProperty(value = "涉事机具牌证号")
    private String machCardNo;

    /** 派单事由 */
    @TableField("content")
    @ApiModelProperty(value = "派单事由")
    private String content;

    /** 条目情形 0待派 1已派 2挂起(派不出去) */
    @TableField("status")
    @ApiModelProperty(value = "条目情形 0待派 1已派 2挂起")
    private Integer status;

    /** 头一回派出的班组（收回重派也不覆盖） */
    @TableField("first_team_no")
    @ApiModelProperty(value = "头一回派出的班组")
    private String firstTeamNo;

    /** 头一回派出的时刻（收回重派也不覆盖） */
    @TableField("first_assign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "头一回派出的时刻")
    private Date firstAssignTime;

    /** 当前受派班组 */
    @TableField("assigned_team_no")
    @ApiModelProperty(value = "当前受派班组")
    private String assignedTeamNo;

    /** 当前这一遍派出的时刻 */
    @TableField("assign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "当前这一遍派出的时刻")
    private Date assignTime;

    /** 挂起缘故（当场写清，页面仍可翻见） */
    @TableField("hold_reason")
    @ApiModelProperty(value = "挂起缘故")
    private String holdReason;

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

    public Integer getItemType() {
        return itemType;
    }

    public void setItemType(Integer itemType) {
        this.itemType = itemType;
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

    public Integer getBusySeason() {
        return busySeason;
    }

    public void setBusySeason(Integer busySeason) {
        this.busySeason = busySeason;
    }

    public String getTeamNo() {
        return teamNo;
    }

    public void setTeamNo(String teamNo) {
        this.teamNo = teamNo;
    }

    public String getMachCardNo() {
        return machCardNo;
    }

    public void setMachCardNo(String machCardNo) {
        this.machCardNo = machCardNo;
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

    public String getFirstTeamNo() {
        return firstTeamNo;
    }

    public void setFirstTeamNo(String firstTeamNo) {
        this.firstTeamNo = firstTeamNo;
    }

    public Date getFirstAssignTime() {
        return firstAssignTime;
    }

    public void setFirstAssignTime(Date firstAssignTime) {
        this.firstAssignTime = firstAssignTime;
    }

    public String getAssignedTeamNo() {
        return assignedTeamNo;
    }

    public void setAssignedTeamNo(String assignedTeamNo) {
        this.assignedTeamNo = assignedTeamNo;
    }

    public Date getAssignTime() {
        return assignTime;
    }

    public void setAssignTime(Date assignTime) {
        this.assignTime = assignTime;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
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
}
