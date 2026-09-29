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
  item_no varchar(64) DEFAULT NULL COMMENT '检修复查与上门服务派单事项号',
  due_at datetime DEFAULT NULL COMMENT '本项应当到场那天的最后一刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前派单的自然日数',
  content varchar(255) DEFAULT NULL COMMENT '派单事由与受派班组记要',
  time_mode int DEFAULT '0' COMMENT '时辰口径 0普通(九至十一/十五至十八两段) 1重点时期(照值守班次)',
  own_team_no varchar(64) DEFAULT NULL COMMENT '本条所属班组编号(停摆即挂起)',
  machine_card_no varchar(64) DEFAULT NULL COMMENT '关联农机牌证卡号(命中已缴回即挂起)',
  team_no varchar(64) DEFAULT NULL COMMENT '本次受派班组编号',
  team_name varchar(128) DEFAULT NULL COMMENT '本次受派班组名称',
  dispatched_date date DEFAULT NULL COMMENT '本次派成当日(同班组当日去重只认日期)',
  first_team_no varchar(64) DEFAULT NULL COMMENT '头一回受派班组编号(收回再派不得覆盖)',
  first_team_name varchar(128) DEFAULT NULL COMMENT '头一回受派班组名称(收回再派不得覆盖)',
  first_send_at datetime DEFAULT NULL COMMENT '头一回派出时刻(收回再派不得覆盖)',
  responded_at datetime DEFAULT NULL COMMENT '班组回话时刻(空即未回话,过当日可收回)',
  reassign_count int DEFAULT '0' COMMENT '收回重派次数',
  recover_remark varchar(500) DEFAULT NULL COMMENT '收回缘故与历次留痕',
  hang_remark varchar(500) DEFAULT NULL COMMENT '挂起缘故(当场写清)',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派 2挂起(派不出去)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  KEY idx_duty_item_no (item_no),
  KEY idx_duty_status (status),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三夏三秋检修复查与上门服务派单条目';

CREATE TABLE IF NOT EXISTS t_amss_duty_team (
  id bigint NOT NULL COMMENT '主键',
  team_no varchar(64) DEFAULT NULL COMMENT '班组编号',
  team_name varchar(128) DEFAULT NULL COMMENT '班组名称',
  season varchar(8) DEFAULT NULL COMMENT '值守时期 SX三夏 SQ三秋 空表示普通班组',
  shift_start varchar(8) DEFAULT NULL COMMENT '值守班次起 HH:mm(重点时期按此刻当班)',
  shift_end varchar(8) DEFAULT NULL COMMENT '值守班次止 HH:mm(重点时期按此刻当班)',
  status int DEFAULT '0' COMMENT '班组情形 0当班可用 1本季整班停摆',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='派单班组与重点时期值守班次';

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

-- 牌证缴回底档：CARD-REVOKE-001 已于年初收回（段3/已了结），命中即挂起；CARD-ACTIVE-001 在办
INSERT IGNORE INTO t_amss_out_card (id, biz_no, stage, status, content, last_action, del_flag, create_by, create_time)
VALUES (100, 'CARD-REVOKE-001', 3, 2, '变型拖拉机牌证年初已收回', '收回', 0, 'seed', NOW()),
       (101, 'CARD-ACTIVE-001', 1, 1, '牌证正常，缴回流程在办', '验车', 0, 'seed', NOW());

-- 派单班组：T01/T02 普通维修班；T03/T04 三夏值守班次（重点时期按班次，不受两段约束）；T05 本季整班停摆
INSERT IGNORE INTO t_amss_duty_team (id, team_no, team_name, season, shift_start, shift_end, status, del_flag, create_by, create_time)
VALUES (100, 'T01', '临川县第一农机维修班', NULL, NULL, NULL, 0, 0, 'seed', NOW()),
       (101, 'T02', '临川县第二农机维修班', NULL, NULL, NULL, 0, 0, 'seed', NOW()),
       (102, 'T03', '三夏跨区作业点值守甲班', 'SX', '08:00', '20:00', 0, 0, 'seed', NOW()),
       (103, 'T04', '三夏跨区作业点值守乙班', 'SX', '08:00', '20:00', 0, 0, 'seed', NOW()),
       (104, 'T05', '临川县停摆维修班（本季整班停摆）', NULL, NULL, NULL, 1, 0, 'seed', NOW());

-- 派单条目样例：due_at 一律记到场当天末尾一刻；amount 为提前开口的自然日数
-- X001 今日到期可派；X002 牌证已收回应挂起；X003 所属班组停摆应挂起；X004 重点时期按值守班；
-- X005 七日后才到开口；X006 已过期未派仍须开口；X007 已派未回话样例（次轮跑在派成次日即收回重派）
INSERT IGNORE INTO t_amss_duty_task
  (id, item_no, due_at, amount, content, time_mode, own_team_no, machine_card_no, team_no, team_name, dispatched_date,
   first_team_no, first_team_name, first_send_at, responded_at, reassign_count, status, del_flag, create_by, create_time)
VALUES
  (100, 'X001', TIMESTAMP(CURDATE(), '23:59:59'), 1.00, '履带拖拉机检修复查', 0, 'T01', 'CARD-ACTIVE-001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 'seed', NOW()),
  (101, 'X002', TIMESTAMP(CURDATE(), '23:59:59'), 1.00, '履带拖拉机检修复查（牌证已收回）', 0, 'T01', 'CARD-REVOKE-001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 'seed', NOW()),
  (102, 'X003', TIMESTAMP(CURDATE(), '23:59:59'), 1.00, '上门维修技术服务（所属班组停摆）', 0, 'T05', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 'seed', NOW()),
  (103, 'X004', TIMESTAMP(CURDATE(), '23:59:59'), 1.00, '跨区作业服务点值守', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 'seed', NOW()),
  (104, 'X005', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '23:59:59'), 1.00, '七日后到场的检修复查', 0, 'T01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 'seed', NOW()),
  (105, 'X006', TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '23:59:59'), 0.00, '昨日到期仍未派的上门服务', 0, 'T02', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 'seed', NOW()),
  (106, 'X007', TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '23:59:59'), 2.00, '已派未回话样例', 0, 'T01', NULL, 'T01', '临川县第一农机维修班', DATE_SUB(CURDATE(), INTERVAL 1 DAY),
   'T01', '临川县第一农机维修班', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 0, 1, 0, 'seed', DATE_SUB(NOW(), INTERVAL 1 DAY));

