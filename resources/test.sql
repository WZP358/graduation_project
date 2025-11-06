DROP TABLE IF EXISTS `beatdata`;
CREATE TABLE `beatdata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `music_name` varchar(255) NOT NULL COMMENT '音乐名称',
  `beat_times` text NOT NULL COMMENT '节拍时刻数组(JSON格式)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint NOT NULL COMMENT '创建用户ID',
  `file_path` varchar(255) NOT NULL COMMENT '文件在服务器上的存放路径',
  `detection_mode` varchar(20) DEFAULT 'librosa' COMMENT '识别方式',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_music_user_mode` (`music_name`,`created_by`,`detection_mode`),
  UNIQUE KEY `idx_music_name_user_mode_unique` (`music_name`,`created_by`,`detection_mode`),
  KEY `idx_file_path` (`file_path`),
  KEY `idx_music_name` (`music_name`),
  CONSTRAINT `beatdata_ibfk_1` FOREIGN KEY (`music_name`) REFERENCES `music` (`name`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `beatdata_ibfk_2` FOREIGN KEY (`file_path`) REFERENCES `music` (`file_path`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 COMMENT='代码生成业务表';

