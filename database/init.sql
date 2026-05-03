-- =============================================
-- 城市应急资源智能调度与复用平台
-- 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS emergency_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE emergency_platform;

-- =============================================
-- 资源表
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource (
    id BIGINT PRIMARY KEY COMMENT '资源ID',
    name VARCHAR(100) NOT NULL COMMENT '资源名称',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型',
    sub_type VARCHAR(50) COMMENT '子类型',
    
    longitude DECIMAL(10, 7) COMMENT '经度',
    latitude DECIMAL(10, 7) COMMENT '纬度',
    address VARCHAR(255) COMMENT '所在地址',
    
    status INT DEFAULT 1 COMMENT '状态: 1-可用, 2-繁忙, 3-维修中, 4-离线',
    current_mode INT DEFAULT 1 COMMENT '当前模式: 1-仅救援, 2-可复用, 3-复用激活, 4-切换中',
    allow_reuse TINYINT(1) DEFAULT 0 COMMENT '是否允许复用',
    
    max_load INT DEFAULT 1 COMMENT '最大负载',
    current_load INT DEFAULT 0 COMMENT '当前负载',
    
    staff_id BIGINT COMMENT '关联服务人员ID',
    staff_name VARCHAR(50) COMMENT '服务人员姓名',
    
    vehicle_type VARCHAR(50) COMMENT '车辆类型',
    vehicle_capacity DECIMAL(10, 2) COMMENT '车辆容量',
    vehicle_plate VARCHAR(20) COMMENT '车牌号',
    
    total_rescue_count INT DEFAULT 0 COMMENT '累计救援次数',
    total_reuse_count INT DEFAULT 0 COMMENT '累计复用次数',
    historical_score DECIMAL(3, 2) DEFAULT 3.0 COMMENT '历史评分',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_resource_type (resource_type),
    INDEX idx_status (status),
    INDEX idx_current_mode (current_mode),
    INDEX idx_staff_id (staff_id),
    INDEX idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源表';

-- =============================================
-- 资源技能表
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_skill (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    skill_code VARCHAR(50) NOT NULL COMMENT '技能编码',
    skill_name VARCHAR(100) COMMENT '技能名称',
    skill_level INT DEFAULT 1 COMMENT '技能等级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_resource_skill (resource_id, skill_code),
    INDEX idx_skill_code (skill_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源技能表';

-- =============================================
-- 资源服务区域表
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
-- 订单表
-- =============================================
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    order_type INT NOT NULL COMMENT '订单类型',
    order_type_name VARCHAR(50) COMMENT '订单类型名称',
    
    customer_name VARCHAR(50) COMMENT '客户姓名',
    customer_phone VARCHAR(20) COMMENT '客户电话',
    
    longitude DECIMAL(10, 7) COMMENT '经度',
    latitude DECIMAL(10, 7) COMMENT '纬度',
    address VARCHAR(255) COMMENT '详细地址',
    
    priority INT DEFAULT 1 COMMENT '优先级',
    urgency_level INT DEFAULT 1 COMMENT '紧急级别',
    is_emergency TINYINT(1) DEFAULT 0 COMMENT '是否紧急订单',
    is_reuse_mode TINYINT(1) DEFAULT 0 COMMENT '是否复用模式订单',
    
    status INT DEFAULT 1 COMMENT '状态: 1-待分配, 2-已分配, 3-进行中, 4-已完成, 5-已取消',
    
    assigned_resource_id BIGINT COMMENT '分配的资源ID',
    assigned_staff_id BIGINT COMMENT '分配的服务人员ID',
    
    expected_start_time DATETIME COMMENT '期望开始时间',
    latest_start_time DATETIME COMMENT '最晚开始时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    expected_duration INT COMMENT '预计时长(分钟)',
    
    description TEXT COMMENT '订单描述',
    remark TEXT COMMENT '备注',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_order_no (order_no),
    INDEX idx_order_type (order_type),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_assigned_resource (assigned_resource_id),
    INDEX idx_create_time (create_time),
    INDEX idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- =============================================
-- 订单需求技能表
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
-- 服务人员表
-- =============================================
CREATE TABLE IF NOT EXISTS t_staff (
    id BIGINT PRIMARY KEY COMMENT '服务人员ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    id_card VARCHAR(18) COMMENT '身份证号',
    
    gender INT COMMENT '性别: 1-男, 2-女',
    age INT COMMENT '年龄',
    avatar VARCHAR(255) COMMENT '头像URL',
    
    status INT DEFAULT 1 COMMENT '状态: 1-在职, 2-休假, 3-离职',
    current_status INT DEFAULT 1 COMMENT '当前状态: 1-空闲, 2-繁忙, 3-休息',
    
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    total_rescue_orders INT DEFAULT 0 COMMENT '救援订单数',
    total_reuse_orders INT DEFAULT 0 COMMENT '复用订单数',
    average_score DECIMAL(3, 2) DEFAULT 3.0 COMMENT '平均评分',
    
    longitude DECIMAL(10, 7) COMMENT '当前经度',
    latitude DECIMAL(10, 7) COMMENT '当前纬度',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_current_status (current_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务人员表';

-- =============================================
-- 服务人员技能表
-- =============================================
CREATE TABLE IF NOT EXISTS t_staff_skill (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    staff_id BIGINT NOT NULL COMMENT '服务人员ID',
    skill_code VARCHAR(50) NOT NULL COMMENT '技能编码',
    skill_name VARCHAR(100) COMMENT '技能名称',
    skill_level INT DEFAULT 1 COMMENT '技能等级',
    certificate_no VARCHAR(50) COMMENT '证书编号',
    expiry_date DATE COMMENT '有效期截止日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_staff_skill (staff_id, skill_code),
    INDEX idx_skill_code (skill_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务人员技能表';

-- =============================================
-- 定位数据表
-- =============================================
CREATE TABLE IF NOT EXISTS t_location_data (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
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
-- 融合定位表
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
-- 调度记录表
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
    
    order_ids TEXT COMMENT '订单ID列表(JSON)',
    resource_ids TEXT COMMENT '资源ID列表(JSON)',
    solution_details TEXT COMMENT '解决方案详情',
    
    start_time DATETIME COMMENT '调度开始时间',
    end_time DATETIME COMMENT '调度结束时间',
    duration_ms BIGINT COMMENT '调度耗时(毫秒)',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_scheduling_id (scheduling_id),
    INDEX idx_scheduling_type (scheduling_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度记录表';

-- =============================================
-- 资源复用记录表
-- =============================================
CREATE TABLE IF NOT EXISTS t_resource_reuse_record (
    id BIGINT PRIMARY KEY COMMENT 'ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    
    previous_mode INT COMMENT '之前模式',
    target_mode INT COMMENT '目标模式',
    switch_type INT COMMENT '切换类型: 1-救援到复用, 2-复用到救援',
    
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
-- 系统配置表
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
(8, 'reuse.peak.ratio', '50', '高峰时段复用比例(%)', '高峰时段允许进入复用模式的资源比例');
