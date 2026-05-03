-- =============================================
-- 城市应急资源智能调度与复用平台
-- 数据库初始化脚本 V2.0
-- 架构优化：宽表转窄表 + NoSQL非核心字段迁移
-- =============================================

CREATE DATABASE IF NOT EXISTS emergency_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE emergency_platform;

-- =============================================
-- 核心资源表（窄表设计 - 仅包含核心交易字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_core (
    id BIGINT PRIMARY KEY COMMENT '资源ID',
    name VARCHAR(100) NOT NULL COMMENT '资源名称',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型',
    
    status INT DEFAULT 1 COMMENT '状态: 1-可用, 2-繁忙, 3-维修中, 4-离线',
    current_mode INT DEFAULT 1 COMMENT '当前模式: 1-仅救援, 2-可复用, 3-复用激活, 4-切换中',
    allow_reuse TINYINT(1) DEFAULT 0 COMMENT '是否允许复用',
    
    staff_id BIGINT COMMENT '关联服务人员ID',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_resource_type (resource_type),
    INDEX idx_status (status),
    INDEX idx_current_mode (current_mode),
    INDEX idx_staff_id (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核心资源表';

-- =============================================
-- 资源位置表（独立表 - 高频更新字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_location (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL UNIQUE COMMENT '资源ID',
    
    longitude DECIMAL(10, 7) COMMENT '经度',
    latitude DECIMAL(10, 7) COMMENT '纬度',
    
    location_update_time DATETIME COMMENT '位置更新时间',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_resource_id (resource_id),
    INDEX idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源位置表';

-- =============================================
-- 资源车辆信息表（独立表 - 车辆特定字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_vehicle (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL UNIQUE COMMENT '资源ID',
    
    vehicle_type VARCHAR(50) COMMENT '车辆类型: PICKUP-皮卡, VAN-厢式货车, TRUCK-卡车, AMBULANCE-救护车, FIRE_TRUCK-消防车',
    vehicle_sub_type VARCHAR(50) COMMENT '车辆子类型',
    vehicle_plate VARCHAR(20) COMMENT '车牌号',
    
    max_load_weight DECIMAL(10, 2) COMMENT '最大载重(吨)',
    max_load_volume DECIMAL(10, 2) COMMENT '最大容积(立方米)',
    max_passengers INT COMMENT '最大载客人数',
    
    fuel_type VARCHAR(20) COMMENT '燃料类型',
    purchase_date DATE COMMENT '购买日期',
    last_maintenance_date DATE COMMENT '上次维护日期',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_resource_id (resource_id),
    INDEX idx_vehicle_type (vehicle_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源车辆信息表';

-- =============================================
-- 资源统计表（独立表 - 统计字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_stats (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL UNIQUE COMMENT '资源ID',
    
    total_rescue_count INT DEFAULT 0 COMMENT '累计救援次数',
    total_freight_count INT DEFAULT 0 COMMENT '累计货运次数',
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    
    total_distance DECIMAL(15, 2) DEFAULT 0 COMMENT '累计行驶距离(km)',
    total_duration DECIMAL(15, 2) DEFAULT 0 COMMENT '累计工作时长(小时)',
    
    average_score DECIMAL(3, 2) DEFAULT 3.0 COMMENT '平均评分',
    score_count INT DEFAULT 0 COMMENT '评分次数',
    
    first_order_time DATETIME COMMENT '首单时间',
    last_order_time DATETIME COMMENT '最后一单时间',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_resource_id (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源统计表';

-- =============================================
-- 资源技能表（保持不变 - 已为窄表）
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_skill (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    skill_code VARCHAR(50) NOT NULL COMMENT '技能编码',
    skill_name VARCHAR(100) COMMENT '技能名称',
    skill_level INT DEFAULT 1 COMMENT '技能等级: 1-初级, 2-中级, 3-高级, 4-专家',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_resource_skill (resource_id, skill_code),
    INDEX idx_skill_code (skill_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源技能表';

-- =============================================
-- 资源服务区域表（保持不变 - 已为窄表）
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_service_area (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    area_code VARCHAR(50) NOT NULL COMMENT '区域编码',
    area_name VARCHAR(100) COMMENT '区域名称',
    priority INT DEFAULT 1 COMMENT '优先级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_resource_area (resource_id, area_code),
    INDEX idx_area_code (area_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源服务区域表';

-- =============================================
-- 核心订单表（窄表设计 - 仅包含核心交易字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_order_core (
    id BIGINT PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    order_type INT NOT NULL COMMENT '订单类型: 1-道路救援, 2-事故救援, 3-医疗救援, 4-消防救援, 5-小件货运, 6-大件货运, 7-紧急货运, 8-搬家, 9-设备运输',
    
    priority INT DEFAULT 1 COMMENT '优先级',
    urgency_level INT DEFAULT 1 COMMENT '紧急级别: 1-普通, 2-紧急, 3-非常紧急, 4-特急',
    is_emergency TINYINT(1) DEFAULT 0 COMMENT '是否紧急订单',
    
    status INT DEFAULT 1 COMMENT '状态: 1-待分配, 2-已分配, 3-进行中, 4-已完成, 5-已取消',
    
    assigned_resource_id BIGINT COMMENT '分配的资源ID',
    assigned_staff_id BIGINT COMMENT '分配的服务人员ID',
    
    expected_start_time DATETIME COMMENT '期望开始时间',
    latest_start_time DATETIME COMMENT '最晚开始时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    expected_duration INT COMMENT '预计时长(分钟)',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_order_no (order_no),
    INDEX idx_order_type (order_type),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_assigned_resource (assigned_resource_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核心订单表';

-- =============================================
-- 订单位置表（独立表 - 位置字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_order_location (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',
    
    longitude DECIMAL(10, 7) COMMENT '经度',
    latitude DECIMAL(10, 7) COMMENT '纬度',
    address VARCHAR(255) COMMENT '详细地址',
    
    dest_longitude DECIMAL(10, 7) COMMENT '目的地经度',
    dest_latitude DECIMAL(10, 7) COMMENT '目的地纬度',
    dest_address VARCHAR(255) COMMENT '目的地详细地址',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_order_id (order_id),
    INDEX idx_location (longitude, latitude),
    INDEX idx_dest_location (dest_longitude, dest_latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单位置表';

-- =============================================
-- 订单货运信息表（独立表 - 货运特定字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_order_freight (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    order_id BIGINT NOT NULL UNIQUE COMMENT '订单ID',
    
    cargo_type VARCHAR(50) COMMENT '货物类型',
    cargo_weight DECIMAL(10, 2) COMMENT '货物重量(吨)',
    cargo_volume DECIMAL(10, 2) COMMENT '货物体积(立方米)',
    
    required_vehicle_type VARCHAR(50) COMMENT '要求车辆类型',
    required_skills JSON COMMENT '要求技能列表(JSON)',
    
    has_fragile TINYINT(1) DEFAULT 0 COMMENT '是否有易碎品',
    has_hazardous TINYINT(1) DEFAULT 0 COMMENT '是否有危险品',
    needs_loading TINYINT(1) DEFAULT 0 COMMENT '是否需要装卸服务',
    
    estimated_distance DECIMAL(10, 2) COMMENT '预计距离(km)',
    estimated_cost DECIMAL(10, 2) COMMENT '预计费用',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单货运信息表';

-- =============================================
-- 订单需求技能表（保持不变 - 已为窄表）
-- =============================================
CREATE TABLE IF NOT EXISTS t_order_required_skill (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    skill_code VARCHAR(50) NOT NULL COMMENT '技能编码',
    skill_name VARCHAR(100) COMMENT '技能名称',
    required_level INT DEFAULT 1 COMMENT '要求等级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_order_skill (order_id, skill_code),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单需求技能表';

-- =============================================
-- 核心服务人员表（窄表设计 - 仅包含核心交易字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_staff_core (
    id BIGINT PRIMARY KEY COMMENT '服务人员ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    
    status INT DEFAULT 1 COMMENT '状态: 1-在职, 2-休假, 3-离职',
    current_status INT DEFAULT 1 COMMENT '当前状态: 1-空闲, 2-繁忙, 3-休息',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_current_status (current_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核心服务人员表';

-- =============================================
-- 服务人员位置表（独立表 - 高频更新字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_staff_location (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    staff_id BIGINT NOT NULL UNIQUE COMMENT '服务人员ID',
    
    longitude DECIMAL(10, 7) COMMENT '当前经度',
    latitude DECIMAL(10, 7) COMMENT '当前纬度',
    
    location_update_time DATETIME COMMENT '位置更新时间',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_staff_id (staff_id),
    INDEX idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务人员位置表';

-- =============================================
-- 服务人员信息表（独立表 - 扩展信息字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_staff_info (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    staff_id BIGINT NOT NULL UNIQUE COMMENT '服务人员ID',
    
    id_card VARCHAR(18) COMMENT '身份证号',
    gender INT COMMENT '性别: 1-男, 2-女',
    age INT COMMENT '年龄',
    avatar VARCHAR(255) COMMENT '头像URL',
    
    emergency_contact VARCHAR(50) COMMENT '紧急联系人',
    emergency_phone VARCHAR(20) COMMENT '紧急联系电话',
    
    hire_date DATE COMMENT '入职日期',
    department VARCHAR(50) COMMENT '所属部门',
    position VARCHAR(50) COMMENT '职位',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_staff_id (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务人员信息表';

-- =============================================
-- 服务人员统计表（独立表 - 统计字段）
-- =============================================
CREATE TABLE IF NOT EXISTS t_staff_stats (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    staff_id BIGINT NOT NULL UNIQUE COMMENT '服务人员ID',
    
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    total_rescue_orders INT DEFAULT 0 COMMENT '救援订单数',
    total_freight_orders INT DEFAULT 0 COMMENT '货运订单数',
    
    total_distance DECIMAL(15, 2) DEFAULT 0 COMMENT '累计行驶距离(km)',
    total_duration DECIMAL(15, 2) DEFAULT 0 COMMENT '累计工作时长(小时)',
    
    average_score DECIMAL(3, 2) DEFAULT 3.0 COMMENT '平均评分',
    score_count INT DEFAULT 0 COMMENT '评分次数',
    
    first_order_time DATETIME COMMENT '首单时间',
    last_order_time DATETIME COMMENT '最后一单时间',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_staff_id (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务人员统计表';

-- =============================================
-- 服务人员技能表（保持不变 - 已为窄表）
-- =============================================
CREATE TABLE IF NOT EXISTS t_staff_skill (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    staff_id BIGINT NOT NULL COMMENT '服务人员ID',
    skill_code VARCHAR(50) NOT NULL COMMENT '技能编码',
    skill_name VARCHAR(100) COMMENT '技能名称',
    skill_level INT DEFAULT 1 COMMENT '技能等级: 1-初级, 2-中级, 3-高级, 4-专家',
    certificate_no VARCHAR(50) COMMENT '证书编号',
    expiry_date DATE COMMENT '有效期截止日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_staff_skill (staff_id, skill_code),
    INDEX idx_skill_code (skill_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务人员技能表';

-- =============================================
-- 定位数据表（保持不变 - 已为窄表，可迁移到NoSQL）
-- =============================================
CREATE TABLE IF NOT EXISTS t_location_data (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT COMMENT '资源ID',
    staff_id BIGINT COMMENT '服务人员ID',
    
    longitude DECIMAL(10, 7) NOT NULL COMMENT '经度',
    latitude DECIMAL(10, 7) NOT NULL COMMENT '纬度',
    altitude DECIMAL(10, 2) COMMENT '海拔高度',
    
    speed DECIMAL(10, 2) COMMENT '速度(km/h)',
    direction DECIMAL(5, 2) COMMENT '方向(0-360度)',
    accuracy DECIMAL(10, 2) COMMENT '定位精度(米)',
    
    source INT NOT NULL COMMENT '定位源: 1-GPS, 2-北斗, 3-WiFi, 4-基站, 5-惯性导航',
    source_name VARCHAR(20) COMMENT '定位源名称',
    
    timestamp DATETIME NOT NULL COMMENT '定位时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_resource_id (resource_id),
    INDEX idx_staff_id (staff_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定位数据表';

-- =============================================
-- 融合定位表（保持不变 - 已为窄表，可迁移到NoSQL）
-- =============================================
CREATE TABLE IF NOT EXISTS t_fused_location (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    staff_id BIGINT COMMENT '服务人员ID',
    
    longitude DECIMAL(10, 7) NOT NULL COMMENT '融合后经度',
    latitude DECIMAL(10, 7) NOT NULL COMMENT '融合后纬度',
    altitude DECIMAL(10, 2) COMMENT '海拔高度',
    
    speed DECIMAL(10, 2) COMMENT '速度(km/h)',
    direction DECIMAL(5, 2) COMMENT '方向(0-360度)',
    accuracy DECIMAL(10, 2) COMMENT '定位精度(米)',
    
    fused_sources INT COMMENT '融合的定位源数量',
    confidence DECIMAL(5, 4) COMMENT '置信度',
    is_high_accurate TINYINT(1) DEFAULT 0 COMMENT '是否高精度(亚米级)',
    
    timestamp DATETIME NOT NULL COMMENT '定位时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_resource_time (resource_id, timestamp),
    INDEX idx_resource_id (resource_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融合定位表';

-- =============================================
-- 调度记录表（窄表设计 - 大字段迁移到NoSQL）
-- =============================================
CREATE TABLE IF NOT EXISTS t_scheduling_record (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    scheduling_id VARCHAR(50) NOT NULL COMMENT '调度批次ID',
    scheduling_type INT COMMENT '调度类型: 1-普通调度, 2-紧急调度',
    
    hard_score INT COMMENT '硬分数',
    soft_score INT COMMENT '软分数',
    total_distance DECIMAL(10, 2) COMMENT '总距离(km)',
    estimated_time DECIMAL(10, 2) COMMENT '预计总时间(分钟)',
    unassigned_count INT COMMENT '未分配订单数',
    
    order_count INT COMMENT '订单数量',
    resource_count INT COMMENT '资源数量',
    
    start_time DATETIME COMMENT '调度开始时间',
    end_time DATETIME COMMENT '调度结束时间',
    duration_ms BIGINT COMMENT '调度耗时(毫秒)',
    
    result_stored_in_nosql TINYINT(1) DEFAULT 0 COMMENT '结果是否存储在NoSQL',
    nosql_key VARCHAR(255) COMMENT 'NoSQL存储键',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_scheduling_id (scheduling_id),
    INDEX idx_scheduling_type (scheduling_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度记录表';

-- =============================================
-- 资源复用记录表（保持不变 - 已为窄表）
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_reuse_record (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    
    previous_mode INT COMMENT '之前模式',
    target_mode INT COMMENT '目标模式',
    switch_type INT COMMENT '切换类型: 1-救援到货运, 2-货运到救援',
    
    order_id BIGINT COMMENT '关联订单ID',
    order_type INT COMMENT '订单类型',
    
    switch_start_time DATETIME COMMENT '切换开始时间',
    switch_end_time DATETIME COMMENT '切换完成时间',
    switch_duration_ms BIGINT COMMENT '切换耗时(毫秒)',
    
    switch_status INT DEFAULT 1 COMMENT '切换状态: 1-成功, 2-失败, 3-取消',
    fail_reason VARCHAR(255) COMMENT '失败原因',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_resource_id (resource_id),
    INDEX idx_order_id (order_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源复用记录表';

-- =============================================
-- 系统配置表（保持不变）
-- =============================================
CREATE TABLE IF NOT EXISTS t_sys_config (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_name VARCHAR(100) COMMENT '配置名称',
    config_desc VARCHAR(255) COMMENT '配置描述',
    
    is_editable TINYINT(1) DEFAULT 1 COMMENT '是否可编辑',
    status INT DEFAULT 1 COMMENT '状态',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- =============================================
-- NoSQL迁移说明表
-- =============================================
CREATE TABLE IF NOT EXISTS t_nosql_migration (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    table_name VARCHAR(100) NOT NULL COMMENT '原表名',
    column_name VARCHAR(100) NOT NULL COMMENT '迁移字段名',
    data_type VARCHAR(50) COMMENT '数据类型',
    nosql_type VARCHAR(20) COMMENT 'NoSQL类型: MONGODB, REDIS',
    collection_name VARCHAR(100) COMMENT '集合/表名',
    key_pattern VARCHAR(255) COMMENT '键模式',
    
    reason VARCHAR(255) COMMENT '迁移原因',
    status INT DEFAULT 1 COMMENT '状态: 1-待迁移, 2-迁移中, 3-已迁移',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NoSQL迁移说明表';

-- =============================================
-- 插入NoSQL迁移配置
-- =============================================
INSERT INTO t_nosql_migration (id, table_name, column_name, data_type, nosql_type, collection_name, key_pattern, reason) VALUES
(1, 't_scheduling_record', 'order_ids', 'JSON', 'MONGODB', 'scheduling_details', 'scheduling:{scheduling_id}:orders', '大字段，高频写入，影响核心交易'),
(2, 't_scheduling_record', 'resource_ids', 'JSON', 'MONGODB', 'scheduling_details', 'scheduling:{scheduling_id}:resources', '大字段，高频写入，影响核心交易'),
(3, 't_scheduling_record', 'solution_details', 'TEXT', 'MONGODB', 'scheduling_details', 'scheduling:{scheduling_id}:solution', '大字段，高频写入，影响核心交易'),
(4, 't_order_core', 'description', 'TEXT', 'MONGODB', 'order_extra', 'order:{order_id}:description', '非核心字段，查询频率低'),
(5, 't_order_core', 'remark', 'TEXT', 'MONGODB', 'order_extra', 'order:{order_id}:remark', '非核心字段，查询频率低'),
(6, 't_location_data', '*', 'ALL', 'MONGODB', 'location_history', 'location:history:{resource_id}:{timestamp}', '历史轨迹数据，海量数据，影响核心交易'),
(7, 't_fused_location', '*', 'ALL', 'MONGODB', 'fused_location_history', 'fused:history:{resource_id}:{timestamp}', '历史融合定位数据，海量数据，影响核心交易'),
(8, 't_resource_vehicle', 'fuel_type', 'VARCHAR', 'REDIS', 'resource_cache', 'resource:{resource_id}:vehicle', '非核心字段，可缓存'),
(9, 't_resource_vehicle', 'purchase_date', 'DATE', 'REDIS', 'resource_cache', 'resource:{resource_id}:vehicle', '非核心字段，可缓存'),
(10, 't_resource_vehicle', 'last_maintenance_date', 'DATE', 'REDIS', 'resource_cache', 'resource:{resource_id}:vehicle', '非核心字段，可缓存');

-- =============================================
-- 初始化数据
-- =============================================

-- 系统配置
INSERT INTO t_sys_config (id, config_key, config_value, config_name, config_desc) VALUES
(1, 'peak.hour.morning.start', '07:00', '早高峰开始时间', '早高峰时段开始时间'),
(2, 'peak.hour.morning.end', '09:00', '早高峰结束时间', '早高峰时段结束时间'),
(3, 'peak.hour.evening.start', '17:00', '晚高峰开始时间', '晚高峰时段开始时间'),
(4, 'peak.hour.evening.end', '19:00', '晚高峰结束时间', '晚高峰时段结束时间'),
(5, 'scheduling.emergency.timeout', '5000', '紧急调度超时时间(毫秒)', '紧急任务调度的最大超时时间'),
(6, 'scheduling.normal.timeout', '30000', '普通调度超时时间(毫秒)', '普通任务调度的最大超时时间'),
(7, 'location.fusion.window', '5000', '定位融合时间窗口(毫秒)', '五模定位融合的时间窗口'),
(8, 'reuse.peak.ratio', '50', '高峰时段复用比例(%)', '高峰时段允许进入货运模式的资源比例'),
(9, 'vehicle.type.pickup', '{"maxWeight":2.0,"maxVolume":5.0,"skills":["FREIGHT_SMALL","PICKUP","MOVING"]}', '皮卡车辆配置', '皮卡类型车辆的默认配置');

-- =============================================
-- 视图设计：核心资源完整信息视图
-- =============================================
CREATE OR REPLACE VIEW v_resource_full AS
SELECT 
    r.id,
    r.name,
    r.resource_type,
    r.status,
    r.current_mode,
    r.allow_reuse,
    r.staff_id,
    r.create_time,
    r.update_time,
    
    rl.longitude,
    rl.latitude,
    rl.location_update_time,
    
    rv.vehicle_type,
    rv.vehicle_sub_type,
    rv.vehicle_plate,
    rv.max_load_weight,
    rv.max_load_volume,
    rv.max_passengers,
    
    rs.total_rescue_count,
    rs.total_freight_count,
    rs.total_orders,
    rs.average_score
FROM t_resource_core r
LEFT JOIN t_resource_location rl ON r.id = rl.resource_id
LEFT JOIN t_resource_vehicle rv ON r.id = rv.resource_id
LEFT JOIN t_resource_stats rs ON r.id = rs.resource_id
WHERE r.deleted = 0;

-- =============================================
-- 视图设计：核心订单完整信息视图
-- =============================================
CREATE OR REPLACE VIEW v_order_full AS
SELECT 
    o.id,
    o.order_no,
    o.order_type,
    o.priority,
    o.urgency_level,
    o.is_emergency,
    o.status,
    o.assigned_resource_id,
    o.assigned_staff_id,
    o.expected_start_time,
    o.latest_start_time,
    o.actual_start_time,
    o.actual_end_time,
    o.expected_duration,
    o.create_time,
    o.update_time,
    
    ol.longitude,
    ol.latitude,
    ol.address,
    ol.dest_longitude,
    ol.dest_latitude,
    ol.dest_address,
    
    of.cargo_type,
    of.cargo_weight,
    of.cargo_volume,
    of.required_vehicle_type,
    of.estimated_distance,
    of.estimated_cost
FROM t_order_core o
LEFT JOIN t_order_location ol ON o.id = ol.order_id
LEFT JOIN t_order_freight of ON o.id = of.order_id
WHERE o.deleted = 0;
