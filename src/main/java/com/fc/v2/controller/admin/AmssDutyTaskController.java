package com.fc.v2.controller.admin;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TAmssDutyLog;
import com.fc.v2.model.auto.TAmssDutyTask;
import com.fc.v2.service.ITAmssDutyTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 三夏三秋派单台账 Controller。
 * 只读：叫醒一轮派单的只有周期任务那一侧，界面既不留补条口子，也不留撤条把手；
 * 这里只回答“派没派出去、派给谁、为何挂起、收回重派的痕迹”，没有任何写操作入口。
 *
 * @author fuce
 * @date 2026-09-15
 */
@Api(value = "三夏三秋派单台账")
@Controller
@RequestMapping("/amssDutyTask")
public class AmssDutyTaskController extends BaseController {

    private final String prefix = "admin/amssDutyTask";

    @Autowired
    private ITAmssDutyTaskService amssDutyTaskService;

    @ApiOperation(value = "派单台账跳转", notes = "派单台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("amssDutyTask:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "三夏三秋派单台账", action = "list")
    @ApiOperation(value = "派单台账", notes = "只读列表：待派/已派/挂起都翻得着")
    @GetMapping("/list")
    @RequiresPermissions("amssDutyTask:list")
    @ResponseBody
    public ResultTable list(TAmssDutyTask record) {
        QueryWrapper<TAmssDutyTask> qw = new QueryWrapper<TAmssDutyTask>().eq("del_flag", 0);
        if (record.getItemNo() != null && !record.getItemNo().trim().isEmpty()) {
            qw.like("item_no", record.getItemNo().trim());
        }
        if (record.getStatus() != null) {
            qw.eq("status", record.getStatus());
        }
        if (record.getItemType() != null) {
            qw.eq("item_type", record.getItemType());
        }
        qw.orderByAsc("id");
        startPage();
        com.github.pagehelper.PageInfo<TAmssDutyTask> page =
                new com.github.pagehelper.PageInfo<TAmssDutyTask>(amssDutyTaskService.selectList(qw));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "一件事项的派单流水", notes = "派出/收回留痕，头一条不动，只读")
    @GetMapping("/logs/{itemNo}")
    @RequiresPermissions("amssDutyTask:list")
    @ResponseBody
    public List<TAmssDutyLog> logs(@PathVariable("itemNo") String itemNo) {
        return amssDutyTaskService.listLogsByItem(itemNo);
    }
}
