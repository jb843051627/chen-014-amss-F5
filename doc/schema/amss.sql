-- amss 农业机械安全监理与跨区机收作业服务管理 -- schema (chen-014)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_014

CREATE TABLE IF NOT EXISTS t_amss_chk_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '检验结论判据代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '判据名称',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '一档不合格项数封顶值',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档不合格项数封顶值',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档不合格项数封顶值',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '抢档顺位(数值越大越优先)',
  status int DEFAULT NULL COMMENT '判据的情形 0现行 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全技术检验结论判据线';

CREATE TABLE IF NOT EXISTS t_amss_coop_doc (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '农机服务组织底档字号',
  site_name varchar(128) DEFAULT NULL COMMENT '服务组织全称',
  site_type varchar(32) DEFAULT NULL COMMENT '组织类别类',
  road_name varchar(128) DEFAULT NULL COMMENT '管辖监理站(省—市—县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单卡在队面积一档上限(亩)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(亩)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(亩)',
  status int DEFAULT NULL COMMENT '底档情形 0在用 1已收回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农机服务组织底档';

CREATE TABLE IF NOT EXISTS t_amss_diff_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '通行费免收对账批次号',
  row_no int DEFAULT NULL COMMENT '原清单行次',
  item_code varchar(64) DEFAULT NULL COMMENT '差异项代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行争议差额(元)',
  status int DEFAULT NULL COMMENT '行进展 0待核 1已核销 2挂账',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通行费免收与作业证发放对账行';

CREATE TABLE IF NOT EXISTS t_amss_duty_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) NOT NULL COMMENT '派单事项号（一件事一行，不重号）',
  item_type int DEFAULT NULL COMMENT '事项类别 1检修复查 2上门维修技术服务 3跨区作业服务点值守',
  due_at datetime DEFAULT NULL COMMENT '本项应当到场那天的最后一刻（倒计时从这一刻往前倒）',
  amount decimal(12,2) DEFAULT NULL COMMENT '本站定的提前开口自然日数（口径要挪只动它）',
  busy_season int DEFAULT '0' COMMENT '是否三夏三秋重点时期 0常规 1重点(重点期上门不受两个时段约束)',
  team_no varchar(64) DEFAULT NULL COMMENT '所属班组字号（该班整季停摆则挂起）',
  mach_card_no varchar(64) DEFAULT NULL COMMENT '涉事机具牌证号（牌证年初已收回则挂起）',
  content varchar(255) DEFAULT NULL COMMENT '派单事由',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派 2挂起(派不出去)',
  first_team_no varchar(64) DEFAULT NULL COMMENT '头一回派出的班组（收回重派也不覆盖）',
  first_assign_time datetime DEFAULT NULL COMMENT '头一回派出的时刻（收回重派也不覆盖）',
  assigned_team_no varchar(64) DEFAULT NULL COMMENT '当前受派班组',
  assign_time datetime DEFAULT NULL COMMENT '当前这一遍派出的时刻',
  hold_reason varchar(255) DEFAULT NULL COMMENT '挂起缘故（当场写清，页面仍可翻见）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_duty_item_no (item_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三夏三秋检修复查与上门服务派单条目';

CREATE TABLE IF NOT EXISTS t_amss_duty_team (
  id bigint NOT NULL COMMENT '主键',
  team_no varchar(64) NOT NULL COMMENT '班组字号',
  team_name varchar(128) DEFAULT NULL COMMENT '班组名称',
  status int DEFAULT '0' COMMENT '班组情形 0当班 1整季停摆',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_duty_team_no (team_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='派单受派班组底档';

CREATE TABLE IF NOT EXISTS t_amss_mach_card (
  id bigint NOT NULL COMMENT '主键',
  card_no varchar(64) NOT NULL COMMENT '机具牌证号',
  card_name varchar(128) DEFAULT NULL COMMENT '机车/机主标注',
  status int DEFAULT '0' COMMENT '牌证情形 0有效 1年初已收回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mach_card_no (card_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='机具牌证底档（年初收回登记）';

CREATE TABLE IF NOT EXISTS t_amss_duty_log (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) NOT NULL COMMENT '派单事项号',
  team_no varchar(64) NOT NULL COMMENT '受派班组字号',
  action int NOT NULL COMMENT '流水动作 1派出 2收回',
  action_time datetime NOT NULL COMMENT '动作落账时刻',
  action_day date NOT NULL COMMENT '动作当日（同事项同班组同动作当日只留头一条）',
  reason varchar(255) DEFAULT NULL COMMENT '缘故（收回缘故等）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_duty_log_once (item_no, team_no, action_day, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='派单流水（派出/收回留痕，头一条不动）';

CREATE TABLE IF NOT EXISTS t_amss_out_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '变型拖拉机牌证缴回事务卡号',
  stage int DEFAULT NULL COMMENT '当前缴回段 0..3（收牌/验车/清账/了结）',
  status int DEFAULT NULL COMMENT '卡的落定 0未起 1在办 2已了结',
  content varchar(255) DEFAULT NULL COMMENT '缴回记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='变型拖拉机牌证缴回事务卡';

CREATE TABLE IF NOT EXISTS t_amss_pass_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '跨区作业证核准单号',
  node_no int DEFAULT NULL COMMENT '当前所在层 0..3',
  sign_mode int DEFAULT NULL COMMENT '同层核准办法 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本层应签人数',
  sign_count int DEFAULT NULL COMMENT '本层已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨区作业证逐级核准单';

CREATE TABLE IF NOT EXISTS t_amss_team_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '在队机手建档卡号',
  node_no int DEFAULT NULL COMMENT '本卡名录版本号',
  site_id int DEFAULT NULL COMMENT '所属农机服务组织底档',
  site_no varchar(64) DEFAULT NULL COMMENT '所属服务组织底档字号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡合并后在队服务面积(亩)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新进队服务面积(亩)',
  content varchar(255) DEFAULT NULL COMMENT '核验串（随名录版本走）',
  grade_level int DEFAULT NULL COMMENT '在队服务覆盖程度',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已压死',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨区作业队在机手名录建档卡';

-- 初始底档数据（跨区作业队在机手名录）
-- 单卡在队面积上限：80 / 200 / 500 亩
INSERT IGNORE INTO t_amss_coop_doc (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'ZZ00', '江原县农机服务组织底档（临川县支农农机专业合作社）', '农机合作社', '江原省—江原市—临川县', 80.00, 200.00, 500.00, 0, 0, 'seed', NOW()),
       (1, 'ZZ01', '江原县农机服务组织底档（临川县丰收跨区作业队，已收回）', '跨区作业队', '江原省—江原市—临川县', 80.00, 200.00, 500.00, 1, 0, 'seed', NOW());

-- 派单受派班组底档：T01/T02 当班，T03 整季停摆
INSERT IGNORE INTO t_amss_duty_team (id, team_no, team_name, status, del_flag, create_by, create_time)
VALUES (101, 'T01', '临川县农机一队', 0, 0, 'seed', NOW()),
       (102, 'T02', '临川县农机二队', 0, 0, 'seed', NOW()),
       (103, 'T03', '临川县农机三队（整季停摆）', 1, 0, 'seed', NOW());

-- 机具牌证底档：G0001 有效，G0002 年初已收回
INSERT IGNORE INTO t_amss_mach_card (id, card_no, card_name, status, del_flag, create_by, create_time)
VALUES (201, 'G0001', '履带拖拉机（机主老周）', 0, 0, 'seed', NOW()),
       (202, 'G0002', '轮式拖拉机（牌证已收回）', 1, 0, 'seed', NOW());

-- 派单条目种子：到期日按当天动态算，便于周期任务首轮即见账
INSERT IGNORE INTO t_amss_duty_task
(id, item_no, item_type, due_at, amount, busy_season, team_no, mach_card_no, content, status, del_flag, create_by, create_time)
VALUES
(301, 'SX-FC-0001', 1, CAST(CONCAT(CURDATE(), ' 23:59:59') AS DATETIME), 2.00, 0, 'T01', 'G0001', '履带拖拉机检修复查', 0, 0, 'seed', NOW()),
(302, 'SX-SM-0002', 2, CAST(CONCAT(CURDATE(), ' 23:59:59') AS DATETIME), 0.00, 0, 'T02', 'G0001', '上门维修技术服务（时段外按住等下轮）', 0, 0, 'seed', NOW()),
(303, 'SQ-DY-0003', 3, CAST(CONCAT(CURDATE(), ' 23:59:59') AS DATETIME), 0.00, 1, 'T01', NULL, '三秋跨区作业服务点值守（重点期不受时段约束）', 0, 0, 'seed', NOW()),
(304, 'SX-FC-0004', 1, CAST(CONCAT(CURDATE(), ' 23:59:59') AS DATETIME), 0.00, 0, 'T03', 'G0001', '检修复查（所属班组整季停摆，应挂起）', 0, 0, 'seed', NOW()),
(305, 'SX-FC-0005', 1, CAST(CONCAT(CURDATE(), ' 23:59:59') AS DATETIME), 0.00, 0, 'T02', 'G0002', '检修复查（机具牌证年初已收回，应挂起）', 0, 0, 'seed', NOW()),
(306, 'SX-FC-0006', 1, CAST(CONCAT(DATE_ADD(CURDATE(), INTERVAL 10 DAY), ' 23:59:59') AS DATETIME), 0.00, 0, 'T01', 'G0001', '十日后到场（未开口，本轮不动）', 0, 0, 'seed', NOW()),
(307, 'SX-FC-0007', 1, CAST(CONCAT(CURDATE(), ' 23:59:59') AS DATETIME), 1.00, 0, 'T01', 'G0001', '昨日派出当日未回话（应收回留痕、重派别家）',
       1, 0, 'seed', NOW());
UPDATE t_amss_duty_task
SET first_team_no = 'T01', first_assign_time = CAST(CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 10:00:00') AS DATETIME),
    assigned_team_no = 'T01', assign_time = CAST(CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 10:00:00') AS DATETIME)
WHERE item_no = 'SX-FC-0007';

