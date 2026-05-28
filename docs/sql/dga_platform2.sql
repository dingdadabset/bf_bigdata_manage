/*
 Navicat Premium Dump SQL

 Source Server         : 大数据元数据备库-172.20.85.39
 Source Server Type    : MySQL
 Source Server Version : 50721 (5.7.21-log)
 Source Host           : 172.20.85.39:3306
 Source Schema         : dga_platform2

 Target Server Type    : MySQL
 Target Server Version : 50721 (5.7.21-log)
 File Encoding         : 65001

 Date: 21/05/2026 17:46:25
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for access_activity_evidence
-- ----------------------------
DROP TABLE IF EXISTS `access_activity_evidence`;
CREATE TABLE `access_activity_evidence`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `collected_at` datetime(6) NULL DEFAULT NULL,
  `confidence` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `evidence` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `evidence_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `last_active_at` datetime(6) NULL DEFAULT NULL,
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_system` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_access_activity_user`(`username`, `cluster_name`, `source_system`) USING BTREE,
  INDEX `idx_access_activity_source`(`source_system`, `collected_at`) USING BTREE,
  INDEX `idx_access_activity_key`(`evidence_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of access_activity_evidence
-- ----------------------------
INSERT INTO `access_activity_evidence` VALUES (1, 'HDP', 'HDP集群', '2026-05-11 01:39:59.069000', 'LOW', 'HiveServer2/Sentry grants visible: 9', 'ACTIVITY|HDP|HIVE_SERVER2|bf_dingquan', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_dingquan');
INSERT INTO `access_activity_evidence` VALUES (2, 'HDP', 'HDP集群', '2026-05-11 01:39:59.511000', 'LOW', 'HiveServer2/Sentry grants visible: 84', 'ACTIVITY|HDP|HIVE_SERVER2|hive', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'hive');
INSERT INTO `access_activity_evidence` VALUES (3, 'HDP', 'HDP集群', '2026-05-11 01:39:59.676000', 'LOW', 'HiveServer2/Sentry grants visible: 15', 'ACTIVITY|HDP|HIVE_SERVER2|liwenming', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'liwenming');
INSERT INTO `access_activity_evidence` VALUES (4, 'HDP', 'HDP集群', '2026-05-11 01:39:59.901000', 'LOW', 'HiveServer2/Sentry grants visible: 28', 'ACTIVITY|HDP|HIVE_SERVER2|xy_app_spark', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_app_spark');
INSERT INTO `access_activity_evidence` VALUES (5, 'HDP', 'HDP集群', '2026-05-11 01:40:00.050000', 'LOW', 'HiveServer2/Sentry grants visible: 38', 'ACTIVITY|HDP|HIVE_SERVER2|yanbinbin', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'yanbinbin');
INSERT INTO `access_activity_evidence` VALUES (6, 'HDP', 'HDP集群', '2026-05-11 01:40:00.088000', 'LOW', 'HiveServer2/Sentry grants visible: 28', 'ACTIVITY|HDP|HIVE_SERVER2|yarn', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'yarn');
INSERT INTO `access_activity_evidence` VALUES (7, 'HDP', 'HDP集群', '2026-05-11 01:40:00.188000', 'LOW', 'Ranger policy contains this user; Ranger does not expose user activity time here', 'ACTIVITY|HDP|RANGER|bf_dingquan', NULL, NULL, 'RANGER', 'PRESENT', 'bf_dingquan');
INSERT INTO `access_activity_evidence` VALUES (8, 'HDP', 'HDP集群', '2026-05-11 01:40:00.190000', 'LOW', 'Ranger policy contains this user; Ranger does not expose user activity time here', 'ACTIVITY|HDP|RANGER|hive', NULL, NULL, 'RANGER', 'PRESENT', 'hive');
INSERT INTO `access_activity_evidence` VALUES (9, 'HDP', 'HDP集群', '2026-05-11 01:40:00.192000', 'LOW', 'Ranger policy contains this user; Ranger does not expose user activity time here', 'ACTIVITY|HDP|RANGER|liwenming', NULL, NULL, 'RANGER', 'PRESENT', 'liwenming');
INSERT INTO `access_activity_evidence` VALUES (10, 'HDP', 'HDP集群', '2026-05-11 01:40:00.194000', 'LOW', 'Ranger policy contains this user; Ranger does not expose user activity time here', 'ACTIVITY|HDP|RANGER|xy_app_spark', NULL, NULL, 'RANGER', 'PRESENT', 'xy_app_spark');
INSERT INTO `access_activity_evidence` VALUES (11, 'HDP', 'HDP集群', '2026-05-11 01:40:00.196000', 'LOW', 'Ranger policy contains this user; Ranger does not expose user activity time here', 'ACTIVITY|HDP|RANGER|yanbinbin', NULL, NULL, 'RANGER', 'PRESENT', 'yanbinbin');
INSERT INTO `access_activity_evidence` VALUES (12, 'HDP', 'HDP集群', '2026-05-11 01:40:00.198000', 'LOW', 'Ranger policy contains this user; Ranger does not expose user activity time here', 'ACTIVITY|HDP|RANGER|yarn', NULL, NULL, 'RANGER', 'PRESENT', 'yarn');
INSERT INTO `access_activity_evidence` VALUES (13, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:00.311000', 'LOW', 'HiveServer2/Sentry grants visible: 43', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_app_admaster', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_app_admaster');
INSERT INTO `access_activity_evidence` VALUES (14, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:00.826000', 'LOW', 'HiveServer2/Sentry grants visible: 30', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_app_bi', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_app_bi');
INSERT INTO `access_activity_evidence` VALUES (15, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:04.775000', 'LOW', 'HiveServer2/Sentry grants visible: 27', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_app_output', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_app_output');
INSERT INTO `access_activity_evidence` VALUES (16, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:06.038000', 'LOW', 'HiveServer2/Sentry grants visible: 67', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_bi', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_bi');
INSERT INTO `access_activity_evidence` VALUES (17, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:06.517000', 'LOW', 'HiveServer2/Sentry grants visible: 3', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_bill', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_bill');
INSERT INTO `access_activity_evidence` VALUES (18, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:09.990000', 'LOW', 'HiveServer2/Sentry grants visible: 13', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_cp', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_cp');
INSERT INTO `access_activity_evidence` VALUES (19, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:10.495000', 'LOW', 'HiveServer2/Sentry grants visible: 6', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_dba', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_dba');
INSERT INTO `access_activity_evidence` VALUES (20, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:10.937000', 'LOW', 'HiveServer2/Sentry grants visible: 3', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_dev', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_dev');
INSERT INTO `access_activity_evidence` VALUES (21, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:12.804000', 'LOW', 'HiveServer2/Sentry grants visible: 7', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_hsq', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_hsq');
INSERT INTO `access_activity_evidence` VALUES (22, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:13.314000', 'LOW', 'HiveServer2/Sentry grants visible: 6', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_jhzf', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_jhzf');
INSERT INTO `access_activity_evidence` VALUES (23, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:13.869000', 'LOW', 'HiveServer2/Sentry grants visible: 10', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_kj_bi', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_kj_bi');
INSERT INTO `access_activity_evidence` VALUES (24, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:15.171000', 'LOW', 'HiveServer2/Sentry grants visible: 41', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_md', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_md');
INSERT INTO `access_activity_evidence` VALUES (25, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:15.777000', 'LOW', 'HiveServer2/Sentry grants visible: 337', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_oc', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_oc');
INSERT INTO `access_activity_evidence` VALUES (26, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:16.903000', 'LOW', 'HiveServer2/Sentry grants visible: 5', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_pay', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_pay');
INSERT INTO `access_activity_evidence` VALUES (27, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:17.407000', 'LOW', 'HiveServer2/Sentry grants visible: 6', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_pd', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_pd');
INSERT INTO `access_activity_evidence` VALUES (28, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:17.866000', 'LOW', 'HiveServer2/Sentry grants visible: 24', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_qd', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_qd');
INSERT INTO `access_activity_evidence` VALUES (29, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:19.109000', 'LOW', 'HiveServer2/Sentry grants visible: 52', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_qjs', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_qjs');
INSERT INTO `access_activity_evidence` VALUES (30, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:19.605000', 'LOW', 'HiveServer2/Sentry grants visible: 29', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_rm', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_rm');
INSERT INTO `access_activity_evidence` VALUES (31, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:20.131000', 'LOW', 'HiveServer2/Sentry grants visible: 14', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_trade', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_trade');
INSERT INTO `access_activity_evidence` VALUES (32, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:26.877000', 'LOW', 'HiveServer2/Sentry grants visible: 35', 'ACTIVITY|CDH_BX|HIVE_SERVER2|bf_zl', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'bf_zl');
INSERT INTO `access_activity_evidence` VALUES (33, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:29.261000', 'LOW', 'HiveServer2/Sentry grants visible: 1', 'ACTIVITY|CDH_BX|HIVE_SERVER2|credit_radar', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'credit_radar');
INSERT INTO `access_activity_evidence` VALUES (34, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:30.504000', 'LOW', 'HiveServer2/Sentry grants visible: 1', 'ACTIVITY|CDH_BX|HIVE_SERVER2|dingquan01', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'dingquan01');
INSERT INTO `access_activity_evidence` VALUES (35, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:31.042000', 'LOW', 'HiveServer2/Sentry grants visible: 2', 'ACTIVITY|CDH_BX|HIVE_SERVER2|dingquan_test', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'dingquan_test');
INSERT INTO `access_activity_evidence` VALUES (36, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:36.801000', 'LOW', 'HiveServer2/Sentry grants visible: 11', 'ACTIVITY|CDH_BX|HIVE_SERVER2|lantianwei', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'lantianwei');
INSERT INTO `access_activity_evidence` VALUES (37, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:44.740000', 'LOW', 'HiveServer2/Sentry grants visible: 16', 'ACTIVITY|CDH_BX|HIVE_SERVER2|md_liyayun', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'md_liyayun');
INSERT INTO `access_activity_evidence` VALUES (38, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:50.905000', 'LOW', 'HiveServer2/Sentry grants visible: 57', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_algo', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_algo');
INSERT INTO `access_activity_evidence` VALUES (39, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:51.407000', 'LOW', 'HiveServer2/Sentry grants visible: 6', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_app_bi', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_app_bi');
INSERT INTO `access_activity_evidence` VALUES (40, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:51.818000', 'LOW', 'HiveServer2/Sentry grants visible: 50', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_app_hive', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_app_hive');
INSERT INTO `access_activity_evidence` VALUES (41, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:52.998000', 'LOW', 'HiveServer2/Sentry grants visible: 102', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_app_spark', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_app_spark');
INSERT INTO `access_activity_evidence` VALUES (42, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:53.412000', 'LOW', 'HiveServer2/Sentry grants visible: 20', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_bigdata_develop', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_bigdata_develop');
INSERT INTO `access_activity_evidence` VALUES (43, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:53.854000', 'LOW', 'HiveServer2/Sentry grants visible: 53', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_dev', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_dev');
INSERT INTO `access_activity_evidence` VALUES (44, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:54.397000', 'LOW', 'HiveServer2/Sentry grants visible: 11', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_fx', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_fx');
INSERT INTO `access_activity_evidence` VALUES (45, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:54.831000', 'LOW', 'HiveServer2/Sentry grants visible: 22', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_jc', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_jc');
INSERT INTO `access_activity_evidence` VALUES (46, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:56.789000', 'LOW', 'HiveServer2/Sentry grants visible: 4', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_oc', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_oc');
INSERT INTO `access_activity_evidence` VALUES (47, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:58.469000', 'LOW', 'HiveServer2/Sentry grants visible: 8', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_test', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_test');
INSERT INTO `access_activity_evidence` VALUES (48, 'CDH_BX', 'CDH-宝信集群', '2026-05-11 01:42:58.950000', 'LOW', 'HiveServer2/Sentry grants visible: 2', 'ACTIVITY|CDH_BX|HIVE_SERVER2|xy_yq', NULL, NULL, 'HIVE_SERVER2', 'PRESENT', 'xy_yq');

-- ----------------------------
-- Table structure for access_governance_issue
-- ----------------------------
DROP TABLE IF EXISTS `access_governance_issue`;
CREATE TABLE `access_governance_issue`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `access_id` bigint(20) NULL DEFAULT NULL,
  `assignee` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `database_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `detected_at` datetime(6) NULL DEFAULT NULL,
  `evidence` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `issue_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `issue_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `owner` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `recommendation` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `resolved_at` datetime(6) NULL DEFAULT NULL,
  `resolved_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `resource_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_systems` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `collaborator_owners` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `confidence` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_active_at` datetime(6) NULL DEFAULT NULL,
  `last_active_source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_access_issue_status`(`status`, `severity`) USING BTREE,
  INDEX `idx_access_issue_user`(`username`, `cluster_name`) USING BTREE,
  INDEX `idx_access_issue_key`(`issue_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of access_governance_issue
-- ----------------------------
INSERT INTO `access_governance_issue` VALUES (1, NULL, NULL, 'HDP', 'HDP', NULL, '2026-05-11 01:40:00.238000', '该账号存在 1 条高权限：ALL test/* [RANGER]；复核周期 90 天。', 'HIGH_PRIVILEGE_REVIEW|HDP|bf_dingquan', 'HIGH_PRIVILEGE_REVIEW', NULL, 'ALL', '按账号复核业务必要性；确认保留后更新该账号相关高权限复核时间，不需要则回收权限。', NULL, NULL, 'ACCOUNT', 'HIGH', 'RANGER', 'OPEN', NULL, '2026-05-11 01:40:00.238000', 'bf_dingquan', NULL, NULL, NULL, NULL);
INSERT INTO `access_governance_issue` VALUES (2, NULL, NULL, 'HDP', 'HDP', NULL, '2026-05-11 01:40:00.240000', '该账号有 1 条权限缺少主负责人：ALL test/* [RANGER]。', 'UNOWNED_PERMISSION|HDP|bf_dingquan', 'UNOWNED_PERMISSION', NULL, 'ALL', '要求业务主负责人按账号认领；保存后会批量回写该账号下缺 owner 的权限记录。', NULL, NULL, 'ACCOUNT', 'MEDIUM', 'RANGER', 'OPEN', NULL, '2026-05-11 01:40:00.240000', 'bf_dingquan', NULL, NULL, NULL, NULL);
INSERT INTO `access_governance_issue` VALUES (3, NULL, NULL, 'HDP', 'HDP', NULL, '2026-05-11 01:40:00.243000', '该账号存在 2 条高权限：CREATE xy_algo/* [RANGER]；等 1 条；复核周期 90 天。', 'HIGH_PRIVILEGE_REVIEW|HDP|liwenming', 'HIGH_PRIVILEGE_REVIEW', NULL, 'CREATE', '按账号复核业务必要性；确认保留后更新该账号相关高权限复核时间，不需要则回收权限。', NULL, NULL, 'ACCOUNT', 'HIGH', 'RANGER', 'OPEN', NULL, '2026-05-11 01:40:00.243000', 'liwenming', NULL, NULL, NULL, NULL);
INSERT INTO `access_governance_issue` VALUES (4, NULL, NULL, 'HDP', 'HDP集群', NULL, '2026-05-11 01:40:00.244000', '该账号有 14 条权限缺少主负责人：SELECT xy_dev/* [RANGER]；SELECT xy_algo/* [RANGER]；SELECT xy_oms_wf/* [RANGER]；INSERT xy_dev/* [RANGER]；INSERT xy_oms_wf/* [RANGER]；INSERT xy_algo/* [RANGER]；SELECT xy_ods/* [RANGER]；SELECT xy_dw/* [RANGER]；SELECT xy_dm/* [RANGER]；SELECT iceberg_dm/* [RANGER]；等 4 条。', 'UNOWNED_PERMISSION|HDP|liwenming', 'UNOWNED_PERMISSION', NULL, 'SELECT,INSERT,CREATE', '要求业务主负责人按账号认领；保存后会批量回写该账号下缺 owner 的权限记录。', NULL, NULL, 'ACCOUNT', 'MEDIUM', 'RANGER', 'OPEN', NULL, '2026-05-11 01:40:00.244000', 'liwenming', NULL, NULL, NULL, NULL);
INSERT INTO `access_governance_issue` VALUES (5, NULL, NULL, 'HDP', 'HDP', NULL, '2026-05-11 01:40:00.246000', '该账号有 10 条权限缺少主负责人：SELECT iceberg_dm/* [RANGER]；SELECT iceberg_dw/* [RANGER]；SELECT iceberg_ods/* [RANGER]；SELECT xy_algo/* [RANGER]；SELECT xx_dispose/* [RANGER]；SELECT xy_dm/* [RANGER]；SELECT xy_dw/* [RANGER]；SELECT xy_ods/* [RANGER]；SELECT xy_oms_wf/* [RANGER]；SELECT yqy_clear/* [RANGER]。', 'UNOWNED_PERMISSION|HDP|yanbinbin', 'UNOWNED_PERMISSION', NULL, 'SELECT', '要求业务主负责人按账号认领；保存后会批量回写该账号下缺 owner 的权限记录。', NULL, NULL, 'ACCOUNT', 'MEDIUM', 'RANGER', 'OPEN', NULL, '2026-05-11 01:40:00.246000', 'yanbinbin', NULL, NULL, NULL, NULL);
INSERT INTO `access_governance_issue` VALUES (6, NULL, NULL, 'CDH_BX', 'CDH_BX', NULL, '2026-05-11 01:43:06.589000', '该账号存在 1 条高权限：ALL wgh_test/* [HIVE_SERVER2]；复核周期 90 天。', 'HIGH_PRIVILEGE_REVIEW|CDH_BX|dingquan_test', 'HIGH_PRIVILEGE_REVIEW', NULL, 'ALL', '按账号复核业务必要性；确认保留后更新该账号相关高权限复核时间，不需要则回收权限。', NULL, NULL, 'ACCOUNT', 'HIGH', 'HIVE_SERVER2', 'OPEN', NULL, '2026-05-11 01:43:06.589000', 'dingquan_test', NULL, NULL, NULL, NULL);
INSERT INTO `access_governance_issue` VALUES (7, NULL, NULL, 'CDH_BX', 'CDH_BX', NULL, '2026-05-11 01:43:06.590000', '该账号有 2 条权限缺少主负责人：ALL wgh_test/* [HIVE_SERVER2]；SELECT aggr_bill/* [HIVE_SERVER2]。', 'UNOWNED_PERMISSION|CDH_BX|dingquan_test', 'UNOWNED_PERMISSION', NULL, 'ALL,SELECT', '要求业务主负责人按账号认领；保存后会批量回写该账号下缺 owner 的权限记录。', NULL, NULL, 'ACCOUNT', 'MEDIUM', 'HIVE_SERVER2', 'OPEN', NULL, '2026-05-11 01:43:06.590000', 'dingquan_test', NULL, NULL, NULL, NULL);
INSERT INTO `access_governance_issue` VALUES (8, NULL, NULL, 'CDH_BX', 'CDH_BX', NULL, '2026-05-11 01:43:06.592000', '该账号有 1 条权限缺少主负责人：SELECT wgh_test/* [HIVE_SERVER2]。', 'UNOWNED_PERMISSION|CDH_BX|dingquan01', 'UNOWNED_PERMISSION', NULL, 'SELECT', '要求业务主负责人按账号认领；保存后会批量回写该账号下缺 owner 的权限记录。', NULL, NULL, 'ACCOUNT', 'MEDIUM', 'HIVE_SERVER2', 'OPEN', NULL, '2026-05-11 01:43:06.592000', 'dingquan01', NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for access_owner
-- ----------------------------
DROP TABLE IF EXISTS `access_owner`;
CREATE TABLE `access_owner`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_access_owner_code`(`owner_code`) USING BTREE,
  INDEX `idx_access_owner_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of access_owner
-- ----------------------------

-- ----------------------------
-- Table structure for data_source_config
-- ----------------------------
DROP TABLE IF EXISTS `data_source_config`;
CREATE TABLE `data_source_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `endpoint_id` bigint(20) NULL DEFAULT NULL,
  `is_deleted` bit(1) NULL DEFAULT NULL,
  `last_sync_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_sync_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_sync_time` datetime(6) NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ds_search_scope`(`type`, `endpoint_id`, `is_deleted`, `id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of data_source_config
-- ----------------------------

-- ----------------------------
-- Table structure for data_source_type
-- ----------------------------
DROP TABLE IF EXISTS `data_source_type`;
CREATE TABLE `data_source_type`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `jdbc_url_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_t0102m6lefauayiultj30d2kb`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of data_source_type
-- ----------------------------

-- ----------------------------
-- Table structure for dga_access_log
-- ----------------------------
DROP TABLE IF EXISTS `dga_access_log`;
CREATE TABLE `dga_access_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `permission_granted` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `request_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_resource` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_access_log_user_time`(`username`, `created_at`) USING BTREE,
  INDEX `idx_access_log_type_status_time`(`request_type`, `status`, `created_at`) USING BTREE,
  INDEX `idx_access_log_status_time`(`status`, `created_at`) USING BTREE,
  INDEX `idx_access_log_deleted_time`(`is_deleted`, `created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_access_log
-- ----------------------------

-- ----------------------------
-- Table structure for dga_cluster
-- ----------------------------
DROP TABLE IF EXISTS `dga_cluster`;
CREATE TABLE `dga_cluster`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `is_deleted` bit(1) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `hive_password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `hive_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `hive_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ldap_base` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ldap_password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ldap_urls` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ldap_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ldap_user_base` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_vx7b4wx153vvwmc02sth34yb`(`cluster_name`) USING BTREE,
  UNIQUE INDEX `UK_9xd1ywofdw1bww4n1hmknryc6`(`cluster_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_cluster
-- ----------------------------
INSERT INTO `dga_cluster` VALUES (1, 'CDH_BX', 'CDH-宝信集群', NULL, NULL, NULL, '2026-04-28 03:41:02.454000', '最重要的集群', 'ACTIVE', 'CDH', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-28 03:41:02.454000');
INSERT INTO `dga_cluster` VALUES (2, 'HDP', 'HDP集群', NULL, NULL, NULL, '2026-04-28 05:46:27.070000', '', 'ACTIVE', 'HDP', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:46:27.070000');
INSERT INTO `dga_cluster` VALUES (3, 'CDH_PBS', 'CDH-鹏博士', NULL, NULL, NULL, '2026-04-28 06:14:16.337000', '', 'ACTIVE', 'CDH', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:14:16.337000');

-- ----------------------------
-- Table structure for dga_cluster_endpoint
-- ----------------------------
DROP TABLE IF EXISTS `dga_cluster_endpoint`;
CREATE TABLE `dga_cluster_endpoint`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auth_backend` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `base_dn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `endpoint_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `service_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  `url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_base_dn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `driver_profile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `driver_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_cluster_endpoint
-- ----------------------------
INSERT INTO `dga_cluster_endpoint` VALUES (1, 'SENTRY', '', 'CDH_BX', '2026-04-28 03:43:06.768000', NULL, 'HIVE_SERVER2', 'Huyu72kj0xU', '', 'ACTIVE', '2026-04-28 03:43:06.768000', 'jdbc:hive2://172.20.85.29:10000', '', 'yarn', NULL, NULL);
INSERT INTO `dga_cluster_endpoint` VALUES (2, NULL, 'dc=baofoo,dc=com', 'CDH_BX', '2026-04-28 03:45:57.605000', NULL, 'LDAP', 'baofoo@64', '', 'ACTIVE', '2026-04-28 05:44:25.613000', 'ldap://172.20.15.13:389', 'ou=People,dc=baofoo,dc=com', 'admin', NULL, NULL);
INSERT INTO `dga_cluster_endpoint` VALUES (3, 'SENTRY', '', 'HDP', '2026-04-28 05:50:51.418000', NULL, 'HIVE_SERVER2', 'Huyu72kj0xU', '', 'ACTIVE', '2026-04-28 05:50:51.418000', 'jdbc:hive2://172.20.84.34:10000/test', '', 'yarn', NULL, NULL);
INSERT INTO `dga_cluster_endpoint` VALUES (4, 'RANGER', '', 'HDP', '2026-04-28 05:51:46.052000', NULL, 'RANGER', 'Admini8888', '', 'ACTIVE', '2026-04-28 05:51:46.052000', 'http://bigdata84-35:6080', '', 'admin', NULL, NULL);
INSERT INTO `dga_cluster_endpoint` VALUES (5, NULL, 'dc=baofoo,dc=com', 'HDP', '2026-04-28 05:53:55.196000', NULL, 'LDAP', 'baofoo@64', '', 'ACTIVE', '2026-04-28 05:53:55.196000', 'ldap://172.20.15.13:389', 'ou=People,dc=baofoo,dc=com', 'admin', NULL, NULL);
INSERT INTO `dga_cluster_endpoint` VALUES (6, NULL, 'dc=baofoo,dc=com', 'CDH_PBS', '2026-04-28 06:32:11.520000', NULL, 'LDAP', 'Ouh76wsb9hu2Lv', '', 'ACTIVE', '2026-04-28 06:32:11.520000', 'ldap://192.168.81.201:389', 'ou=People,dc=baofoo,dc=com', 'admin', NULL, NULL);

-- ----------------------------
-- Table structure for dga_column_metadata
-- ----------------------------
DROP TABLE IF EXISTS `dga_column_metadata`;
CREATE TABLE `dga_column_metadata`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `column_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `comment_str` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_primary_key` bit(1) NULL DEFAULT NULL,
  `security_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_column_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for dga_data_lineage
-- ----------------------------
DROP TABLE IF EXISTS `dga_data_lineage`;
CREATE TABLE `dga_data_lineage`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `data_source_id` bigint(20) NULL DEFAULT NULL,
  `lineage_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `parsed_at` datetime(6) NULL DEFAULT NULL,
  `run_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_endpoint_id` bigint(20) NULL DEFAULT NULL,
  `source_project` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_sql_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_table_id` bigint(20) NOT NULL,
  `source_task` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_task_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_workflow` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_table_id` bigint(20) NOT NULL,
  `transformation_logic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_data_lineage
-- ----------------------------

-- ----------------------------
-- Table structure for dga_data_theme
-- ----------------------------
DROP TABLE IF EXISTS `dga_data_theme`;
CREATE TABLE `dga_data_theme`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `parent_id` bigint(20) NULL DEFAULT NULL,
  `sort_order` int(11) NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `theme_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_data_theme
-- ----------------------------
INSERT INTO `dga_data_theme` VALUES (1, '2026-04-28 03:37:41.402000', '订单、支付、退款等交易数据', NULL, 10, 'ACTIVE', '交易主题', '2026-04-28 03:37:41.402000');
INSERT INTO `dga_data_theme` VALUES (2, '2026-04-28 03:37:41.432000', '账号、用户画像、行为等用户数据', NULL, 20, 'ACTIVE', '用户主题', '2026-04-28 03:37:41.432000');
INSERT INTO `dga_data_theme` VALUES (3, '2026-04-28 03:37:41.434000', '活动、渠道、增长等运营分析数据', NULL, 30, 'ACTIVE', '运营主题', '2026-04-28 03:37:41.434000');
INSERT INTO `dga_data_theme` VALUES (4, '2026-04-28 03:37:41.436000', '风险识别、审计、安全相关数据', NULL, 40, 'ACTIVE', '风控主题', '2026-04-28 03:37:41.436000');

-- ----------------------------
-- Table structure for dga_governance_task
-- ----------------------------
DROP TABLE IF EXISTS `dga_governance_task`;
CREATE TABLE `dga_governance_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `handler` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `issue_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `issue_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_id` bigint(20) NULL DEFAULT NULL,
  `task_status` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_governance_task
-- ----------------------------

-- ----------------------------
-- Table structure for dga_lineage_parse_task
-- ----------------------------
DROP TABLE IF EXISTS `dga_lineage_parse_task`;
CREATE TABLE `dga_lineage_parse_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `data_source_id` bigint(20) NULL DEFAULT NULL,
  `data_source_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `failed_edge_count` int(11) NULL DEFAULT NULL,
  `finished_at` datetime(6) NULL DEFAULT NULL,
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `run_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_endpoint_id` bigint(20) NULL DEFAULT NULL,
  `source_endpoint_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `started_at` datetime(6) NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `success_edge_count` int(11) NULL DEFAULT NULL,
  `trigger_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `triggered_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_lineage_parse_task
-- ----------------------------

-- ----------------------------
-- Table structure for dga_metadata_collection_task
-- ----------------------------
DROP TABLE IF EXISTS `dga_metadata_collection_task`;
CREATE TABLE `dga_metadata_collection_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `datasource_id` bigint(20) NOT NULL,
  `datasource_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `failed_table_count` int(11) NULL DEFAULT NULL,
  `finished_at` datetime(6) NULL DEFAULT NULL,
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `started_at` datetime(6) NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `success_table_count` int(11) NULL DEFAULT NULL,
  `trigger_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `triggered_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_metadata_collection_task
-- ----------------------------

-- ----------------------------
-- Table structure for dga_metadata_tag
-- ----------------------------
DROP TABLE IF EXISTS `dga_metadata_tag`;
CREATE TABLE `dga_metadata_tag`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `color` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `tag_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `tag_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_metadata_tag
-- ----------------------------

-- ----------------------------
-- Table structure for dga_metric_definition
-- ----------------------------
DROP TABLE IF EXISTS `dga_metric_definition`;
CREATE TABLE `dga_metric_definition`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `business_definition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `calculation_logic` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `metric_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `metric_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_id` bigint(20) NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_metric_definition
-- ----------------------------

-- ----------------------------
-- Table structure for dga_quality_execution
-- ----------------------------
DROP TABLE IF EXISTS `dga_quality_execution`;
CREATE TABLE `dga_quality_execution`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `error_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `executed_at` datetime(6) NULL DEFAULT NULL,
  `result_value` double NULL DEFAULT NULL,
  `rule_id` bigint(20) NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `duration_ms` bigint(20) NULL DEFAULT NULL,
  `executed_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `executed_sql` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scan_filter` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scan_scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_id` bigint(20) NULL DEFAULT NULL,
  `threshold` double NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_quality_execution
-- ----------------------------

-- ----------------------------
-- Table structure for dga_quality_issue
-- ----------------------------
DROP TABLE IF EXISTS `dga_quality_issue`;
CREATE TABLE `dga_quality_issue`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `first_seen_at` datetime(6) NULL DEFAULT NULL,
  `issue_description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `issue_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_execution_id` bigint(20) NULL DEFAULT NULL,
  `last_seen_at` datetime(6) NULL DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `resolved_at` datetime(6) NULL DEFAULT NULL,
  `result_value` double NULL DEFAULT NULL,
  `rule_id` bigint(20) NOT NULL,
  `severity` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_id` bigint(20) NULL DEFAULT NULL,
  `threshold` double NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_quality_issue
-- ----------------------------

-- ----------------------------
-- Table structure for dga_quality_rule
-- ----------------------------
DROP TABLE IF EXISTS `dga_quality_rule`;
CREATE TABLE `dga_quality_rule`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `action_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `rule_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `table_id` bigint(20) NOT NULL,
  `threshold` double NULL DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `datasource_id` bigint(20) NULL DEFAULT NULL,
  `db_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `expected_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_executed_at` datetime(6) NULL DEFAULT NULL,
  `last_execution_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_result_value` double NULL DEFAULT NULL,
  `max_value` double NULL DEFAULT NULL,
  `min_value` double NULL DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `regex_pattern` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `rule_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scan_scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `severity` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_quality_rule
-- ----------------------------

-- ----------------------------
-- Table structure for dga_resource_link
-- ----------------------------
DROP TABLE IF EXISTS `dga_resource_link`;
CREATE TABLE `dga_resource_link`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `env` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_deleted` bit(1) NULL DEFAULT NULL,
  `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `recommended` bit(1) NULL DEFAULT NULL,
  `sort_order` int(11) NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_resource_url`(`url`) USING BTREE,
  INDEX `idx_category_env`(`category`, `env`) USING BTREE,
  INDEX `idx_recommended`(`recommended`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_resource_link
-- ----------------------------
INSERT INTO `dga_resource_link` VALUES (1, 'DevOps工具', '2026-04-28 03:39:44.737000', '堡垒机 - 仅限内网访问', 'TEST', b'0', NULL, '测试环境堡垒机（Luna）', b'1', 10, 'ACTIVE', '2026-04-28 03:39:44.737000', 'http://10.0.19.86/luna/');
INSERT INTO `dga_resource_link` VALUES (2, '大数据组件', '2026-04-28 03:39:44.739000', '大数据集群管理控制台', 'TEST', b'0', NULL, '测试环境 HDP 集群（Ambari）', b'1', 20, 'ACTIVE', '2026-04-28 03:39:44.739000', 'http://10.0.25.14:8080/');

-- ----------------------------
-- Table structure for dga_system_setting
-- ----------------------------
DROP TABLE IF EXISTS `dga_system_setting`;
CREATE TABLE `dga_system_setting`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `scope` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `setting_group` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `setting_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `setting_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `value_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_setting_scope_group_key`(`scope`, `setting_group`, `setting_key`) USING BTREE,
  INDEX `idx_setting_scope_group`(`scope`, `setting_group`) USING BTREE,
  INDEX `idx_setting_key`(`setting_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_system_setting
-- ----------------------------

-- ----------------------------
-- Table structure for dga_table_business_metadata
-- ----------------------------
DROP TABLE IF EXISTS `dga_table_business_metadata`;
CREATE TABLE `dga_table_business_metadata`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `business_definition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `business_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `business_owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `theme_id` bigint(20) NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  `updated_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_table_business_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for dga_table_metadata
-- ----------------------------
DROP TABLE IF EXISTS `dga_table_metadata`;
CREATE TABLE `dga_table_metadata`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `datasource_id` bigint(20) NOT NULL,
  `db_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `last_access_time` datetime(6) NULL DEFAULT NULL,
  `location_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `record_count` bigint(20) NULL DEFAULT NULL,
  `storage_format` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `total_size` bigint(20) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_table_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for dga_table_score
-- ----------------------------
DROP TABLE IF EXISTS `dga_table_score`;
CREATE TABLE `dga_table_score`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `governance_advice` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cost_score` decimal(5, 2) NULL DEFAULT NULL,
  `quality_score` decimal(5, 2) NULL DEFAULT NULL,
  `report_date` date NOT NULL,
  `score_cost` decimal(19, 2) NULL DEFAULT NULL,
  `score_quality` decimal(19, 2) NULL DEFAULT NULL,
  `score_security` decimal(19, 2) NULL DEFAULT NULL,
  `score_spec` decimal(19, 2) NULL DEFAULT NULL,
  `security_score` decimal(5, 2) NULL DEFAULT NULL,
  `storage_score` decimal(5, 2) NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `total_score` decimal(5, 2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_table_score
-- ----------------------------

-- ----------------------------
-- Table structure for dga_table_tag_mapping
-- ----------------------------
DROP TABLE IF EXISTS `dga_table_tag_mapping`;
CREATE TABLE `dga_table_tag_mapping`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) NULL DEFAULT NULL,
  `assigned_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `tag_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_table_tag_mapping
-- ----------------------------

-- ----------------------------
-- Table structure for dga_user_recent_views
-- ----------------------------
DROP TABLE IF EXISTS `dga_user_recent_views`;
CREATE TABLE `dga_user_recent_views`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `view_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `view_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `viewed_at` datetime(6) NULL DEFAULT NULL,
  `datasource_id` bigint(20) NULL DEFAULT NULL,
  `resource_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_user_recent_views
-- ----------------------------

-- ----------------------------
-- Table structure for dga_users
-- ----------------------------
DROP TABLE IF EXISTS `dga_users`;
CREATE TABLE `dga_users`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CDH',
  `is_protected` bit(1) NULL DEFAULT NULL,
  `expires_at` datetime(6) NULL DEFAULT NULL,
  `last_active_at` datetime(6) NULL DEFAULT NULL,
  `last_active_source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(6) NOT NULL,
  `creation_strategy` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `first_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_deleted` int(11) NOT NULL DEFAULT 0,
  `last_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK4no4piterbhq6h4hjmqnp56e1`(`username`, `cluster_name`) USING BTREE,
  UNIQUE INDEX `uk_cluster_username`(`cluster_name`, `username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 292 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dga_users
-- ----------------------------
INSERT INTO `dga_users` VALUES (1, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 03:50:55.470000', 'LDAP_IMPORT', NULL, 'dingquan_test', 0, 'dingquan_test', '$2a$10$anyt7Cw8ju.rmzowusOw1OAtJrv1pW0jx5e.RYAMVzDcDgtLFoGQ6', NULL, 'dingquan_test');
INSERT INTO `dga_users` VALUES (2, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.722000', 'LDAP_IMPORT', NULL, 'hpt', 0, 'hpt', NULL, NULL, 'hpt');
INSERT INTO `dga_users` VALUES (3, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.726000', 'LDAP_IMPORT', NULL, 'bf_qiandandan', 0, 'bf_qiandandan', NULL, NULL, 'bf_qiandandan');
INSERT INTO `dga_users` VALUES (4, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.729000', 'LDAP_IMPORT', NULL, 'bf_app_spark', 0, 'bf_app_spark', NULL, NULL, 'bf_app_spark');
INSERT INTO `dga_users` VALUES (5, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.732000', 'LDAP_IMPORT', NULL, 'bf_app_bi', 0, 'bf_app_bi', NULL, NULL, 'bf_app_bi');
INSERT INTO `dga_users` VALUES (6, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.735000', 'LDAP_IMPORT', NULL, 'bf_liuting', 0, 'bf_liuting', NULL, NULL, 'bf_liuting');
INSERT INTO `dga_users` VALUES (7, 'CDH-宝信集群', b'1', NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.739000', 'LDAP_IMPORT', NULL, 'xy_app_spark', 0, 'xy_app_spark', NULL, NULL, 'xy_app_spark');
INSERT INTO `dga_users` VALUES (8, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.743000', 'LDAP_IMPORT', NULL, 'xy_app_hive', 0, 'xy_app_hive', NULL, NULL, 'xy_app_hive');
INSERT INTO `dga_users` VALUES (9, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.746000', 'LDAP_IMPORT', NULL, 'bf_app_admaster', 0, 'bf_app_admaster', NULL, NULL, 'bf_app_admaster');
INSERT INTO `dga_users` VALUES (10, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.749000', 'LDAP_IMPORT', NULL, 'bf_app_boas', 0, 'bf_app_boas', NULL, NULL, 'bf_app_boas');
INSERT INTO `dga_users` VALUES (11, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.751000', 'LDAP_IMPORT', NULL, 'bf_app_mon', 0, 'bf_app_mon', NULL, NULL, 'bf_app_mon');
INSERT INTO `dga_users` VALUES (12, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.754000', 'LDAP_IMPORT', NULL, 'bf_zhoulei', 0, 'bf_zhoulei', NULL, NULL, 'bf_zhoulei');
INSERT INTO `dga_users` VALUES (13, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.758000', 'LDAP_IMPORT', NULL, 'xy_app_prd', 0, 'xy_app_prd', NULL, NULL, 'xy_app_prd');
INSERT INTO `dga_users` VALUES (14, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.760000', 'LDAP_IMPORT', NULL, 'bf_xuanhuanhuan', 0, 'bf_xuanhuanhuan', NULL, NULL, 'bf_xuanhuanhuan');
INSERT INTO `dga_users` VALUES (15, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.763000', 'LDAP_IMPORT', NULL, 'bf_wangan', 0, 'bf_wangan', NULL, NULL, 'bf_wangan');
INSERT INTO `dga_users` VALUES (16, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.767000', 'LDAP_IMPORT', NULL, 'bf_ouxitao', 0, 'bf_ouxitao', NULL, NULL, 'bf_ouxitao');
INSERT INTO `dga_users` VALUES (17, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.769000', 'LDAP_IMPORT', NULL, 'bf_app_console', 0, 'bf_app_console', NULL, NULL, 'bf_app_console');
INSERT INTO `dga_users` VALUES (18, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.772000', 'LDAP_IMPORT', NULL, 'bf_app_clearing', 0, 'bf_app_clearing', NULL, NULL, 'bf_app_clearing');
INSERT INTO `dga_users` VALUES (19, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.776000', 'LDAP_IMPORT', NULL, 'bf_fanwenbo', 0, 'bf_fanwenbo', NULL, NULL, 'bf_fanwenbo');
INSERT INTO `dga_users` VALUES (20, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.779000', 'LDAP_IMPORT', NULL, 'bf_cdh_group_dev', 0, 'bf_cdh_group_dev', NULL, NULL, 'bf_cdh_group_dev');
INSERT INTO `dga_users` VALUES (21, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.781000', 'LDAP_IMPORT', NULL, 'bf_app_cross_border', 0, 'bf_app_cross_border', NULL, NULL, 'bf_app_cross_border');
INSERT INTO `dga_users` VALUES (22, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.784000', 'LDAP_IMPORT', NULL, 'xy_jc', 0, 'xy_jc', NULL, NULL, 'xy_jc');
INSERT INTO `dga_users` VALUES (23, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.787000', 'LDAP_IMPORT', NULL, 'xy_kf', 0, 'xy_kf', NULL, NULL, 'xy_kf');
INSERT INTO `dga_users` VALUES (24, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.789000', 'LDAP_IMPORT', NULL, 'xy_fx', 0, 'xy_fx', NULL, NULL, 'xy_fx');
INSERT INTO `dga_users` VALUES (25, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.792000', 'LDAP_IMPORT', NULL, 'bf_wulongwei', 0, 'bf_wulongwei', NULL, NULL, 'bf_wulongwei');
INSERT INTO `dga_users` VALUES (26, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.795000', 'LDAP_IMPORT', NULL, 'zhang', 0, 'li', NULL, NULL, 'bf_zhangli');
INSERT INTO `dga_users` VALUES (27, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.797000', 'LDAP_IMPORT', NULL, 'tom', 0, 'tom', NULL, NULL, 'cmjobuser');
INSERT INTO `dga_users` VALUES (28, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.800000', 'LDAP_IMPORT', NULL, 'Hadoop Yarn', 0, 'hue', NULL, NULL, 'hue');
INSERT INTO `dga_users` VALUES (29, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.802000', 'LDAP_IMPORT', NULL, 'bf_app_output', 0, 'bf_app_output', NULL, NULL, 'bf_app_output');
INSERT INTO `dga_users` VALUES (30, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.805000', 'LDAP_IMPORT', NULL, 'xy_push', 0, 'xy_push', NULL, NULL, 'xy_push');
INSERT INTO `dga_users` VALUES (31, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.807000', 'LDAP_IMPORT', NULL, 'Hive', 0, 'hive', NULL, NULL, 'hive');
INSERT INTO `dga_users` VALUES (32, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.810000', 'LDAP_IMPORT', NULL, 'bf_md', 0, 'bf_md', NULL, NULL, 'bf_md');
INSERT INTO `dga_users` VALUES (33, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.813000', 'LDAP_IMPORT', NULL, 'bf_dba', 0, 'bf_dba', NULL, NULL, 'bf_dba');
INSERT INTO `dga_users` VALUES (34, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.816000', 'LDAP_IMPORT', NULL, 'xy_yq', 0, 'xy_yq', NULL, NULL, 'xy_yq');
INSERT INTO `dga_users` VALUES (35, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.819000', 'LDAP_IMPORT', NULL, 'bf_bi', 0, 'bf_bi', NULL, NULL, 'bf_bi');
INSERT INTO `dga_users` VALUES (36, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.821000', 'LDAP_IMPORT', NULL, 'bf_dev', 0, 'bf_dev', NULL, NULL, 'bf_dev');
INSERT INTO `dga_users` VALUES (37, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.824000', 'LDAP_IMPORT', NULL, 'bf_oc', 0, 'bf_oc', NULL, NULL, 'bf_oc');
INSERT INTO `dga_users` VALUES (38, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.826000', 'LDAP_IMPORT', NULL, 'bf_rm', 0, 'bf_rm', NULL, NULL, 'bf_rm');
INSERT INTO `dga_users` VALUES (39, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.828000', 'LDAP_IMPORT', NULL, 'xy_test', 0, 'xy_test', NULL, NULL, 'xy_test');
INSERT INTO `dga_users` VALUES (40, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.831000', 'LDAP_IMPORT', NULL, 'li', 0, 'vy', NULL, NULL, 'livy');
INSERT INTO `dga_users` VALUES (41, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.834000', 'LDAP_IMPORT', NULL, 'bf_pd', 0, 'bf_pd', NULL, NULL, 'bf_pd');
INSERT INTO `dga_users` VALUES (42, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.836000', 'LDAP_IMPORT', NULL, 'bf_qd', 0, 'bf_qd', NULL, NULL, 'bf_qd');
INSERT INTO `dga_users` VALUES (43, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.840000', 'LDAP_IMPORT', NULL, 'bf_bill', 0, 'bf_bill', NULL, NULL, 'bf_bill');
INSERT INTO `dga_users` VALUES (44, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.842000', 'LDAP_IMPORT', NULL, 'bf_trade', 0, 'bf_trade', NULL, NULL, 'bf_trade');
INSERT INTO `dga_users` VALUES (45, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.845000', 'LDAP_IMPORT', NULL, 'bf_zl', 0, 'bf_zl', NULL, NULL, 'bf_zl');
INSERT INTO `dga_users` VALUES (46, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.848000', 'LDAP_IMPORT', NULL, 'bf_clr', 0, 'bf_clr', NULL, NULL, 'bf_clr');
INSERT INTO `dga_users` VALUES (47, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.850000', 'LDAP_IMPORT', NULL, 'bf_cp', 0, 'bf_cp', NULL, NULL, 'bf_cp');
INSERT INTO `dga_users` VALUES (48, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.853000', 'LDAP_IMPORT', NULL, 'bf_qjs', 0, 'bf_qjs', NULL, NULL, 'bf_qjs');
INSERT INTO `dga_users` VALUES (49, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.855000', 'LDAP_IMPORT', NULL, 'radar', 0, 'credit', NULL, NULL, 'credit_radar');
INSERT INTO `dga_users` VALUES (50, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.858000', 'LDAP_IMPORT', NULL, 'oc', 0, 'xy', NULL, NULL, 'xy_oc');
INSERT INTO `dga_users` VALUES (51, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.861000', 'LDAP_IMPORT', NULL, 'xy_app_bi', 0, 'xy_app_bi', NULL, NULL, 'xy_app_bi');
INSERT INTO `dga_users` VALUES (52, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.864000', 'LDAP_IMPORT', NULL, 'xy_rm', 0, 'xy_rm', NULL, NULL, 'xy_rm');
INSERT INTO `dga_users` VALUES (53, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.866000', 'LDAP_IMPORT', NULL, 'zeppelin', 0, 'zeppelin', NULL, NULL, 'zeppelin');
INSERT INTO `dga_users` VALUES (54, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.870000', 'LDAP_IMPORT', NULL, 'bf_hsq', 0, 'bf_hsq', NULL, NULL, 'bf_hsq');
INSERT INTO `dga_users` VALUES (55, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.872000', 'LDAP_IMPORT', NULL, 'bf_jhzf', 0, 'bf_jhzf', NULL, NULL, 'bf_jhzf');
INSERT INTO `dga_users` VALUES (56, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.875000', 'LDAP_IMPORT', NULL, 'xy_bigdata_develop', 0, 'xy_bigdata_develop', NULL, NULL, 'xy_bigdata_develop');
INSERT INTO `dga_users` VALUES (57, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.878000', 'LDAP_IMPORT', NULL, 'xy_app_hive', 0, 'xy_md', NULL, NULL, 'xy_md');
INSERT INTO `dga_users` VALUES (58, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.881000', 'LDAP_IMPORT', NULL, 'bf_kj_bi', 0, 'bf_kj_bi', NULL, NULL, 'bf_kj_bi');
INSERT INTO `dga_users` VALUES (59, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.883000', 'LDAP_IMPORT', NULL, 'xy_algo', 0, 'xy_algo', NULL, NULL, 'xy_algo');
INSERT INTO `dga_users` VALUES (60, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.886000', 'LDAP_IMPORT', NULL, 'xy_dev', 0, 'xy_dev', NULL, NULL, 'xy_dev');
INSERT INTO `dga_users` VALUES (61, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.889000', 'LDAP_IMPORT', NULL, 'md_liyayun', 0, 'md_liyayun', NULL, NULL, 'md_liyayun');
INSERT INTO `dga_users` VALUES (62, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.893000', 'LDAP_IMPORT', NULL, 'bf_pay', 0, 'bf_pay', NULL, NULL, 'bf_pay');
INSERT INTO `dga_users` VALUES (63, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.895000', 'LDAP_IMPORT', NULL, 'bf_zhaiyingying', 0, 'bf_zhaiyingying', NULL, NULL, 'bf_zhaiyingying');
INSERT INTO `dga_users` VALUES (64, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.898000', 'LDAP_IMPORT', NULL, 'bf_weiguohai', 0, 'bf_weiguohai', NULL, NULL, 'bf_weiguohai');
INSERT INTO `dga_users` VALUES (65, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.901000', 'LDAP_IMPORT', NULL, 'bf_chenguangchen', 0, 'bf_chenguangchen', NULL, NULL, 'bf_chenguangchen');
INSERT INTO `dga_users` VALUES (66, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.904000', 'LDAP_IMPORT', NULL, 'dinglulu', 0, 'dinglulu', NULL, NULL, 'dinglulu');
INSERT INTO `dga_users` VALUES (67, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.906000', 'LDAP_IMPORT', NULL, 'kangwenhua', 0, 'kangwenhua', NULL, NULL, 'kangwenhua');
INSERT INTO `dga_users` VALUES (68, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.909000', 'LDAP_IMPORT', NULL, 'zhaosong', 0, 'zhaosong', NULL, NULL, 'zhaosong');
INSERT INTO `dga_users` VALUES (69, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.911000', 'LDAP_IMPORT', NULL, 'liuxinran', 0, 'liuxinran', NULL, NULL, 'liuxinran');
INSERT INTO `dga_users` VALUES (70, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.914000', 'LDAP_IMPORT', NULL, 'wanglongbo', 0, 'wanglongbo', NULL, NULL, 'wanglongbo');
INSERT INTO `dga_users` VALUES (71, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.917000', 'LDAP_IMPORT', NULL, 'tianmin', 0, 'tianmin', NULL, NULL, 'tianmin');
INSERT INTO `dga_users` VALUES (72, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.919000', 'LDAP_IMPORT', NULL, 'liwenming', 0, 'liwenming', NULL, NULL, 'liwenming');
INSERT INTO `dga_users` VALUES (73, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.922000', 'LDAP_IMPORT', NULL, 'liuyahao', 0, 'liuyahao', NULL, NULL, 'liuyahao');
INSERT INTO `dga_users` VALUES (74, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.925000', 'LDAP_IMPORT', NULL, 'dingwenquan', 0, 'dingwenquan', NULL, NULL, 'dingwenquan');
INSERT INTO `dga_users` VALUES (75, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.927000', 'LDAP_IMPORT', NULL, 'linyifei', 0, 'linyifei', NULL, NULL, 'linyifei');
INSERT INTO `dga_users` VALUES (76, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.930000', 'LDAP_IMPORT', NULL, 'xuemin', 0, 'xuemin', NULL, NULL, 'xuemin');
INSERT INTO `dga_users` VALUES (77, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.933000', 'LDAP_IMPORT', NULL, 'mashuang', 0, 'mashuang', NULL, NULL, 'mashuang');
INSERT INTO `dga_users` VALUES (78, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.937000', 'LDAP_IMPORT', NULL, 'xushuai', 0, 'xushuai', NULL, NULL, 'xushuai');
INSERT INTO `dga_users` VALUES (79, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.940000', 'LDAP_IMPORT', NULL, 'yaole', 0, 'yaole', NULL, NULL, 'yaole');
INSERT INTO `dga_users` VALUES (80, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.943000', 'LDAP_IMPORT', NULL, 'yuanboao', 0, 'yuanboao', NULL, NULL, 'yuanboao');
INSERT INTO `dga_users` VALUES (81, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.946000', 'LDAP_IMPORT', NULL, 'jiaoyuze', 0, 'jiaoyuze', NULL, NULL, 'jiaoyuze');
INSERT INTO `dga_users` VALUES (82, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.948000', 'LDAP_IMPORT', NULL, 'limengjie', 0, 'limengjie', NULL, NULL, 'limengjie');
INSERT INTO `dga_users` VALUES (83, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.951000', 'LDAP_IMPORT', NULL, 'wuyue', 0, 'wuyue', NULL, NULL, 'wuyue');
INSERT INTO `dga_users` VALUES (84, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.954000', 'LDAP_IMPORT', NULL, 'lixuhang', 0, 'lixuhang', NULL, NULL, 'lixuhang');
INSERT INTO `dga_users` VALUES (85, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.957000', 'LDAP_IMPORT', NULL, 'wangyaru', 0, 'wangyaru', NULL, NULL, 'wangyaru');
INSERT INTO `dga_users` VALUES (86, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.959000', 'LDAP_IMPORT', NULL, 'yanggang', 0, 'yanggang', NULL, NULL, 'yanggang');
INSERT INTO `dga_users` VALUES (87, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.962000', 'LDAP_IMPORT', NULL, 'kangsiqin', 0, 'kangsiqin', NULL, NULL, 'kangsiqin');
INSERT INTO `dga_users` VALUES (88, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.964000', 'LDAP_IMPORT', NULL, 'wangjinyuan', 0, 'wangjinyuan', NULL, NULL, 'wangjinyuan');
INSERT INTO `dga_users` VALUES (89, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.967000', 'LDAP_IMPORT', NULL, 'caoshiyu', 0, 'caoshiyu', NULL, NULL, 'caoshiyu');
INSERT INTO `dga_users` VALUES (90, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.969000', 'LDAP_IMPORT', NULL, 'chengqian', 0, 'chengqian', NULL, NULL, 'chengqian');
INSERT INTO `dga_users` VALUES (91, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.972000', 'LDAP_IMPORT', NULL, 'bf_zhuhan', 0, 'bf_zhuhan', NULL, NULL, 'bf_zhuhan');
INSERT INTO `dga_users` VALUES (92, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.974000', 'LDAP_IMPORT', NULL, 'zhaojuan', 0, 'zhaojuan', NULL, NULL, 'zhaojuan');
INSERT INTO `dga_users` VALUES (93, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.977000', 'LDAP_IMPORT', NULL, 'mandao_bi', 0, 'mandao_bi', NULL, NULL, 'mandao_bi');
INSERT INTO `dga_users` VALUES (94, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.981000', 'LDAP_IMPORT', NULL, 'tanbo', 0, 'tanbo', NULL, NULL, 'tanbo');
INSERT INTO `dga_users` VALUES (95, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.983000', 'LDAP_IMPORT', NULL, 'bf_dingquan', 0, 'bf_dingquan', NULL, NULL, 'bf_dingquan');
INSERT INTO `dga_users` VALUES (96, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.985000', 'LDAP_IMPORT', NULL, 'guest', 0, 'guest', NULL, NULL, 'guest');
INSERT INTO `dga_users` VALUES (97, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.989000', 'LDAP_IMPORT', NULL, 'lantianwei', 0, 'lantianwei', NULL, NULL, 'lantianwei');
INSERT INTO `dga_users` VALUES (98, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.991000', 'LDAP_IMPORT', NULL, 'yuanweilong', 0, 'yuanweilong', NULL, NULL, 'yuanweilong');
INSERT INTO `dga_users` VALUES (99, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.994000', 'LDAP_IMPORT', NULL, 'yanbinbin', 0, 'yanbinbin', NULL, NULL, 'yanbinbin');
INSERT INTO `dga_users` VALUES (100, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:29.997000', 'LDAP_IMPORT', NULL, 'zhujiacheng', 0, 'zhujiacheng', NULL, NULL, 'zhujiacheng');
INSERT INTO `dga_users` VALUES (101, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:30.000000', 'LDAP_IMPORT', NULL, 'xuqi', 0, 'xuqi', NULL, NULL, 'xuqi');
INSERT INTO `dga_users` VALUES (102, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:30.003000', 'LDAP_IMPORT', NULL, 'zhangyaping', 0, 'zhangyaping', NULL, NULL, 'zhangyaping');
INSERT INTO `dga_users` VALUES (103, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:30.005000', 'LDAP_IMPORT', NULL, 'afanti', 0, 'afanti', NULL, NULL, 'afanti');
INSERT INTO `dga_users` VALUES (104, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:30.008000', 'LDAP_IMPORT', NULL, 'Hadoop Yarn', 0, 'yarn', NULL, NULL, 'yarn');
INSERT INTO `dga_users` VALUES (105, 'CDH-宝信集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:44:30.011000', 'LDAP_IMPORT', NULL, 'dingquan01', 0, 'dingquan01', NULL, NULL, 'dingquan01');
INSERT INTO `dga_users` VALUES (106, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.428000', 'LDAP_IMPORT', NULL, 'hpt', 0, 'hpt', NULL, NULL, 'hpt');
INSERT INTO `dga_users` VALUES (107, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.431000', 'LDAP_IMPORT', NULL, 'bf_qiandandan', 0, 'bf_qiandandan', NULL, NULL, 'bf_qiandandan');
INSERT INTO `dga_users` VALUES (108, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.434000', 'LDAP_IMPORT', NULL, 'bf_app_spark', 0, 'bf_app_spark', NULL, NULL, 'bf_app_spark');
INSERT INTO `dga_users` VALUES (109, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.436000', 'LDAP_IMPORT', NULL, 'bf_app_bi', 0, 'bf_app_bi', NULL, NULL, 'bf_app_bi');
INSERT INTO `dga_users` VALUES (110, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.439000', 'LDAP_IMPORT', NULL, 'bf_liuting', 0, 'bf_liuting', NULL, NULL, 'bf_liuting');
INSERT INTO `dga_users` VALUES (111, 'HDP集群', b'1', NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.442000', 'LDAP_IMPORT', NULL, 'xy_app_spark', 0, 'xy_app_spark', NULL, NULL, 'xy_app_spark');
INSERT INTO `dga_users` VALUES (112, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.444000', 'LDAP_IMPORT', NULL, 'xy_app_hive', 0, 'xy_app_hive', NULL, NULL, 'xy_app_hive');
INSERT INTO `dga_users` VALUES (113, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.447000', 'LDAP_IMPORT', NULL, 'bf_app_admaster', 0, 'bf_app_admaster', NULL, NULL, 'bf_app_admaster');
INSERT INTO `dga_users` VALUES (114, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.450000', 'LDAP_IMPORT', NULL, 'bf_app_boas', 0, 'bf_app_boas', NULL, NULL, 'bf_app_boas');
INSERT INTO `dga_users` VALUES (115, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.453000', 'LDAP_IMPORT', NULL, 'bf_app_mon', 0, 'bf_app_mon', NULL, NULL, 'bf_app_mon');
INSERT INTO `dga_users` VALUES (116, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.455000', 'LDAP_IMPORT', NULL, 'bf_zhoulei', 0, 'bf_zhoulei', NULL, NULL, 'bf_zhoulei');
INSERT INTO `dga_users` VALUES (117, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.458000', 'LDAP_IMPORT', NULL, 'xy_app_prd', 0, 'xy_app_prd', NULL, NULL, 'xy_app_prd');
INSERT INTO `dga_users` VALUES (118, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.460000', 'LDAP_IMPORT', NULL, 'bf_xuanhuanhuan', 0, 'bf_xuanhuanhuan', NULL, NULL, 'bf_xuanhuanhuan');
INSERT INTO `dga_users` VALUES (119, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.464000', 'LDAP_IMPORT', NULL, 'bf_wangan', 0, 'bf_wangan', NULL, NULL, 'bf_wangan');
INSERT INTO `dga_users` VALUES (120, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.467000', 'LDAP_IMPORT', NULL, 'bf_ouxitao', 0, 'bf_ouxitao', NULL, NULL, 'bf_ouxitao');
INSERT INTO `dga_users` VALUES (121, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.470000', 'LDAP_IMPORT', NULL, 'bf_app_console', 0, 'bf_app_console', NULL, NULL, 'bf_app_console');
INSERT INTO `dga_users` VALUES (122, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.473000', 'LDAP_IMPORT', NULL, 'bf_app_clearing', 0, 'bf_app_clearing', NULL, NULL, 'bf_app_clearing');
INSERT INTO `dga_users` VALUES (123, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.475000', 'LDAP_IMPORT', NULL, 'bf_fanwenbo', 0, 'bf_fanwenbo', NULL, NULL, 'bf_fanwenbo');
INSERT INTO `dga_users` VALUES (124, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.478000', 'LDAP_IMPORT', NULL, 'bf_cdh_group_dev', 0, 'bf_cdh_group_dev', NULL, NULL, 'bf_cdh_group_dev');
INSERT INTO `dga_users` VALUES (125, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.480000', 'LDAP_IMPORT', NULL, 'bf_app_cross_border', 0, 'bf_app_cross_border', NULL, NULL, 'bf_app_cross_border');
INSERT INTO `dga_users` VALUES (126, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.483000', 'LDAP_IMPORT', NULL, 'xy_jc', 0, 'xy_jc', NULL, NULL, 'xy_jc');
INSERT INTO `dga_users` VALUES (127, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.486000', 'LDAP_IMPORT', NULL, 'xy_kf', 0, 'xy_kf', NULL, NULL, 'xy_kf');
INSERT INTO `dga_users` VALUES (128, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.489000', 'LDAP_IMPORT', NULL, 'xy_fx', 0, 'xy_fx', NULL, NULL, 'xy_fx');
INSERT INTO `dga_users` VALUES (129, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.492000', 'LDAP_IMPORT', NULL, 'bf_wulongwei', 0, 'bf_wulongwei', NULL, NULL, 'bf_wulongwei');
INSERT INTO `dga_users` VALUES (130, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.496000', 'LDAP_IMPORT', NULL, 'zhang', 0, 'li', NULL, NULL, 'bf_zhangli');
INSERT INTO `dga_users` VALUES (131, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.499000', 'LDAP_IMPORT', NULL, 'tom', 0, 'tom', NULL, NULL, 'cmjobuser');
INSERT INTO `dga_users` VALUES (132, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.502000', 'LDAP_IMPORT', NULL, 'Hadoop Yarn', 0, 'hue', NULL, NULL, 'hue');
INSERT INTO `dga_users` VALUES (133, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.505000', 'LDAP_IMPORT', NULL, 'bf_app_output', 0, 'bf_app_output', NULL, NULL, 'bf_app_output');
INSERT INTO `dga_users` VALUES (134, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.508000', 'LDAP_IMPORT', NULL, 'xy_push', 0, 'xy_push', NULL, NULL, 'xy_push');
INSERT INTO `dga_users` VALUES (135, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.512000', 'LDAP_IMPORT', NULL, 'Hive', 0, 'hive', NULL, NULL, 'hive');
INSERT INTO `dga_users` VALUES (136, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.514000', 'LDAP_IMPORT', NULL, 'bf_md', 0, 'bf_md', NULL, NULL, 'bf_md');
INSERT INTO `dga_users` VALUES (137, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.518000', 'LDAP_IMPORT', NULL, 'bf_dba', 0, 'bf_dba', NULL, NULL, 'bf_dba');
INSERT INTO `dga_users` VALUES (138, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.520000', 'LDAP_IMPORT', NULL, 'xy_yq', 0, 'xy_yq', NULL, NULL, 'xy_yq');
INSERT INTO `dga_users` VALUES (139, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.523000', 'LDAP_IMPORT', NULL, 'bf_bi', 0, 'bf_bi', NULL, NULL, 'bf_bi');
INSERT INTO `dga_users` VALUES (140, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.525000', 'LDAP_IMPORT', NULL, 'bf_dev', 0, 'bf_dev', NULL, NULL, 'bf_dev');
INSERT INTO `dga_users` VALUES (141, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.527000', 'LDAP_IMPORT', NULL, 'bf_oc', 0, 'bf_oc', NULL, NULL, 'bf_oc');
INSERT INTO `dga_users` VALUES (142, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.530000', 'LDAP_IMPORT', NULL, 'bf_rm', 0, 'bf_rm', NULL, NULL, 'bf_rm');
INSERT INTO `dga_users` VALUES (143, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.534000', 'LDAP_IMPORT', NULL, 'xy_test', 0, 'xy_test', NULL, NULL, 'xy_test');
INSERT INTO `dga_users` VALUES (144, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.537000', 'LDAP_IMPORT', NULL, 'li', 0, 'vy', NULL, NULL, 'livy');
INSERT INTO `dga_users` VALUES (145, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.540000', 'LDAP_IMPORT', NULL, 'bf_pd', 0, 'bf_pd', NULL, NULL, 'bf_pd');
INSERT INTO `dga_users` VALUES (146, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.543000', 'LDAP_IMPORT', NULL, 'bf_qd', 0, 'bf_qd', NULL, NULL, 'bf_qd');
INSERT INTO `dga_users` VALUES (147, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.546000', 'LDAP_IMPORT', NULL, 'bf_bill', 0, 'bf_bill', NULL, NULL, 'bf_bill');
INSERT INTO `dga_users` VALUES (148, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.550000', 'LDAP_IMPORT', NULL, 'bf_trade', 0, 'bf_trade', NULL, NULL, 'bf_trade');
INSERT INTO `dga_users` VALUES (149, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.553000', 'LDAP_IMPORT', NULL, 'bf_zl', 0, 'bf_zl', NULL, NULL, 'bf_zl');
INSERT INTO `dga_users` VALUES (150, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.556000', 'LDAP_IMPORT', NULL, 'bf_clr', 0, 'bf_clr', NULL, NULL, 'bf_clr');
INSERT INTO `dga_users` VALUES (151, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.558000', 'LDAP_IMPORT', NULL, 'bf_cp', 0, 'bf_cp', NULL, NULL, 'bf_cp');
INSERT INTO `dga_users` VALUES (152, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.560000', 'LDAP_IMPORT', NULL, 'bf_qjs', 0, 'bf_qjs', NULL, NULL, 'bf_qjs');
INSERT INTO `dga_users` VALUES (153, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.563000', 'LDAP_IMPORT', NULL, 'radar', 0, 'credit', NULL, NULL, 'credit_radar');
INSERT INTO `dga_users` VALUES (154, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.565000', 'LDAP_IMPORT', NULL, 'oc', 0, 'xy', NULL, NULL, 'xy_oc');
INSERT INTO `dga_users` VALUES (155, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.567000', 'LDAP_IMPORT', NULL, 'xy_app_bi', 0, 'xy_app_bi', NULL, NULL, 'xy_app_bi');
INSERT INTO `dga_users` VALUES (156, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.569000', 'LDAP_IMPORT', NULL, 'xy_rm', 0, 'xy_rm', NULL, NULL, 'xy_rm');
INSERT INTO `dga_users` VALUES (157, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.571000', 'LDAP_IMPORT', NULL, 'zeppelin', 0, 'zeppelin', NULL, NULL, 'zeppelin');
INSERT INTO `dga_users` VALUES (158, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.574000', 'LDAP_IMPORT', NULL, 'bf_hsq', 0, 'bf_hsq', NULL, NULL, 'bf_hsq');
INSERT INTO `dga_users` VALUES (159, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.576000', 'LDAP_IMPORT', NULL, 'bf_jhzf', 0, 'bf_jhzf', NULL, NULL, 'bf_jhzf');
INSERT INTO `dga_users` VALUES (160, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.578000', 'LDAP_IMPORT', NULL, 'xy_bigdata_develop', 0, 'xy_bigdata_develop', NULL, NULL, 'xy_bigdata_develop');
INSERT INTO `dga_users` VALUES (161, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.580000', 'LDAP_IMPORT', NULL, 'xy_app_hive', 0, 'xy_md', NULL, NULL, 'xy_md');
INSERT INTO `dga_users` VALUES (162, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.583000', 'LDAP_IMPORT', NULL, 'bf_kj_bi', 0, 'bf_kj_bi', NULL, NULL, 'bf_kj_bi');
INSERT INTO `dga_users` VALUES (163, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.586000', 'LDAP_IMPORT', NULL, 'xy_algo', 0, 'xy_algo', NULL, NULL, 'xy_algo');
INSERT INTO `dga_users` VALUES (164, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.589000', 'LDAP_IMPORT', NULL, 'xy_dev', 0, 'xy_dev', NULL, NULL, 'xy_dev');
INSERT INTO `dga_users` VALUES (165, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.592000', 'LDAP_IMPORT', NULL, 'md_liyayun', 0, 'md_liyayun', NULL, NULL, 'md_liyayun');
INSERT INTO `dga_users` VALUES (166, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.594000', 'LDAP_IMPORT', NULL, 'bf_pay', 0, 'bf_pay', NULL, NULL, 'bf_pay');
INSERT INTO `dga_users` VALUES (167, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.596000', 'LDAP_IMPORT', NULL, 'bf_zhaiyingying', 0, 'bf_zhaiyingying', NULL, NULL, 'bf_zhaiyingying');
INSERT INTO `dga_users` VALUES (168, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.599000', 'LDAP_IMPORT', NULL, 'bf_weiguohai', 0, 'bf_weiguohai', NULL, NULL, 'bf_weiguohai');
INSERT INTO `dga_users` VALUES (169, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.601000', 'LDAP_IMPORT', NULL, 'bf_chenguangchen', 0, 'bf_chenguangchen', NULL, NULL, 'bf_chenguangchen');
INSERT INTO `dga_users` VALUES (170, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.603000', 'LDAP_IMPORT', NULL, 'dinglulu', 0, 'dinglulu', NULL, NULL, 'dinglulu');
INSERT INTO `dga_users` VALUES (171, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.606000', 'LDAP_IMPORT', NULL, 'kangwenhua', 0, 'kangwenhua', NULL, NULL, 'kangwenhua');
INSERT INTO `dga_users` VALUES (172, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.608000', 'LDAP_IMPORT', NULL, 'zhaosong', 0, 'zhaosong', NULL, NULL, 'zhaosong');
INSERT INTO `dga_users` VALUES (173, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.610000', 'LDAP_IMPORT', NULL, 'liuxinran', 0, 'liuxinran', NULL, NULL, 'liuxinran');
INSERT INTO `dga_users` VALUES (174, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.612000', 'LDAP_IMPORT', NULL, 'wanglongbo', 0, 'wanglongbo', NULL, NULL, 'wanglongbo');
INSERT INTO `dga_users` VALUES (175, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.614000', 'LDAP_IMPORT', NULL, 'tianmin', 0, 'tianmin', NULL, NULL, 'tianmin');
INSERT INTO `dga_users` VALUES (176, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.617000', 'LDAP_IMPORT', NULL, 'liwenming', 0, 'liwenming', NULL, NULL, 'liwenming');
INSERT INTO `dga_users` VALUES (177, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.620000', 'LDAP_IMPORT', NULL, 'liuyahao', 0, 'liuyahao', NULL, NULL, 'liuyahao');
INSERT INTO `dga_users` VALUES (178, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.622000', 'LDAP_IMPORT', NULL, 'dingwenquan', 0, 'dingwenquan', NULL, NULL, 'dingwenquan');
INSERT INTO `dga_users` VALUES (179, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.625000', 'LDAP_IMPORT', NULL, 'linyifei', 0, 'linyifei', NULL, NULL, 'linyifei');
INSERT INTO `dga_users` VALUES (180, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.628000', 'LDAP_IMPORT', NULL, 'xuemin', 0, 'xuemin', NULL, NULL, 'xuemin');
INSERT INTO `dga_users` VALUES (181, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.631000', 'LDAP_IMPORT', NULL, 'mashuang', 0, 'mashuang', NULL, NULL, 'mashuang');
INSERT INTO `dga_users` VALUES (182, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.634000', 'LDAP_IMPORT', NULL, 'xushuai', 0, 'xushuai', NULL, NULL, 'xushuai');
INSERT INTO `dga_users` VALUES (183, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.637000', 'LDAP_IMPORT', NULL, 'yaole', 0, 'yaole', NULL, NULL, 'yaole');
INSERT INTO `dga_users` VALUES (184, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.640000', 'LDAP_IMPORT', NULL, 'yuanboao', 0, 'yuanboao', NULL, NULL, 'yuanboao');
INSERT INTO `dga_users` VALUES (185, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.643000', 'LDAP_IMPORT', NULL, 'jiaoyuze', 0, 'jiaoyuze', NULL, NULL, 'jiaoyuze');
INSERT INTO `dga_users` VALUES (186, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.646000', 'LDAP_IMPORT', NULL, 'limengjie', 0, 'limengjie', NULL, NULL, 'limengjie');
INSERT INTO `dga_users` VALUES (187, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.649000', 'LDAP_IMPORT', NULL, 'wuyue', 0, 'wuyue', NULL, NULL, 'wuyue');
INSERT INTO `dga_users` VALUES (188, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.651000', 'LDAP_IMPORT', NULL, 'lixuhang', 0, 'lixuhang', NULL, NULL, 'lixuhang');
INSERT INTO `dga_users` VALUES (189, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.654000', 'LDAP_IMPORT', NULL, 'wangyaru', 0, 'wangyaru', NULL, NULL, 'wangyaru');
INSERT INTO `dga_users` VALUES (190, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.656000', 'LDAP_IMPORT', NULL, 'yanggang', 0, 'yanggang', NULL, NULL, 'yanggang');
INSERT INTO `dga_users` VALUES (191, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.658000', 'LDAP_IMPORT', NULL, 'kangsiqin', 0, 'kangsiqin', NULL, NULL, 'kangsiqin');
INSERT INTO `dga_users` VALUES (192, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.661000', 'LDAP_IMPORT', NULL, 'wangjinyuan', 0, 'wangjinyuan', NULL, NULL, 'wangjinyuan');
INSERT INTO `dga_users` VALUES (193, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.663000', 'LDAP_IMPORT', NULL, 'caoshiyu', 0, 'caoshiyu', NULL, NULL, 'caoshiyu');
INSERT INTO `dga_users` VALUES (194, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.666000', 'LDAP_IMPORT', NULL, 'chengqian', 0, 'chengqian', NULL, NULL, 'chengqian');
INSERT INTO `dga_users` VALUES (195, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.668000', 'LDAP_IMPORT', NULL, 'bf_zhuhan', 0, 'bf_zhuhan', NULL, NULL, 'bf_zhuhan');
INSERT INTO `dga_users` VALUES (196, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.671000', 'LDAP_IMPORT', NULL, 'zhaojuan', 0, 'zhaojuan', NULL, NULL, 'zhaojuan');
INSERT INTO `dga_users` VALUES (197, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.674000', 'LDAP_IMPORT', NULL, 'mandao_bi', 0, 'mandao_bi', NULL, NULL, 'mandao_bi');
INSERT INTO `dga_users` VALUES (198, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.677000', 'LDAP_IMPORT', NULL, 'tanbo', 0, 'tanbo', NULL, NULL, 'tanbo');
INSERT INTO `dga_users` VALUES (199, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.680000', 'LDAP_IMPORT', NULL, 'bf_dingquan', 0, 'bf_dingquan', NULL, NULL, 'bf_dingquan');
INSERT INTO `dga_users` VALUES (200, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.683000', 'LDAP_IMPORT', NULL, 'guest', 0, 'guest', NULL, NULL, 'guest');
INSERT INTO `dga_users` VALUES (201, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.687000', 'LDAP_IMPORT', NULL, 'lantianwei', 0, 'lantianwei', NULL, NULL, 'lantianwei');
INSERT INTO `dga_users` VALUES (202, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.690000', 'LDAP_IMPORT', NULL, 'yuanweilong', 0, 'yuanweilong', NULL, NULL, 'yuanweilong');
INSERT INTO `dga_users` VALUES (203, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.693000', 'LDAP_IMPORT', NULL, 'yanbinbin', 0, 'yanbinbin', NULL, NULL, 'yanbinbin');
INSERT INTO `dga_users` VALUES (204, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.696000', 'LDAP_IMPORT', NULL, 'zhujiacheng', 0, 'zhujiacheng', NULL, NULL, 'zhujiacheng');
INSERT INTO `dga_users` VALUES (205, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.699000', 'LDAP_IMPORT', NULL, 'xuqi', 0, 'xuqi', NULL, NULL, 'xuqi');
INSERT INTO `dga_users` VALUES (206, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.702000', 'LDAP_IMPORT', NULL, 'zhangyaping', 0, 'zhangyaping', NULL, NULL, 'zhangyaping');
INSERT INTO `dga_users` VALUES (207, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.705000', 'LDAP_IMPORT', NULL, 'afanti', 0, 'afanti', NULL, NULL, 'afanti');
INSERT INTO `dga_users` VALUES (208, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.708000', 'LDAP_IMPORT', NULL, 'Hadoop Yarn', 0, 'yarn', NULL, NULL, 'yarn');
INSERT INTO `dga_users` VALUES (209, 'HDP集群', NULL, NULL, NULL, NULL, NULL, '2026-04-28 05:56:47.710000', 'LDAP_IMPORT', NULL, 'dingquan01', 0, 'dingquan01', NULL, NULL, 'dingquan01');
INSERT INTO `dga_users` VALUES (210, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.637000', 'LDAP_IMPORT', NULL, 'Hadoop Yarn', 0, 'yarn', NULL, NULL, 'yarn');
INSERT INTO `dga_users` VALUES (211, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.640000', 'LDAP_IMPORT', NULL, 'Hive', 0, 'hive', NULL, NULL, 'hive');
INSERT INTO `dga_users` VALUES (212, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.643000', 'LDAP_IMPORT', NULL, 'hpt', 0, 'hpt', NULL, NULL, 'hpt');
INSERT INTO `dga_users` VALUES (213, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.645000', 'LDAP_IMPORT', NULL, 'bf_shenwangdong', 0, 'bf_shenwangdong', NULL, NULL, 'bf_shenwangdong');
INSERT INTO `dga_users` VALUES (214, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.648000', 'LDAP_IMPORT', NULL, 'bf_yanglei', 0, 'bf_yanglei', NULL, NULL, 'bf_yanglei');
INSERT INTO `dga_users` VALUES (215, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.651000', 'LDAP_IMPORT', NULL, 'bf_qiandandan', 0, 'bf_qiandandan', NULL, NULL, 'bf_qiandandan');
INSERT INTO `dga_users` VALUES (216, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.654000', 'LDAP_IMPORT', NULL, 'bf_app_spark', 0, 'bf_app_spark', NULL, NULL, 'bf_app_spark');
INSERT INTO `dga_users` VALUES (217, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.657000', 'LDAP_IMPORT', NULL, 'bf_app_bi', 0, 'bf_app_bi', NULL, NULL, 'bf_app_bi');
INSERT INTO `dga_users` VALUES (218, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.660000', 'LDAP_IMPORT', NULL, 'bf_liuting', 0, 'bf_liuting', NULL, NULL, 'bf_liuting');
INSERT INTO `dga_users` VALUES (219, 'CDH-鹏博士', b'1', NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.663000', 'LDAP_IMPORT', NULL, 'xy_app_spark', 0, 'xy_app_spark', NULL, NULL, 'xy_app_spark');
INSERT INTO `dga_users` VALUES (220, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.665000', 'LDAP_IMPORT', NULL, 'xy_app_hive', 0, 'xy_app_hive', NULL, NULL, 'xy_app_hive');
INSERT INTO `dga_users` VALUES (221, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.667000', 'LDAP_IMPORT', NULL, 'xy_jiangyuande', 0, 'xy_jiangyuande', NULL, NULL, 'xy_jiangyuande');
INSERT INTO `dga_users` VALUES (222, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.669000', 'LDAP_IMPORT', NULL, 'bf_app_admaster', 0, 'bf_app_admaster', NULL, NULL, 'bf_app_admaster');
INSERT INTO `dga_users` VALUES (223, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.671000', 'LDAP_IMPORT', NULL, 'bf_app_boas', 0, 'bf_app_boas', NULL, NULL, 'bf_app_boas');
INSERT INTO `dga_users` VALUES (224, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.672000', 'LDAP_IMPORT', NULL, 'bf_wangshu', 0, 'bf_wangshu', NULL, NULL, 'bf_wangshu');
INSERT INTO `dga_users` VALUES (225, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.674000', 'LDAP_IMPORT', NULL, 'bf_hetianlun', 0, 'bf_hetianlun', NULL, NULL, 'bf_hetianlun');
INSERT INTO `dga_users` VALUES (226, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.676000', 'LDAP_IMPORT', NULL, 'bf_app_mon', 0, 'bf_app_mon', NULL, NULL, 'bf_app_mon');
INSERT INTO `dga_users` VALUES (227, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.679000', 'LDAP_IMPORT', NULL, 'bf_zhoulei', 0, 'bf_zhoulei', NULL, NULL, 'bf_zhoulei');
INSERT INTO `dga_users` VALUES (228, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.680000', 'LDAP_IMPORT', NULL, 'bf_jiangruogu', 0, 'bf_jiangruogu', NULL, NULL, 'bf_jiangruogu');
INSERT INTO `dga_users` VALUES (229, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.683000', 'LDAP_IMPORT', NULL, 'bf_penglouchao', 0, 'bf_penglouchao', NULL, NULL, 'bf_penglouchao');
INSERT INTO `dga_users` VALUES (230, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.685000', 'LDAP_IMPORT', NULL, 'bf_shiyafei', 0, 'bf_shiyafei', NULL, NULL, 'bf_shiyafei');
INSERT INTO `dga_users` VALUES (231, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.689000', 'LDAP_IMPORT', NULL, 'bf_luolin', 0, 'bf_luolin', NULL, NULL, 'bf_luolin');
INSERT INTO `dga_users` VALUES (232, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.696000', 'LDAP_IMPORT', NULL, 'xy_app_prd', 0, 'xy_app_prd', NULL, NULL, 'xy_app_prd');
INSERT INTO `dga_users` VALUES (233, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.699000', 'LDAP_IMPORT', NULL, 'bf_duyanyan', 0, 'bf_duyanyan', NULL, NULL, 'bf_duyanyan');
INSERT INTO `dga_users` VALUES (234, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.701000', 'LDAP_IMPORT', NULL, 'bf_xuanhuanhuan', 0, 'bf_xuanhuanhuan', NULL, NULL, 'bf_xuanhuanhuan');
INSERT INTO `dga_users` VALUES (235, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.705000', 'LDAP_IMPORT', NULL, 'bf_youguoqiang', 0, 'bf_youguoqiang', NULL, NULL, 'bf_youguoqiang');
INSERT INTO `dga_users` VALUES (236, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.708000', 'LDAP_IMPORT', NULL, 'bf_wangan', 0, 'bf_wangan', NULL, NULL, 'bf_wangan');
INSERT INTO `dga_users` VALUES (237, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.712000', 'LDAP_IMPORT', NULL, 'bf_lichangya', 0, 'bf_lichangya', NULL, NULL, 'bf_lichangya');
INSERT INTO `dga_users` VALUES (238, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.715000', 'LDAP_IMPORT', NULL, 'bf_luohao', 0, 'bf_luohao', NULL, NULL, 'bf_luohao');
INSERT INTO `dga_users` VALUES (239, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.719000', 'LDAP_IMPORT', NULL, 'bf_liuxiaohui', 0, 'bf_liuxiaohui', NULL, NULL, 'bf_liuxiaohui');
INSERT INTO `dga_users` VALUES (240, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.721000', 'LDAP_IMPORT', NULL, 'bf_ouxitao', 0, 'bf_ouxitao', NULL, NULL, 'bf_ouxitao');
INSERT INTO `dga_users` VALUES (241, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.723000', 'LDAP_IMPORT', NULL, 'bf_app_console', 0, 'bf_app_console', NULL, NULL, 'bf_app_console');
INSERT INTO `dga_users` VALUES (242, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.725000', 'LDAP_IMPORT', NULL, 'bf_app_clearing', 0, 'bf_app_clearing', NULL, NULL, 'bf_app_clearing');
INSERT INTO `dga_users` VALUES (243, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.727000', 'LDAP_IMPORT', NULL, 'bf_zhangchunmei', 0, 'bf_zhangchunmei', NULL, NULL, 'bf_zhangchunmei');
INSERT INTO `dga_users` VALUES (244, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.729000', 'LDAP_IMPORT', NULL, 'bf_suhaidong', 0, 'bf_suhaidong', NULL, NULL, 'bf_suhaidong');
INSERT INTO `dga_users` VALUES (245, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.731000', 'LDAP_IMPORT', NULL, 'bf_fanwenbo', 0, 'bf_fanwenbo', NULL, NULL, 'bf_fanwenbo');
INSERT INTO `dga_users` VALUES (246, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.733000', 'LDAP_IMPORT', NULL, 'bf_gaoxinghong', 0, 'bf_gaoxinghong', NULL, NULL, 'bf_gaoxinghong');
INSERT INTO `dga_users` VALUES (247, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.735000', 'LDAP_IMPORT', NULL, 'bf_cdh_group_dev', 0, 'bf_cdh_group_dev', NULL, NULL, 'bf_cdh_group_dev');
INSERT INTO `dga_users` VALUES (248, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.737000', 'LDAP_IMPORT', NULL, 'bf_caowenyi', 0, 'bf_caowenyi', NULL, NULL, 'bf_caowenyi');
INSERT INTO `dga_users` VALUES (249, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.739000', 'LDAP_IMPORT', NULL, 'bf_app_cross_border', 0, 'bf_app_cross_border', NULL, NULL, 'bf_app_cross_border');
INSERT INTO `dga_users` VALUES (250, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.741000', 'LDAP_IMPORT', NULL, 'bf_fuxiangkui', 0, 'bf_fuxiangkui', NULL, NULL, 'bf_fuxiangkui');
INSERT INTO `dga_users` VALUES (251, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.743000', 'LDAP_IMPORT', NULL, 'xy_jc', 0, 'xy_jc', NULL, NULL, 'xy_jc');
INSERT INTO `dga_users` VALUES (252, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.746000', 'LDAP_IMPORT', NULL, 'xy_app_hive', 0, 'xy_md', NULL, NULL, 'xy_md');
INSERT INTO `dga_users` VALUES (253, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.749000', 'LDAP_IMPORT', NULL, 'xy_kf', 0, 'xy_kf', NULL, NULL, 'xy_kf');
INSERT INTO `dga_users` VALUES (254, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.752000', 'LDAP_IMPORT', NULL, 'xy_fx', 0, 'xy_fx', NULL, NULL, 'xy_fx');
INSERT INTO `dga_users` VALUES (255, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.755000', 'LDAP_IMPORT', NULL, 'xy_zhuwenxiao', 0, 'bf_jianghuajun', NULL, NULL, 'bf_jianghuajun');
INSERT INTO `dga_users` VALUES (256, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.758000', 'LDAP_IMPORT', NULL, 'bf_xuchuanduo', 0, 'bf_xuchuanduo', NULL, NULL, 'bf_xuchuanduo');
INSERT INTO `dga_users` VALUES (257, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.761000', 'LDAP_IMPORT', NULL, 'bf_wulongwei', 0, 'bf_wulongwei', NULL, NULL, 'bf_wulongwei');
INSERT INTO `dga_users` VALUES (258, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.765000', 'LDAP_IMPORT', NULL, 'bf_yinghongmiao', 0, 'bf_yinghongmiao', NULL, NULL, 'bf_yinghongmiao');
INSERT INTO `dga_users` VALUES (259, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.767000', 'LDAP_IMPORT', NULL, 'zhang', 0, 'li', NULL, NULL, 'bf_zhangli');
INSERT INTO `dga_users` VALUES (260, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.771000', 'LDAP_IMPORT', NULL, 'zhang', 0, 'li', NULL, NULL, 'bf_yangxingye');
INSERT INTO `dga_users` VALUES (261, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.773000', 'LDAP_IMPORT', NULL, 'bf_hanyang', 0, 'bf_hanyang', NULL, NULL, 'bf_hanyang');
INSERT INTO `dga_users` VALUES (262, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.776000', 'LDAP_IMPORT', NULL, 'Hive', 0, 'admin', NULL, NULL, 'admin');
INSERT INTO `dga_users` VALUES (263, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.779000', 'LDAP_IMPORT', NULL, 'bf_xiexin', 0, 'bf_xiexin', NULL, NULL, 'bf_xiexin');
INSERT INTO `dga_users` VALUES (264, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.787000', 'LDAP_IMPORT', NULL, 'bf_app_output', 0, 'bf_app_output', NULL, NULL, 'bf_app_output');
INSERT INTO `dga_users` VALUES (265, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.790000', 'LDAP_IMPORT', NULL, 'xy_push', 0, 'xy_push', NULL, NULL, 'xy_push');
INSERT INTO `dga_users` VALUES (266, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.793000', 'LDAP_IMPORT', NULL, 'bf_dba', 0, 'bf_dba', NULL, NULL, 'bf_dba');
INSERT INTO `dga_users` VALUES (267, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.795000', 'LDAP_IMPORT', NULL, 'xy_yq', 0, 'xy_yq', NULL, NULL, 'xy_yq');
INSERT INTO `dga_users` VALUES (268, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.797000', 'LDAP_IMPORT', NULL, 'bf_bi', 0, 'bf_bi', NULL, NULL, 'bf_bi');
INSERT INTO `dga_users` VALUES (269, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.800000', 'LDAP_IMPORT', NULL, 'bf_dev', 0, 'bf_dev', NULL, NULL, 'bf_dev');
INSERT INTO `dga_users` VALUES (270, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.802000', 'LDAP_IMPORT', NULL, 'bf_pay', 0, 'bf_pay', NULL, NULL, 'bf_pay');
INSERT INTO `dga_users` VALUES (271, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.804000', 'LDAP_IMPORT', NULL, 'bf_rm', 0, 'bf_rm', NULL, NULL, 'bf_rm');
INSERT INTO `dga_users` VALUES (272, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.807000', 'LDAP_IMPORT', NULL, 'bf_oc', 0, 'bf_oc', NULL, NULL, 'bf_oc');
INSERT INTO `dga_users` VALUES (273, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.809000', 'LDAP_IMPORT', NULL, 'bf_md', 0, 'bf_md', NULL, NULL, 'bf_md');
INSERT INTO `dga_users` VALUES (274, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.811000', 'LDAP_IMPORT', NULL, 'bf_qd', 0, 'bf_qd', NULL, NULL, 'bf_qd');
INSERT INTO `dga_users` VALUES (275, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.813000', 'LDAP_IMPORT', NULL, 'dba_liusong', 0, 'dba_liusong', NULL, NULL, 'dba_liusong');
INSERT INTO `dga_users` VALUES (276, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.815000', 'LDAP_IMPORT', NULL, 'dev_security', 0, 'dev_security', NULL, NULL, 'dev_security');
INSERT INTO `dga_users` VALUES (277, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.817000', 'LDAP_IMPORT', NULL, 'xy_app_bi', 0, 'xy_app_bi', NULL, NULL, 'xy_app_bi');
INSERT INTO `dga_users` VALUES (278, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.819000', 'LDAP_IMPORT', NULL, 'bf_hsq', 0, 'bf_hsq', NULL, NULL, 'bf_hsq');
INSERT INTO `dga_users` VALUES (279, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.821000', 'LDAP_IMPORT', NULL, 'bf_trade', 0, 'bf_trade', NULL, NULL, 'bf_trade');
INSERT INTO `dga_users` VALUES (280, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.823000', 'LDAP_IMPORT', NULL, 'bf_zhaiyingying', 0, 'bf_zhaiyingying', NULL, NULL, 'bf_zhaiyingying');
INSERT INTO `dga_users` VALUES (281, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.826000', 'LDAP_IMPORT', NULL, 'bf_weiguohai', 0, 'bf_weiguohai', NULL, NULL, 'bf_weiguohai');
INSERT INTO `dga_users` VALUES (282, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.828000', 'LDAP_IMPORT', NULL, 'bf_chenguangchen', 0, 'bf_chenguangchen', NULL, NULL, 'bf_chenguangchen');
INSERT INTO `dga_users` VALUES (283, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.830000', 'LDAP_IMPORT', NULL, 'zhaojuan', 0, 'zhaojuan', NULL, NULL, 'zhaojuan');
INSERT INTO `dga_users` VALUES (284, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.832000', 'LDAP_IMPORT', NULL, 'dingwenquan', 0, 'dingwenquan', NULL, NULL, 'dingwenquan');
INSERT INTO `dga_users` VALUES (285, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.834000', 'LDAP_IMPORT', NULL, 'mandao_bi', 0, 'mandao_bi', NULL, NULL, 'mandao_bi');
INSERT INTO `dga_users` VALUES (286, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.836000', 'LDAP_IMPORT', NULL, 'tanbo', 0, 'tanbo', NULL, NULL, 'tanbo');
INSERT INTO `dga_users` VALUES (287, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.838000', 'LDAP_IMPORT', NULL, 'zhangchanglian', 0, 'zhangchanglian', NULL, NULL, 'zhangchanglian');
INSERT INTO `dga_users` VALUES (288, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.840000', 'LDAP_IMPORT', NULL, 'md_liyayun', 0, 'md_liyayun', NULL, NULL, 'md_liyayun');
INSERT INTO `dga_users` VALUES (289, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:34:32.843000', 'LDAP_IMPORT', NULL, 'zhangyaping', 0, 'zhangyaping', NULL, NULL, 'zhangyaping');
INSERT INTO `dga_users` VALUES (290, 'CDH-鹏博士', NULL, NULL, NULL, NULL, NULL, '2026-04-28 06:49:44.014000', 'OPENLDAP', '', 'bf_dingquan', 0, 'User', '$2a$10$gfM9kzRLm2.OzQNK.LYxNez./Veb8wxvnjv1nSYnE.7KATfLyYvCy', NULL, 'bf_dingquan');
INSERT INTO `dga_users` VALUES (291, 'CDH-宝信集群', NULL, NULL, NULL, NULL, 'INTERNAL', '2026-05-11 02:57:34.343000', 'OPENLDAP', '', 'dingquan', 0, 'User', '$2a$10$PsfERy.LatcmO2C8lEx2Eu3opK9aUSQQPiI.TCFGskW4nvVbgWC8a', NULL, 'dingquan');

-- ----------------------------
-- Table structure for meta_change_log
-- ----------------------------
DROP TABLE IF EXISTS `meta_change_log`;
CREATE TABLE `meta_change_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `object_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象类型: DATABASE, TABLE, COLUMN, PARTITION',
  `object_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象全名 (db.table 或 db.table.col)',
  `change_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变更类型: CREATE, ALTER, DROP, OWNER_CHANGE, COMMENT_CHANGE',
  `old_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '变更前内容 (JSON 格式)',
  `new_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '变更后内容 (JSON 格式)',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'SYSTEM_SYNC' COMMENT '操作人/触发源',
  `occur_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更发生时间 (源端估算)',
  `discover_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '平台发现时间',
  `is_processed` tinyint(4) NULL DEFAULT 0 COMMENT '是否已处理 (用于触发下游任务): 0-否, 1-是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_discover_time`(`discover_time`) USING BTREE,
  INDEX `idx_object_name`(`object_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '元数据变更审计日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_change_log
-- ----------------------------

-- ----------------------------
-- Table structure for meta_cluster_info
-- ----------------------------
DROP TABLE IF EXISTS `meta_cluster_info`;
CREATE TABLE `meta_cluster_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '集群唯一编码 (如: CDH_PROD_01)',
  `cluster_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '集群显示名称',
  `platform_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '平台类型: CDH, HDP, CDP, AWS_GLUE, ALIYUN_MAXCOMPUTE',
  `platform_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '平台版本 (如: CDH-6.3.2)',
  `hive_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Hive 组件版本 (如: 2.1.1-cdh6.3.2)',
  `metastore_uri` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Hive Metastore URI (thrift://...)',
  `jdbc_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '直连元数据库 JDBC URL (仅限采集器内部使用)',
  `db_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '元数据库用户名 (加密存储)',
  `db_password` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '元数据库密码 (加密存储)',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-停用, -1-异常',
  `sync_strategy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'API' COMMENT '同步策略: API (推荐), DIRECT_DB',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后一次成功同步时间',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'SYSTEM' COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注说明',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cluster_code`(`cluster_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '集群注册信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_cluster_info
-- ----------------------------

-- ----------------------------
-- Table structure for meta_column_info
-- ----------------------------
DROP TABLE IF EXISTS `meta_column_info`;
CREATE TABLE `meta_column_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `column_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `column_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_partition_key` bit(1) NULL DEFAULT NULL,
  `sort_order` int(11) NULL DEFAULT NULL,
  `data_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `comment_str` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_primary_key` bit(1) NULL DEFAULT NULL,
  `security_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_col_name_table`(`column_name`, `table_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_column_info
-- ----------------------------

-- ----------------------------
-- Table structure for meta_column_std
-- ----------------------------
DROP TABLE IF EXISTS `meta_column_std`;
CREATE TABLE `meta_column_std`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `table_id` bigint(20) NOT NULL COMMENT '关联表 ID (meta_table_std.id)',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID (冗余，便于分片)',
  `db_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据库名称 (冗余)',
  `table_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名称 (冗余)',
  `column_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字段名称',
  `column_type` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字段数据类型 (如: string, int, decimal(10,2))',
  `comment` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段注释/描述',
  `column_position` int(11) NOT NULL DEFAULT 0 COMMENT '字段顺序 (从 1 开始)',
  `is_partition_col` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否分区字段: 0-否, 1-是',
  `is_nullable` tinyint(4) NULL DEFAULT 1 COMMENT '是否可空: 0-否, 1-是 (高版本 Hive 支持)',
  `default_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '默认值 (高版本 Hive 支持)',
  `source_col_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源端列 ID',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地记录创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_table_col`(`table_id`, `column_name`) USING BTREE,
  INDEX `idx_table_id`(`table_id`) USING BTREE,
  INDEX `idx_col_name`(`column_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '标准化字段信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_column_std
-- ----------------------------

-- ----------------------------
-- Table structure for meta_database_std
-- ----------------------------
DROP TABLE IF EXISTS `meta_database_std`;
CREATE TABLE `meta_database_std`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID (meta_cluster_info.id)',
  `db_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据库名称',
  `owner_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库所有者',
  `owner_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'USER' COMMENT '所有者类型: USER, ROLE',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '数据库注释/描述',
  `location_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'HDFS 存储根路径',
  `parameters_json` json NULL COMMENT '扩展参数 (DB_PROPERTIES)',
  `source_db_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源端数据库 ID',
  `is_deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-否, 1-是',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '本地更新时间',
  `connection_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cluster_db`(`cluster_id`, `db_name`) USING BTREE,
  INDEX `idx_db_name`(`db_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '标准化数据库信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_database_std
-- ----------------------------

-- ----------------------------
-- Table structure for meta_enhanced_table
-- ----------------------------
DROP TABLE IF EXISTS `meta_enhanced_table`;
CREATE TABLE `meta_enhanced_table`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_id` bigint(20) NULL DEFAULT NULL,
  `column_count` int(11) NULL DEFAULT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `total_size` bigint(20) NULL DEFAULT NULL,
  `db_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `storage_format` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_access_time` datetime(6) NULL DEFAULT NULL,
  `location_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `original_table_id` bigint(20) NULL DEFAULT NULL,
  `owner_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `partition_count` int(11) NULL DEFAULT NULL,
  `record_count` bigint(20) NULL DEFAULT NULL,
  `sync_time` datetime(6) NULL DEFAULT NULL,
  `table_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `partition_keys` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_enhanced_table
-- ----------------------------

-- ----------------------------
-- Table structure for meta_partition_info
-- ----------------------------
DROP TABLE IF EXISTS `meta_partition_info`;
CREATE TABLE `meta_partition_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `datasource_id` bigint(20) NULL DEFAULT NULL,
  `db_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_access_time` datetime(6) NULL DEFAULT NULL,
  `last_modify_time` datetime(6) NULL DEFAULT NULL,
  `hdfs_path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `partition_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `partition_spec` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `record_count` bigint(20) NULL DEFAULT NULL,
  `storage_format` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sync_time` datetime(6) NULL DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_size` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_partition_info
-- ----------------------------

-- ----------------------------
-- Table structure for meta_partition_std
-- ----------------------------
DROP TABLE IF EXISTS `meta_partition_std`;
CREATE TABLE `meta_partition_std`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `table_id` bigint(20) NOT NULL COMMENT '关联表 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `db_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据库名称',
  `table_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名称',
  `partition_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分区名称 (如: dt=20231001/region=cn)',
  `partition_values` json NULL COMMENT '分区键值对解析',
  `location_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分区存储路径',
  `create_time` datetime NULL DEFAULT NULL COMMENT '分区创建时间',
  `last_access_time` datetime NULL DEFAULT NULL COMMENT '最后访问时间',
  `parameters_json` json NULL COMMENT '分区扩展参数',
  `data_size_bytes` bigint(20) NULL DEFAULT 0 COMMENT '分区数据大小',
  `row_count` bigint(20) NULL DEFAULT 0 COMMENT '分区行数',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_table_part`(`table_id`, `partition_name`) USING BTREE,
  INDEX `idx_table_id`(`table_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '标准化分区信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_partition_std
-- ----------------------------

-- ----------------------------
-- Table structure for meta_table_info
-- ----------------------------
DROP TABLE IF EXISTS `meta_table_info`;
CREATE TABLE `meta_table_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `datasource_id` bigint(20) NOT NULL,
  `db_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `file_count` bigint(20) NULL DEFAULT NULL,
  `governance_score` decimal(19, 2) NULL DEFAULT NULL,
  `is_partitioned` bit(1) NULL DEFAULT NULL,
  `lifecycle_days` int(11) NULL DEFAULT NULL,
  `location_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `total_size` bigint(20) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `last_access_time` datetime(6) NULL DEFAULT NULL,
  `lifecycle_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `hdfs_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner_source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `partition_count` bigint(20) NULL DEFAULT NULL,
  `record_count` bigint(20) NULL DEFAULT NULL,
  `source_owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `storage_format` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `datasource_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(6) NULL DEFAULT NULL,
  `dw_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sync_time` datetime(6) NULL DEFAULT NULL,
  `table_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `table_size` bigint(20) NULL DEFAULT NULL,
  `last_modify_time` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_meta_search_filters`(`datasource_id`, `db_name`(128), `owner`(64), `lifecycle_status`(32), `sync_time`) USING BTREE,
  INDEX `idx_meta_source_owner`(`source_owner`) USING BTREE,
  INDEX `idx_meta_table_name`(`table_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_table_info
-- ----------------------------

-- ----------------------------
-- Table structure for meta_table_std
-- ----------------------------
DROP TABLE IF EXISTS `meta_table_std`;
CREATE TABLE `meta_table_std`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `db_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据库名称',
  `table_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名称',
  `table_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表类型: MANAGED_TABLE, EXTERNAL_TABLE, VIRTUAL_VIEW, MATERIALIZED_VIEW',
  `format_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件格式: ORC, PARQUET, AVRO, TEXTFILE, SEQUENCEFILE',
  `input_format` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'InputFormat 类名',
  `output_format` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'OutputFormat 类名',
  `serde_class` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'SerDe 序列化类名',
  `location_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据存储路径',
  `create_time` datetime NULL DEFAULT NULL COMMENT '表创建时间',
  `last_ddl_time` datetime NULL DEFAULT NULL COMMENT '最后 DDL 变更时间',
  `retention` int(11) NULL DEFAULT 0 COMMENT '数据保留时间 (天)',
  `view_expanded_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '视图展开后的 SQL',
  `view_original_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '视图原始创建 SQL',
  `owner_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表所有者',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '表注释/描述',
  `is_partitioned` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否分区表: 0-否, 1-是',
  `partition_keys` json NULL COMMENT '分区字段列表',
  `parameters_json` json NULL COMMENT '扩展参数 (TBLPROPERTIES)',
  `source_tbl_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '源端表 ID',
  `data_size_bytes` bigint(20) NULL DEFAULT 0 COMMENT '数据总大小 (字节)',
  `row_count` bigint(20) NULL DEFAULT 0 COMMENT '预估行数',
  `is_deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-否, 1-是',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地记录创建时间',
  `update_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '本地记录更新时间',
  `partition_count` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cluster_db_tbl`(`cluster_id`, `db_name`, `table_name`) USING BTREE,
  INDEX `idx_db_table`(`db_name`, `table_name`) USING BTREE,
  INDEX `idx_format_type`(`format_type`) USING BTREE,
  INDEX `idx_last_ddl`(`last_ddl_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '标准化表信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of meta_table_std
-- ----------------------------

-- ----------------------------
-- Table structure for user_hive_access
-- ----------------------------
DROP TABLE IF EXISTS `user_hive_access`;
CREATE TABLE `user_hive_access`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `database_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `grant_time` datetime(6) NULL DEFAULT NULL,
  `granted_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT 0,
  `permission` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `revoke_time` datetime(6) NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1077 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_hive_access
-- ----------------------------
INSERT INTO `user_hive_access` VALUES (1, 'CDH-宝信集群', 'wgh_test', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'dingquan01');
INSERT INTO `user_hive_access` VALUES (2, 'HDP集群', 'xy_oms_wf', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', '*', NULL, 'liwenming');
INSERT INTO `user_hive_access` VALUES (3, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', '*', NULL, 'liwenming');
INSERT INTO `user_hive_access` VALUES (4, 'HDP集群', 'xy_algo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', '*', NULL, 'liwenming');
INSERT INTO `user_hive_access` VALUES (5, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'LOCK', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (6, 'HDP集群', '*', NULL, 'SYNC', 0, 'READ', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (7, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'WRITE', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (8, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'SERVICEADMIN', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (9, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'TEMPUDFADMIN', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (10, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (11, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'CREATE', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (12, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'READ', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (13, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'REPLADMIN', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (14, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'REFRESH', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (15, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'ALTER', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (16, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'INSERT', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (17, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'DROP', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (18, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (19, 'HDP集群', '*', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (20, 'HDP集群', 'xy_dev', NULL, 'SYNC', 0, 'INDEX', NULL, 'ACTIVE', '*', NULL, 'yanbinbin');
INSERT INTO `user_hive_access` VALUES (21, 'CDH-宝信集群', 'aggr_bill', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'dingquan_test');
INSERT INTO `user_hive_access` VALUES (22, 'CDH-宝信集群', 'wgh_test', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'dingquan_test');
INSERT INTO `user_hive_access` VALUES (23, 'HDP集群', '*', NULL, 'SYNC', 0, 'READ', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (24, 'HDP集群', 'test', NULL, 'SYNC', 0, 'CREATE', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (25, 'HDP集群', 'test', NULL, 'SYNC', 0, 'INDEX', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (26, 'HDP集群', '*', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (27, 'HDP集群', 'test', NULL, 'SYNC', 0, 'ALTER', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (28, 'HDP集群', 'test', NULL, 'SYNC', 0, 'INSERT', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (29, 'HDP集群', 'test', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (30, 'HDP集群', 'test', NULL, 'SYNC', 0, 'DROP', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (31, 'HDP集群', 'test', NULL, 'SYNC', 0, 'LOCK', NULL, 'ACTIVE', '*', NULL, 'bf_dingquan');
INSERT INTO `user_hive_access` VALUES (32, 'CDH-鹏博士', 'tradecenter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_tc_base', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (33, 'CDH-鹏博士', 'xy_dev', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (34, 'CDH-鹏博士', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (35, 'CDH-鹏博士', 'cardfanr', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (36, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_sms', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (37, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_code_cata', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (38, 'CDH-鹏博士', 'credit_dfp', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (39, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_detail', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (40, 'CDH-鹏博士', 'dpc_hn', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_member_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (41, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/credit_mining.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (42, 'CDH-鹏博士', 'credit_dfp_ip', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (43, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (44, 'CDH-鹏博士', 'xy_ods', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (45, 'CDH-鹏博士', 'xy_ods_db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (46, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_express_order_card', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (47, 'CDH-鹏博士', 'loan_trade', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (48, 'CDH-鹏博士', 'hdfs://ns1/user/alluxio/ha/', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (49, 'CDH-鹏博士', 'loan_cif', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (50, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_share', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (51, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_business_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (52, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/mandao_strategy.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (53, 'CDH-鹏博士', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dianwei_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (54, 'CDH-鹏博士', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_base_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (55, 'CDH-鹏博士', 'credit_decision_engine', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (56, 'CDH-鹏博士', 'loan_gatewat', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (57, 'CDH-鹏博士', 'mandao_strategy', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (58, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_bill', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (59, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_refund_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (60, 'CDH-鹏博士', 'xy_md_tpc', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (61, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_account_relationship', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (62, 'CDH-鹏博士', 'hdfs://ns1/user/xy_app_prd', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (63, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_info_whitelist', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (64, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dm.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (65, 'CDH-鹏博士', 'loan', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (66, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_member_crm', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (67, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_package_usage', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (68, 'CDH-鹏博士', 'spider_data', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (69, 'CDH-鹏博士', 'credit_center', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (70, 'CDH-鹏博士', 'xy_dw', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (71, 'CDH-鹏博士', 'xy_algo', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (72, 'CDH-鹏博士', 'xy_archive', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (73, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_jiangyuande.db', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (74, 'CDH-鹏博士', 'xy_app', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (75, 'CDH-鹏博士', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_order_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (76, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_express_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (77, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (78, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (79, 'CDH-鹏博士', 'credit_business', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (80, 'CDH-鹏博士', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_t_risk_list', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (81, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (82, 'CDH-鹏博士', 'xy_app_hive', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (83, 'CDH-鹏博士', 'xy_dm', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (84, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'entrust_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (85, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_data_dict', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (86, 'CDH-鹏博士', 'loan_manager', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (87, 'CDH-鹏博士', 'loan_config', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (88, 'CDH-鹏博士', 'xy_jiangyuande', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (89, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_ma_merchant_company', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (90, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'account_change_notify', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (91, 'CDH-鹏博士', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_channel_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (92, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_algo.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (93, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_crm', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (94, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_attach', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (95, 'CDH-鹏博士', 'dpc_xy', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_member_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (96, 'CDH-鹏博士', 'loan_repay', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (97, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_archive.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (98, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_01', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (99, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/spider_data.db', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (100, 'CDH-鹏博士', 'tradecenter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_tc_channel_msg', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (101, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_recharge', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (102, 'CDH-鹏博士', 'gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_acq_agentpay', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (103, 'CDH-鹏博士', 'xy_oms_wf', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (104, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_attach', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (105, 'CDH-鹏博士', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (106, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_bind_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (107, 'CDH-鹏博士', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_liangdian_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (108, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_net', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (109, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_details', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (110, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_groud', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (111, 'CDH-鹏博士', 'gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_acq_fee', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (112, 'CDH-鹏博士', 'dpc_hn', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_product_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (113, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (114, 'CDH-鹏博士', 'xy_linsanji', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (115, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_paid_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (116, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_refund_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (117, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (118, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_oms_wf.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (119, 'CDH-鹏博士', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_name_list', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (120, 'CDH-鹏博士', 'dpc_xy', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_product_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (121, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dev.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (122, 'CDH-鹏博士', 'xx_dispose', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (123, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_order_refund_serial', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (124, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (125, 'CDH-鹏博士', 'credit_gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (126, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_voice_call', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (127, 'CDH-鹏博士', 'loan_bill', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (128, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'quick_pay_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (129, 'CDH-鹏博士', 'decision_model', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (130, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_entrust_sign_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (131, 'CDH-鹏博士', 'twingo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_nucc_farms_discipline_name', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (132, 'CDH-鹏博士', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_basic', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (133, 'CDH-鹏博士', 'credit_mining', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (134, 'CDH-宝信集群', 'tradecenter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_tc_base', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (135, 'CDH-宝信集群', 'xy_dev', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (136, 'CDH-宝信集群', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (137, 'CDH-宝信集群', 'cardfanr', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (138, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_sms', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (139, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_code_cata', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (140, 'CDH-宝信集群', 'credit_dfp', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (141, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_detail', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (142, 'CDH-宝信集群', 'dpc_hn', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_member_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (143, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/credit_mining.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (144, 'CDH-宝信集群', 'credit_dfp_ip', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (145, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (146, 'CDH-宝信集群', 'xy_ods', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (147, 'CDH-宝信集群', 'xy_ods_db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (148, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_express_order_card', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (149, 'CDH-宝信集群', 'loan_trade', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (150, 'CDH-宝信集群', 'hdfs://ns1/user/alluxio/ha/', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (151, 'CDH-宝信集群', 'loan_cif', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (152, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_share', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (153, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_business_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (154, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/mandao_strategy.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (155, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dianwei_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (156, 'CDH-宝信集群', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_base_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (157, 'CDH-宝信集群', 'credit_decision_engine', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (158, 'CDH-宝信集群', 'loan_gatewat', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (159, 'CDH-宝信集群', 'mandao_strategy', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (160, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_bill', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (161, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_refund_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (162, 'CDH-宝信集群', 'xy_md_tpc', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (163, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_account_relationship', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (164, 'CDH-宝信集群', 'hdfs://ns1/user/xy_app_prd', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (165, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_info_whitelist', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (166, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dm.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (167, 'CDH-宝信集群', 'loan', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (168, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_member_crm', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (169, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_package_usage', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (170, 'CDH-宝信集群', 'spider_data', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (171, 'CDH-宝信集群', 'credit_center', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (172, 'CDH-宝信集群', 'xy_dw', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (173, 'CDH-宝信集群', 'xy_algo', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (174, 'CDH-宝信集群', 'xy_archive', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (175, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_jiangyuande.db', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (176, 'CDH-宝信集群', 'xy_app', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (177, 'CDH-宝信集群', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_order_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (178, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_express_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (179, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (180, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (181, 'CDH-宝信集群', 'credit_business', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (182, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_t_risk_list', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (183, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (184, 'CDH-宝信集群', 'xy_app_hive', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (185, 'CDH-宝信集群', 'xy_dm', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (186, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'entrust_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (187, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_data_dict', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (188, 'CDH-宝信集群', 'loan_manager', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (189, 'CDH-宝信集群', 'loan_config', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (190, 'CDH-宝信集群', 'xy_jiangyuande', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (191, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_ma_merchant_company', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (192, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'account_change_notify', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (193, 'CDH-宝信集群', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_channel_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (194, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_algo.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (195, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_crm', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (196, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_attach', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (197, 'CDH-宝信集群', 'dpc_xy', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_member_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (198, 'CDH-宝信集群', 'loan_repay', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (199, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_archive.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (200, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_01', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (201, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/spider_data.db', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (202, 'CDH-宝信集群', 'tradecenter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_tc_channel_msg', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (203, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_recharge', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (204, 'CDH-宝信集群', 'gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_acq_agentpay', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (205, 'CDH-宝信集群', 'xy_oms_wf', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (206, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_attach', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (207, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (208, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_bind_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (209, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_liangdian_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (210, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_net', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (211, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_details', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (212, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_groud', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (213, 'CDH-宝信集群', 'gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_acq_fee', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (214, 'CDH-宝信集群', 'dpc_hn', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_product_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (215, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (216, 'CDH-宝信集群', 'xy_linsanji', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (217, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_paid_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (218, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_refund_trans', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (219, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (220, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_oms_wf.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (221, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_name_list', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (222, 'CDH-宝信集群', 'dpc_xy', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_credit_product_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (223, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dev.db', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (224, 'CDH-宝信集群', 'xx_dispose', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (225, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_order_refund_serial', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (226, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_order', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (227, 'CDH-宝信集群', 'credit_gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (228, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_voice_call', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (229, 'CDH-宝信集群', 'loan_bill', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (230, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'quick_pay_bind_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (231, 'CDH-宝信集群', 'decision_model', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (232, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_entrust_sign_info', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (233, 'CDH-宝信集群', 'twingo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_nucc_farms_discipline_name', NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (234, 'CDH-宝信集群', 'hdfs://ns1/user/hive/warehouse/xy_dw.db/dw_spider_carrier_basic', NULL, 'SYNC', 0, '*', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (235, 'CDH-宝信集群', 'credit_mining', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (236, 'HDP集群', '*', NULL, 'SYNC', 0, 'LOCK', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (237, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (238, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (239, 'HDP集群', '*', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (240, 'HDP集群', '*', NULL, 'SYNC', 0, 'REPLADMIN', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (241, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'REPLADMIN', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (242, 'HDP集群', '*', NULL, 'SYNC', 0, 'ALTER', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (243, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'SERVICEADMIN', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (244, 'HDP集群', '*', NULL, 'SYNC', 0, 'DROP', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (245, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'READ', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (246, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'TEMPUDFADMIN', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (247, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'ALTER', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (248, 'HDP集群', '*', NULL, 'SYNC', 0, 'REFRESH', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (249, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'DROP', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (250, 'HDP集群', '*', NULL, 'SYNC', 0, 'READ', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (251, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'INSERT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (252, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'LOCK', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (253, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'REFRESH', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (254, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'WRITE', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (255, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'INDEX', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (256, 'HDP集群', 'ALL DATABASES', NULL, 'SYNC', 0, 'CREATE', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (257, 'HDP集群', '*', NULL, 'SYNC', 0, 'TEMPUDFADMIN', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (258, 'HDP集群', '*', NULL, 'SYNC', 0, 'WRITE', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (259, 'HDP集群', '*', NULL, 'SYNC', 0, 'SERVICEADMIN', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (260, 'HDP集群', '*', NULL, 'SYNC', 0, 'CREATE', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (261, 'HDP集群', '*', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (262, 'HDP集群', '*', NULL, 'SYNC', 0, 'INDEX', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (263, 'HDP集群', '*', NULL, 'SYNC', 0, 'INSERT', NULL, 'ACTIVE', NULL, NULL, 'xy_app_spark');
INSERT INTO `user_hive_access` VALUES (264, 'CDH-宝信集群', 'agent', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'dingquan');
INSERT INTO `user_hive_access` VALUES (265, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_guazhang_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (266, 'CDH-宝信集群', 'pay_aggregate', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_day_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (267, 'CDH-宝信集群', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_merchant_account_book_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (268, 'CDH-宝信集群', 'aggr_pay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (269, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_seller_commission_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (270, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_info_addition', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (271, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fo_channel_balance_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (272, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_seller_commission_monthly', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (273, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_brand_owner', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (274, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_group_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (275, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_no_ownsplit_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (276, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_2018_1203', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (277, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_pay_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (278, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfo_refund', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (279, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_role_user', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (280, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company_partner', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (281, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_attach_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (282, 'CDH-宝信集群', 'pay_share', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (283, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_refund_settle_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (284, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (285, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (286, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'merge_pay_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (287, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_bind_fee_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (288, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'member_union_pay_up', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (289, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_role_target', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (290, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_login_user', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (291, 'CDH-宝信集群', 'pay_gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_risk_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (292, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_plug', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (293, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_extend_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (294, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_details_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (295, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_model', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (296, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_subject_balance_asy', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (297, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_village_trade_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (298, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_business_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (299, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_payment_batch', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (300, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_rate', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (301, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_bal_internal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (302, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (303, 'CDH-宝信集群', 'baofoo_rm_aml', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (304, 'CDH-宝信集群', 'baofoo_schoot', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cd_25_tc_ff', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (305, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_time', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (306, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_split_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (307, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_b2cbank_order_addition', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (308, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (309, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_entrust_payment_channel_config_pt', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (310, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_daily_fi_financial_cooperation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (311, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_csd_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (312, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfo_transfer', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (313, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_settle_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (314, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_order_refund', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (315, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (316, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_dir_counter', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (317, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_acc_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (318, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_order_freezed', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (319, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_order_customer_fee', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (320, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_order_info_new', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (321, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_service_merchant_manager', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (322, 'CDH-宝信集群', 'baofu_rm_aml', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_trade_blacklist', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (323, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_product_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (324, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_2023_08', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (325, 'CDH-宝信集群', 'yqt_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (326, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_market_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (327, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_deal_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (328, 'CDH-宝信集群', 'cloud_billing', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (329, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (330, 'CDH-宝信集群', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_mail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (331, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_error_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (332, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_fail_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (333, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'trade_commission', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (334, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_member_crm', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (335, 'CDH-宝信集群', 'bf_oc', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (336, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_quick_pay', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (337, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_bank', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (338, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_in_out_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (339, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_no_own_split_strategy', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (340, 'CDH-宝信集群', 'baofu_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_channel_rate', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (341, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_r_order_master_v5_yqy', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (342, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company_background_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (343, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_b2cbank_order_vertical_addition', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (344, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (345, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_refund_original_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (346, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_prod', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (347, 'CDH-宝信集群', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_questionable_member_list', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (348, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_daily_balance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (349, 'CDH-宝信集群', 'mandao_crm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (350, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_seller_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (351, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_r_order_master_v5', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (352, 'CDH-宝信集群', 'baofoo_cdp', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cdp_agreement_member_relation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (353, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (354, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_income_pay_member_relation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (355, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (356, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_scan_pay_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (357, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_refund_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (358, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_channel_fee_verify_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (359, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_group', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (360, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_auth_verify_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (361, 'CDH-宝信集群', 'pay_payrouter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (362, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_balance_real', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (363, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_auto_settle_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (364, 'CDH-宝信集群', 'mongo_baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_update_member_state', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (365, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_single_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (366, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_acs_account_order_detail_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (367, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_individual', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (368, 'CDH-宝信集群', 'md_customer_card', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (369, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment_dealcode', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (370, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_role', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (371, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (372, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_fochannel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (373, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_member_temp', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (374, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (375, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_platform_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (376, 'CDH-宝信集群', 'cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (377, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_analyze', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (378, 'CDH-宝信集群', 'baofu_icpay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (379, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (380, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_link', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (381, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (382, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_flow', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (383, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (384, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_share_relationship_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (385, 'CDH-宝信集群', 'pay_tradecenter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (386, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_today', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (387, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_detail_open', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (388, 'CDH-宝信集群', 'cloud_member', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (389, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (390, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_s_account', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (391, 'CDH-宝信集群', 'cloud_account', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (392, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_attribute', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (393, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_detail_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (394, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_special_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (395, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_seller_split', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (396, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_industry_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (397, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (398, 'CDH-宝信集群', 'pay_agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (399, 'CDH-宝信集群', 'default', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (400, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_gear_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (401, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_refund_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (402, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_daily_balance_17', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (403, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (404, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_member_overdue_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (405, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_indy_check_4', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (406, 'CDH-宝信集群', 'md_account_book', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (407, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_bulk_finance_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (408, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_details_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (409, 'CDH-宝信集群', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_sms_sum', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (410, 'CDH-宝信集群', 'cloud_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (411, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_order_refund_serial', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (412, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (413, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_company_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (414, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_trade', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (415, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_fee', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (416, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_merchant_category', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (417, 'CDH-宝信集群', 'mongo_mandao_ops', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (418, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_seller_daily_import', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (419, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_entry_asy_ma', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (420, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_psis_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (421, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_tran_ba', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (422, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment_classification', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (423, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_pt_card_library', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (424, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_fengjiang', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (425, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_fail_fee_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (426, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_quick_pass_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (427, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'gwfi_b2c_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (428, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_pay', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (429, 'CDH-宝信集群', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (430, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product_show', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (431, 'CDH-宝信集群', 'agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (432, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_plugin_group', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (433, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_2018_1112', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (434, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product_show_new', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (435, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_target', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (436, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_income_cost_statistics', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (437, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cb_ma_additional_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (438, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_auth_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (439, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (440, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_merchant_label', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (441, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_strategy_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (442, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_entrust_payment_channel_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (443, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_holiday', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (444, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_department', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (445, 'CDH-宝信集群', 'payrouter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (446, 'CDH-宝信集群', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_person_account_book_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (447, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_open_finance_acc_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (448, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_member_scale', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (449, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_extend', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (450, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_agreement', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (451, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_item_model', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (452, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_bank_key', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (453, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_p_relation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (454, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company_arch', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (455, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_product_with_transtype', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (456, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (457, 'CDH-宝信集群', 'baofoo_bi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (458, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_share', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (459, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_business_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (460, 'CDH-宝信集群', 'credit_gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_serve_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (461, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_agent_ralate', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (462, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (463, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_business_data_20250821', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (464, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (465, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_acct_trade', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (466, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_split_accounts_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (467, 'CDH-宝信集群', 'baofu_cbca', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (468, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_01_bak', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (469, 'CDH-宝信集群', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (470, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'batchpay_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (471, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (472, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_terminal_prod', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (473, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_batch_order_file', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (474, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_payment_voucher_file', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (475, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_pay_business_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (476, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_off_line_recharge', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (477, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_agent_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (478, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_daily_fi_deal_and_fandian', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (479, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_prod_func', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (480, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_baozhangtong_member_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (481, 'CDH-宝信集群', 'baofoo_fo_finance', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (482, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (483, 'CDH-宝信集群', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_sms', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (484, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_individual', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (485, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_account_relationship', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (486, 'CDH-宝信集群', 'cloud_verify', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (487, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'union_pay_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (488, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_role_item', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (489, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_trade_commission', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (490, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_2022', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (491, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_settle_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (492, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_village_growth_rate_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (493, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_account_merchant_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (494, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_bank_recharge', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (495, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_refund_settle_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (496, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_b2cbank_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (497, 'CDH-宝信集群', 'baofu_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_channel_fixed_rate_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (498, 'CDH-宝信集群', 'pay591', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (499, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal_fandian', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (500, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'customer_overdue_20191231', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (501, 'CDH-宝信集群', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_order_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (502, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_express_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (503, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_order_refund', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (504, 'CDH-宝信集群', 'baofoo_psis', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (505, 'CDH-宝信集群', 'baofoo_important_reports', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (506, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_payment_batch_sub', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (507, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_attach_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (508, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_settle_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (509, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_bank', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (510, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_business_data', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (511, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_balance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (512, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_outdate_id', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (513, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_s_person', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (514, 'CDH-宝信集群', 'oracle', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (515, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_s_other_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (516, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_terminal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (517, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (518, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_data_dict', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (519, 'CDH-宝信集群', 'aggr_channel', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'channel_refund_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (520, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (521, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_deal_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (522, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (523, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_trade_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (524, 'CDH-宝信集群', 'md_account_book', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_acct_bal_detail_customer', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (525, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_subject_balance_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (526, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_card_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (527, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_rate_function_for_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (528, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_indy_check_3', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (529, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (530, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_fee_ba', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (531, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_fee_d', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (532, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_psis_owned_fund_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (533, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_report_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (534, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfo_transfer_20170830', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (535, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_ma_merchant_company', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (536, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_balance_ht', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (537, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_crm', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (538, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_merchant_report_modify_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (539, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_ipo_industry', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (540, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_sec_natural_person', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (541, 'CDH-宝信集群', 'mongo_baofoo_rm_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (542, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_bank_bin_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (543, 'CDH-宝信集群', 'bf_zhangxuntao', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (544, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_agrt_express_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (545, 'CDH-宝信集群', 'baofoo_schoot', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_problem_member_freeze_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (546, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_card_serial', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (547, 'CDH-宝信集群', 'mongo_baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (548, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_member_diff_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (549, 'CDH-宝信集群', 'baofoo_rm_v1', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'rm_member_bind_card_fo', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (550, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_no_ownsplit_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (551, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_dir_channel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (552, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_acc_show', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (553, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_merchant_label_relevance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (554, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_dir_sell', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (555, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal_auth', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (556, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (557, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (558, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_rule', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (559, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_zone', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (560, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_settle_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (561, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product_20190811', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (562, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_groud', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (563, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_fichannel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (564, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'merchant_notify_command', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (565, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_card_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (566, 'CDH-宝信集群', 'billtone_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (567, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_industry', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (568, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_paid_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (569, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_split_accounts_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (570, 'CDH-宝信集群', 'baofoo_report', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'r_fo_order_master_v5', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (571, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal_dual', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (572, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_refund_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (573, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_special_batch', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (574, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_code_sub', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (575, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_terminal_ips', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (576, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_sign_page', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (577, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_acc_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (578, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_large_product', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (579, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_2019_0401', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (580, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_name_list', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (581, 'CDH-宝信集群', 'cloud_product', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (582, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_auth_bind_transaction', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (583, 'CDH-宝信集群', 'zxt_test', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (584, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_strategy_fochannel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (585, 'CDH-宝信集群', 'pay_oms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_data_dict', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (586, 'CDH-宝信集群', 'baofu_cbpay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (587, 'CDH-宝信集群', 'aggr_channel', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'channel_pay_order_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (588, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_business_data_20250523', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (589, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (590, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_daily_balance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (591, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (592, 'CDH-宝信集群', 'pay_benefit', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (593, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_operate_system', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (594, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_item', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (595, 'CDH-宝信集群', 'baofu_crm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (596, 'CDH-宝信集群', 'pay_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (597, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_entrust_sign_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (598, 'CDH-宝信集群', 'aggr_bill', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (599, 'CDH-宝信集群', 'md_agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (600, 'CDH-宝信集群', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (601, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_shareholders_beneficiary', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (602, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_guazhang_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (603, 'CDH-鹏博士', 'pay_aggregate', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_day_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (604, 'CDH-鹏博士', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_merchant_account_book_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (605, 'CDH-鹏博士', 'aggr_pay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (606, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_seller_commission_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (607, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_info_addition', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (608, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fo_channel_balance_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (609, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_seller_commission_monthly', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (610, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_brand_owner', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (611, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_group_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (612, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_no_ownsplit_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (613, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_2018_1203', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (614, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_pay_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (615, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfo_refund', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (616, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_role_user', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (617, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company_partner', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (618, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_attach_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (619, 'CDH-鹏博士', 'pay_share', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (620, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_refund_settle_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (621, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (622, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (623, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'merge_pay_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (624, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_bind_fee_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (625, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'member_union_pay_up', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (626, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_role_target', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (627, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_login_user', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (628, 'CDH-鹏博士', 'pay_gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_risk_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (629, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_plug', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (630, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_extend_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (631, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_details_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (632, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_model', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (633, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_subject_balance_asy', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (634, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_village_trade_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (635, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_business_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (636, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_payment_batch', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (637, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_rate', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (638, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_bal_internal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (639, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (640, 'CDH-鹏博士', 'baofoo_rm_aml', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (641, 'CDH-鹏博士', 'baofoo_schoot', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cd_25_tc_ff', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (642, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_time', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (643, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_split_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (644, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_b2cbank_order_addition', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (645, 'CDH-鹏博士', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (646, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_entrust_payment_channel_config_pt', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (647, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_daily_fi_financial_cooperation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (648, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_csd_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (649, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfo_transfer', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (650, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_settle_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (651, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_order_refund', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (652, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (653, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_dir_counter', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (654, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_acc_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (655, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_order_freezed', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (656, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_order_customer_fee', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (657, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_order_info_new', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (658, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_service_merchant_manager', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (659, 'CDH-鹏博士', 'baofu_rm_aml', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_trade_blacklist', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (660, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_product_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (661, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_2023_08', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (662, 'CDH-鹏博士', 'yqt_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (663, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_market_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (664, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_deal_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (665, 'CDH-鹏博士', 'cloud_billing', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (666, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (667, 'CDH-鹏博士', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_mail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (668, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_error_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (669, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_fail_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (670, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'trade_commission', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (671, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_member_crm', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (672, 'CDH-鹏博士', 'bf_oc', NULL, 'SYNC', 0, 'ALL', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (673, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_quick_pay', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (674, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_bank', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (675, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_in_out_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (676, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_no_own_split_strategy', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (677, 'CDH-鹏博士', 'baofu_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_channel_rate', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (678, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_r_order_master_v5_yqy', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (679, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company_background_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (680, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_b2cbank_order_vertical_addition', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (681, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (682, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_refund_original_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (683, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_prod', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (684, 'CDH-鹏博士', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_questionable_member_list', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (685, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_daily_balance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (686, 'CDH-鹏博士', 'mandao_crm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (687, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_seller_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (688, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_r_order_master_v5', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (689, 'CDH-鹏博士', 'baofoo_cdp', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cdp_agreement_member_relation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (690, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (691, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_income_pay_member_relation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (692, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (693, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_scan_pay_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (694, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_refund_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (695, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_channel_fee_verify_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (696, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_group', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (697, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_auth_verify_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (698, 'CDH-鹏博士', 'pay_payrouter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (699, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_balance_real', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (700, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_auto_settle_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (701, 'CDH-鹏博士', 'mongo_baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_update_member_state', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (702, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_single_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (703, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_acs_account_order_detail_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (704, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_individual', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (705, 'CDH-鹏博士', 'md_customer_card', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (706, 'CDH-鹏博士', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment_dealcode', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (707, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_role', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (708, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (709, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_fochannel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (710, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_member_temp', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (711, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (712, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_platform_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (713, 'CDH-鹏博士', 'cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (714, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_analyze', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (715, 'CDH-鹏博士', 'baofu_icpay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (716, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (717, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_link', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (718, 'CDH-鹏博士', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (719, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_flow', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (720, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (721, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_share_relationship_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (722, 'CDH-鹏博士', 'pay_tradecenter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (723, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_today', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (724, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_detail_open', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (725, 'CDH-鹏博士', 'cloud_member', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (726, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (727, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_s_account', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (728, 'CDH-鹏博士', 'cloud_account', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (729, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_attribute', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (730, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_detail_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (731, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_special_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (732, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_seller_split', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (733, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_industry_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (734, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (735, 'CDH-鹏博士', 'pay_agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (736, 'CDH-鹏博士', 'default', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (737, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_gear_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (738, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_refund_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (739, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_daily_balance_17', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (740, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (741, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_member_overdue_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (742, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_indy_check_4', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (743, 'CDH-鹏博士', 'md_account_book', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (744, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_bulk_finance_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (745, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_order_refund_details_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (746, 'CDH-鹏博士', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_sms_sum', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (747, 'CDH-鹏博士', 'cloud_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (748, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_order_refund_serial', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (749, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (750, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_company_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (751, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_trade', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (752, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_fee', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (753, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_merchant_category', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (754, 'CDH-鹏博士', 'mongo_mandao_ops', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (755, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_seller_daily_import', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (756, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_entry_asy_ma', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (757, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_psis_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (758, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_tran_ba', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (759, 'CDH-鹏博士', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment_classification', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (760, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_pt_card_library', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (761, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_fengjiang', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (762, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_fail_fee_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (763, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_quick_pass_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (764, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'gwfi_b2c_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (765, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_pay', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (766, 'CDH-鹏博士', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (767, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product_show', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (768, 'CDH-鹏博士', 'agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (769, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_plugin_group', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (770, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_2018_1112', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (771, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product_show_new', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (772, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_target', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (773, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_income_cost_statistics', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (774, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cb_ma_additional_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (775, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_auth_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (776, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (777, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_merchant_label', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (778, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_strategy_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (779, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_entrust_payment_channel_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (780, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_holiday', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (781, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_department', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (782, 'CDH-鹏博士', 'payrouter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (783, 'CDH-鹏博士', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_person_account_book_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (784, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_open_finance_acc_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (785, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_member_scale', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (786, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_extend', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (787, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_agreement', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (788, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_item_model', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (789, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_bank_key', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (790, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_p_relation', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (791, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_company_arch', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (792, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_product_with_transtype', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (793, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (794, 'CDH-鹏博士', 'baofoo_bi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (795, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_share', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (796, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_payment_business_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (797, 'CDH-鹏博士', 'credit_gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_serve_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (798, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_agent_ralate', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (799, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (800, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_business_data_20250821', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (801, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (802, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_acct_trade', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (803, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_split_accounts_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (804, 'CDH-鹏博士', 'baofu_cbca', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (805, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_pay_order_01_bak', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (806, 'CDH-鹏博士', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (807, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'batchpay_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (808, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_member', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (809, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_terminal_prod', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (810, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_batch_order_file', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (811, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_payment_voucher_file', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (812, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_pay_business_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (813, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_off_line_recharge', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (814, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_agent_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (815, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_daily_fi_deal_and_fandian', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (816, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_prod_func', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (817, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_baozhangtong_member_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (818, 'CDH-鹏博士', 'baofoo_fo_finance', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (819, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (820, 'CDH-鹏博士', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_sms', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (821, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_individual', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (822, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_account_relationship', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (823, 'CDH-鹏博士', 'cloud_verify', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (824, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'union_pay_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (825, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_role_item', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (826, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_trade_commission', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (827, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal_2022', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (828, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_settle_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (829, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_village_growth_rate_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (830, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_account_merchant_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (831, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_bank_recharge', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (832, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_refund_settle_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (833, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_b2cbank_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (834, 'CDH-鹏博士', 'baofu_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_channel_fixed_rate_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (835, 'CDH-鹏博士', 'pay591', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (836, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal_fandian', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (837, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'customer_overdue_20191231', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (838, 'CDH-鹏博士', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_c_order_trans', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (839, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_express_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (840, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_order_refund', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (841, 'CDH-鹏博士', 'baofoo_psis', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (842, 'CDH-鹏博士', 'baofoo_important_reports', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (843, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_payment_batch_sub', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (844, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_attach_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (845, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_settle_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (846, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_bank', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (847, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_business_data', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (848, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_balance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (849, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_outdate_id', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (850, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_s_person', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (851, 'CDH-鹏博士', 'oracle', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (852, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_s_other_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (853, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_terminal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (854, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (855, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_data_dict', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (856, 'CDH-鹏博士', 'aggr_channel', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'channel_refund_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (857, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (858, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_deal_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (859, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (860, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_trade_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (861, 'CDH-鹏博士', 'md_account_book', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_acct_bal_detail_customer', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (862, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_subject_balance_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (863, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_card_type', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (864, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_split_rate_function_for_report', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (865, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_indy_check_3', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (866, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (867, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_fee_ba', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (868, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_commission_fee_d', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (869, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_psis_owned_fund_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (870, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_report_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (871, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfo_transfer_20170830', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (872, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'v_ma_merchant_company', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (873, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_account_balance_ht', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (874, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_crm', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (875, 'CDH-鹏博士', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_merchant_report_modify_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (876, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_ipo_industry', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (877, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_sec_natural_person', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (878, 'CDH-鹏博士', 'mongo_baofoo_rm_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (879, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_bank_bin_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (880, 'CDH-鹏博士', 'bf_zhangxuntao', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (881, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_agrt_express_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (882, 'CDH-鹏博士', 'baofoo_schoot', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_problem_member_freeze_config', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (883, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_card_serial', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (884, 'CDH-鹏博士', 'mongo_baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (885, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_member_diff_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (886, 'CDH-鹏博士', 'baofoo_rm_v1', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'rm_member_bind_card_fo', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (887, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_no_ownsplit_daily', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (888, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_dir_channel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (889, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_acc_show', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (890, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_merchant_label_relevance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (891, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_dir_sell', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (892, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal_auth', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (893, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_share_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (894, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_merchant', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (895, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_settle_rule', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (896, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_zone', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (897, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_settle_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (898, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_product_20190811', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (899, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_bind_groud', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (900, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_rule_fichannel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (901, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'merchant_notify_command', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (902, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_card_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (903, 'CDH-鹏博士', 'billtone_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (904, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_industry', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (905, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_paid_order', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (906, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_split_accounts_detail', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (907, 'CDH-鹏博士', 'baofoo_report', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'r_fo_order_master_v5', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (908, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal_dual', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (909, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_cloud_share_refund_order_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (910, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_special_batch', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (911, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_code_sub', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (912, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_terminal_ips', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (913, 'CDH-鹏博士', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'protocol_sign_page', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (914, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_acc_code', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (915, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_dir_large_product', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (916, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_member_2019_0401', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (917, 'CDH-鹏博士', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_name_list', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (918, 'CDH-鹏博士', 'cloud_product', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (919, 'CDH-鹏博士', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgwfi_auth_bind_transaction', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (920, 'CDH-鹏博士', 'zxt_test', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (921, 'CDH-鹏博士', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fee_strategy_fochannel', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (922, 'CDH-鹏博士', 'pay_oms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_data_dict', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (923, 'CDH-鹏博士', 'baofu_cbpay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (924, 'CDH-鹏博士', 'aggr_channel', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'channel_pay_order_01', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (925, 'CDH-鹏博士', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_business_data_20250523', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (926, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'admin_function', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (927, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_acc_daily_balance', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (928, 'CDH-鹏博士', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (929, 'CDH-鹏博士', 'pay_benefit', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (930, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_operate_system', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (931, 'CDH-鹏博士', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'drilling_item', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (932, 'CDH-鹏博士', 'baofu_crm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (933, 'CDH-鹏博士', 'pay_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (934, 'CDH-鹏博士', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_gw_entrust_sign_info', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (935, 'CDH-鹏博士', 'aggr_bill', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (936, 'CDH-鹏博士', 'md_agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (937, 'CDH-鹏博士', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event_record', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (938, 'CDH-鹏博士', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_shareholders_beneficiary', NULL, 'bf_oc');
INSERT INTO `user_hive_access` VALUES (939, 'CDH-宝信集群', 'baofoo_rm_aml', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (940, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_info', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (941, 'CDH-宝信集群', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_s_settle_acs_association', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (942, 'CDH-宝信集群', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_merchant_account', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (943, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cgw_acs_bank_notice_detail', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (944, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_01', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (945, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_split_accounts_serial', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (946, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (947, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_share_trans_detail', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (948, 'CDH-宝信集群', 'baof0o_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ma_merchant_individua', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (949, 'CDH-宝信集群', 'baofoo_rm_regulator', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (950, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_split_accounts_detail', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (951, 'CDH-宝信集群', 'baofoo_rm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (952, 'CDH-宝信集群', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_s_settle_collect_info_detail', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (953, 'CDH-宝信集群', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (954, 'CDH-宝信集群', 'aggr_pay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fi_unify_pay_order_01', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (955, 'CDH-宝信集群', 'mongo_baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_update_member_state', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (956, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (957, 'CDH-宝信集群', 'baofoo_hadoop_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (958, 'CDH-宝信集群', 'baofoo_rm_v1', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (959, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'aggr_share_order_details_01', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (960, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (961, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (962, 'CDH-宝信集群', 'baofoo_bos', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'bos_payment_approval', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (963, 'CDH-宝信集群', 'baofoo_hadoop', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (964, 'CDH-宝信集群', 'baofoo_report', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_all_transfer_order', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (965, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (966, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cutpayment_split_accounts_info', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (967, 'CDH-宝信集群', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_person_account', NULL, 'bf_rm');
INSERT INTO `user_hive_access` VALUES (968, 'CDH-宝信集群', 'cloud_billing', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_split_function', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (969, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (970, 'CDH-宝信集群', 'zxt_test', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'user_message', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (971, 'CDH-宝信集群', 'yqy_clear', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (972, 'CDH-宝信集群', 'cloud_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_acc_order_v2', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (973, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (974, 'CDH-宝信集群', 'baofoo_boas', NULL, 'SYNC', 0, 'INSERT', NULL, 'ACTIVE', 'special_member_account_detail_result_1119', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (975, 'CDH-宝信集群', 'cloud_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_clear_order_v2', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (976, 'CDH-宝信集群', 'cloud_billing', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_split_rate', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (977, 'CDH-宝信集群', 'payrouter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (978, 'CDH-宝信集群', 'baofoo_bv_config', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'subject_attribute', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (979, 'CDH-宝信集群', 'tradecenter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (980, 'CDH-宝信集群', 'baofoo_custody', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (981, 'CDH-宝信集群', 'cbpay_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (982, 'CDH-宝信集群', 'baofoo_boas', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (983, 'CDH-宝信集群', 'baofoo_report', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (984, 'CDH-宝信集群', 'baofoo_bi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (985, 'CDH-宝信集群', 'baofoo_psis', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'psis_balance_member_detail_daily', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (986, 'CDH-宝信集群', 'baofoo_restat', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (987, 'CDH-宝信集群', 'yqt_bill', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (988, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (989, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (990, 'CDH-宝信集群', 'baofoo_admin', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (991, 'CDH-宝信集群', 'cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (992, 'CDH-宝信集群', 'bd_dwd', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'bi_01_dir_holiday_bfj', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (993, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (994, 'CDH-宝信集群', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (995, 'CDH-宝信集群', 'cloud_member', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (996, 'CDH-宝信集群', 'baofoo_treasure', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (997, 'CDH-宝信集群', 'cloud_account', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (998, 'CDH-宝信集群', 'baofoo_verify', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (999, 'CDH-宝信集群', 'baofoo_bv_report', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1000, 'CDH-宝信集群', 'billtone_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1001, 'CDH-宝信集群', 'baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_order_freezed', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1002, 'CDH-宝信集群', 'default', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1003, 'CDH-宝信集群', 'yqt_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1004, 'CDH-宝信集群', 'baofoo_fo_finance', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1005, 'CDH-宝信集群', 'bf_gongshaojie', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1006, 'CDH-宝信集群', 'cloud_billing', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1007, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1008, 'CDH-宝信集群', 'baofoo_rm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1009, 'CDH-宝信集群', 'md_account_book', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1010, 'CDH-宝信集群', 'cloud_product', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1011, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1012, 'CDH-宝信集群', 'credit_gateway', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1013, 'CDH-宝信集群', 'tmp', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1014, 'CDH-宝信集群', 'baofu_cbpay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1015, 'CDH-宝信集群', 'baofoo_boas', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'special_member_account_detail', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1016, 'CDH-宝信集群', 'cloud_billing', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_no_ownsplit_order', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1017, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1018, 'CDH-宝信集群', 'baofoo_boas', NULL, 'SYNC', 0, 'INSERT', NULL, 'ACTIVE', 'special_member_account_detail_bt', NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1019, 'CDH-宝信集群', 'md_agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1020, 'CDH-宝信集群', 'baofoo_psis', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_qjs');
INSERT INTO `user_hive_access` VALUES (1021, 'CDH-宝信集群', 'baofoo_cutpayment', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1022, 'CDH-宝信集群', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_mail', NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1023, 'CDH-宝信集群', 'baofoo_ps', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'ps_notice_mail_bak20230713', NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1024, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1025, 'CDH-宝信集群', 'mongo_baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_update_member_state', NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1026, 'CDH-宝信集群', 'baofoo_ma', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1027, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment', NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1028, 'CDH-宝信集群', 'baofoo_cm', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_fi_deal', NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1029, 'CDH-宝信集群', 'aggr_channel', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'channel_pay_order_01', NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1030, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1031, 'CDH-宝信集群', 'yqt_bill', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1032, 'CDH-宝信集群', 'baofoo_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1033, 'CDH-宝信集群', 'yqt_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1034, 'CDH-宝信集群', 'baofoo_cm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'cm_deal', NULL, 'bf_cp');
INSERT INTO `user_hive_access` VALUES (1035, 'CDH-宝信集群', 'baofoo_important_reports', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1036, 'CDH-宝信集群', 'cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1037, 'CDH-宝信集群', 'aggr_prod', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1038, 'CDH-宝信集群', 'md_customer', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1039, 'CDH-宝信集群', 'aggr_pay', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1040, 'CDH-宝信集群', 'baofoo_fo', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'fo_payment', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1041, 'CDH-宝信集群', 'baofoo_rm_frms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1042, 'CDH-宝信集群', 'baofoo_rm_v2', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_dispute_complaint_risk_event', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1043, 'CDH-宝信集群', 'agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1044, 'CDH-宝信集群', 'baofoo_boas', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'special_member_account_detail_result_1119', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1045, 'CDH-宝信集群', 'cloud_member', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1046, 'CDH-宝信集群', 'cloud_account', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1047, 'CDH-宝信集群', 'baofoo_restat', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'baofoo_stat_cm_deal_detail', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1048, 'CDH-宝信集群', 'billtone_core', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1049, 'CDH-宝信集群', 'billton_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_customer_ht_account_info_ref', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1050, 'CDH-宝信集群', 'baofoo_verify', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_verify_transfer_bank_detail_two', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1051, 'CDH-宝信集群', 'baofoo_fi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1052, 'CDH-宝信集群', 'billton_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_customer_org', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1053, 'CDH-宝信集群', 'billtone_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1054, 'CDH-宝信集群', 'cloud_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_clear_order_v2', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1055, 'CDH-宝信集群', 'yqt_trade', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1056, 'CDH-宝信集群', 'bf_gongshaojie', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1057, 'CDH-宝信集群', 'cloud_billing', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1058, 'CDH-宝信集群', 'payrouter', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1059, 'CDH-宝信集群', 'md_account_book', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1060, 'CDH-宝信集群', 'cloud_product', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1061, 'CDH-宝信集群', 'mongo_baofoo_log', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'log_update_member_state', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1062, 'CDH-宝信集群', 'baofoo_boas', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'special_member_account_detail_result', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1063, 'CDH-宝信集群', 'billton_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_customer_tc_org_apply', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1064, 'CDH-宝信集群', 'pay_oms', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_data_dict', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1065, 'CDH-宝信集群', 'aggr_channel', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'channel_pay_order_01', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1066, 'CDH-宝信集群', 'baofoo_boas', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'special_member_account_detail', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1067, 'CDH-宝信集群', 'billton_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_customer_account_info_ref', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1068, 'CDH-宝信集群', 'pay_benefit', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1069, 'CDH-宝信集群', 'baofoo_bi', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1070, 'CDH-宝信集群', 'baofu_cgw', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_channel_fixed_rate_detail', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1071, 'CDH-宝信集群', 'yqt_bill', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1072, 'CDH-宝信集群', 'aggr_bill', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1073, 'CDH-宝信集群', 'md_agreement', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1074, 'CDH-宝信集群', 'billton_cif', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 't_customer_contract', NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1075, 'CDH-宝信集群', 'baofoo_psis', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', NULL, NULL, 'bf_app_admaster');
INSERT INTO `user_hive_access` VALUES (1076, 'CDH-宝信集群', 'baofoo_restat', NULL, 'SYNC', 0, 'SELECT', NULL, 'ACTIVE', 'baofoo_stat_cm_deal_detail_his', NULL, 'bf_app_admaster');

-- ----------------------------
-- Table structure for user_resource_access
-- ----------------------------
DROP TABLE IF EXISTS `user_resource_access`;
CREATE TABLE `user_resource_access`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auth_backend` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cluster_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `database_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `engine_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `grant_time` datetime(6) NULL DEFAULT NULL,
  `granted_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT 0,
  `permission` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `revoke_time` datetime(6) NULL DEFAULT NULL,
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `update_time` datetime(6) NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `collaborator_owners` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_reviewed_at` datetime(6) NULL DEFAULT NULL,
  `reviewed_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `review_due_at` datetime(6) NULL DEFAULT NULL,
  `revoked_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_resource_owner`(`owner`, `status`) USING BTREE,
  INDEX `idx_user_resource_review_due`(`review_due_at`, `status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_resource_access
-- ----------------------------
INSERT INTO `user_resource_access` VALUES (1, 'SENTRY', 'CDH_BX', 'CDH_BX', 'wgh_test', 'HIVE', '2026-04-28 03:52:33.900000', 'admin', 0, 'ALL', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'dingquan_test', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (2, 'SENTRY', 'CDH_BX', 'CDH_BX', 'wgh_test', 'HIVE', '2026-04-28 05:58:39.056000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'dingquan01', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (3, 'RANGER', 'HDP', 'HDP', 'test', 'HIVE', '2026-04-28 06:09:37.562000', 'admin', 0, 'ALL', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'bf_dingquan', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (4, 'RANGER', 'HDP', 'HDP集群', 'xy_dev', 'HIVE', '2026-04-29 06:51:09.679000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (5, 'RANGER', 'HDP', 'HDP集群', 'xy_algo', 'HIVE', '2026-04-29 06:51:09.791000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (6, 'RANGER', 'HDP', 'HDP集群', 'xy_oms_wf', 'HIVE', '2026-04-29 06:51:09.905000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (7, 'RANGER', 'HDP', 'HDP', 'xy_dev', 'HIVE', '2026-04-29 07:14:49.207000', 'admin', 0, 'INSERT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (8, 'RANGER', 'HDP', 'HDP', 'xy_oms_wf', 'HIVE', '2026-04-29 07:14:49.326000', 'admin', 0, 'INSERT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (9, 'RANGER', 'HDP', 'HDP', 'xy_algo', 'HIVE', '2026-04-29 07:14:49.442000', 'admin', 0, 'INSERT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (10, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'aggr_bill', 'HIVE', '2026-04-29 07:20:03.595000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'dingquan_test', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (11, 'RANGER', 'HDP', 'HDP', 'xy_ods', 'HIVE', '2026-04-29 09:33:37.434000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (12, 'RANGER', 'HDP', 'HDP', 'xy_dw', 'HIVE', '2026-04-29 09:33:37.572000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (13, 'RANGER', 'HDP', 'HDP', 'xy_dm', 'HIVE', '2026-04-29 09:33:37.674000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (14, 'RANGER', 'HDP', 'HDP', 'iceberg_dm', 'HIVE', '2026-04-29 09:53:29.879000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (15, 'RANGER', 'HDP', 'HDP', 'iceberg_dw', 'HIVE', '2026-04-29 09:53:29.950000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (16, 'RANGER', 'HDP', 'HDP', 'iceberg_ods', 'HIVE', '2026-04-29 09:53:30.027000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (17, 'RANGER', 'HDP', 'HDP', 'xy_algo', 'HIVE', '2026-04-29 09:53:30.116000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (18, 'RANGER', 'HDP', 'HDP', 'xx_dispose', 'HIVE', '2026-04-29 09:53:30.188000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (19, 'RANGER', 'HDP', 'HDP', 'xy_dm', 'HIVE', '2026-04-29 09:53:30.260000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (20, 'RANGER', 'HDP', 'HDP', 'xy_dw', 'HIVE', '2026-04-29 09:53:30.328000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (21, 'RANGER', 'HDP', 'HDP', 'xy_ods', 'HIVE', '2026-04-29 09:53:30.404000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (22, 'RANGER', 'HDP', 'HDP', 'xy_oms_wf', 'HIVE', '2026-04-29 09:53:30.493000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (23, 'RANGER', 'HDP', 'HDP', 'yqy_clear', 'HIVE', '2026-04-29 09:53:30.574000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'yanbinbin', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (24, 'RANGER', 'HDP', 'HDP', 'iceberg_dm', 'HIVE', '2026-04-30 01:51:19.753000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (25, 'RANGER', 'HDP', 'HDP', 'iceberg_dw', 'HIVE', '2026-04-30 01:51:19.862000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (26, 'RANGER', 'HDP', 'HDP', 'iceberg_ods', 'HIVE', '2026-04-30 01:51:19.975000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (27, 'RANGER', 'HDP', 'HDP', 'xy_algo', 'HIVE', '2026-04-30 02:34:26.654000', 'admin', 0, 'CREATE', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (28, 'RANGER', 'HDP', 'HDP', 'xy_algo', 'HIVE', '2026-04-30 02:35:47.132000', 'admin', 0, 'CREATE', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'liwenming', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (29, 'RANGER', 'HDP', 'HDP', 'baofoo_cutpayment', 'HIVE', '2026-05-09 08:31:48.991000', 'admin', 1, 'ALL', 'TABLE', '2026-05-09 16:40:08.000000', 'AUTHORIZATION_CENTER', 'REVOKED', 'cutpayment_share_trans_info', NULL, 'bf_dingquan', NULL, NULL, NULL, NULL, NULL, 'admin');
INSERT INTO `user_resource_access` VALUES (30, 'RANGER', 'HDP', 'HDP', 'baofoo_cutpayment', 'HIVE', NULL, 'admin', 1, 'ALL', 'DATABASE', '2026-05-09 08:39:44.835000', 'AUTHORIZATION_CENTER', 'REVOKED', NULL, NULL, 'bf_dingquan', NULL, NULL, NULL, NULL, NULL, 'admin');
INSERT INTO `user_resource_access` VALUES (31, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'agent', 'HIVE', '2026-05-11 02:58:40.808000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'dingquan', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (32, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_rm_aml', 'HIVE', '2026-05-12 02:26:14.302000', 'zhangxuntao', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'bf_oc', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (33, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-14 07:30:53.863000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_payment_order_checklist_hgc', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (34, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-14 07:30:55.879000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_merchant_dim_day', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (35, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-14 07:30:57.763000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_rule_day_detail', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (36, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-14 07:30:59.911000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_rule_group_day', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (37, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-14 07:31:01.954000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_trade_member_day', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (38, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-14 07:31:03.912000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_trade_member_reason_day', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (39, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-14 07:31:05.690000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_trade_rule_wide', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (40, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-15 03:07:50.294000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_trade_member_day', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (41, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_hadoop', 'HIVE', '2026-05-15 03:07:52.319000', 'zhangxuntao', 0, 'ALL', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_rm_dashboard_trade_member_reason_day', NULL, 'bf_rm', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (42, 'SENTRY', 'CDH_BX', 'CDH_BX', 'cloud_trade', 'HIVE', '2026-05-15 03:17:06.347000', 'admin', 0, 'SELECT', 'TABLE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', 't_acc_order_v2', NULL, 'bf_qjs', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (43, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_split', 'HIVE', '2026-05-19 09:18:54.620000', 'zhangxuntao', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'bf_qjs', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (44, 'SENTRY', 'CDH_BX', 'CDH-宝信集群', 'baofoo_split', 'HIVE', '2026-05-20 05:54:28.173000', 'zhangxuntao', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'bf_cp', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `user_resource_access` VALUES (45, 'SENTRY', 'CDH_BX', 'CDH_BX', 'baofoo_split', 'HIVE', '2026-05-20 10:00:48.797000', 'admin', 0, 'SELECT', 'DATABASE', NULL, 'AUTHORIZATION_CENTER', 'ACTIVE', NULL, NULL, 'bf_app_admaster', NULL, NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auth_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime(6) NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_admin` tinyint(1) NULL DEFAULT 0,
  `last_login_time` datetime(6) NULL DEFAULT NULL,
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `provider` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` tinyint(1) NULL DEFAULT 1,
  `update_time` datetime(6) NULL DEFAULT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_r43af9ap4edm43mmtq01oddj6`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'local', NULL, '2026-04-28 06:19:56.035000', '123@163.com', 0, NULL, NULL, 'dingquan User', NULL, '123', NULL, 1, '2026-04-28 06:19:56.035000', 'dingquan');
INSERT INTO `users` VALUES (2, 'local', NULL, '2026-05-07 09:53:01.523000', '123@163.com', 0, '2026-05-21 09:29:46.891000', NULL, 'admin User', NULL, '$2a$10$BMEuPWEMrkey.SfY7sxV5Oc794Hp.38aupHW.jSd/JgpUcG5oZHQm', NULL, 1, '2026-05-21 09:29:46.893000', 'admin');
INSERT INTO `users` VALUES (3, 'local', NULL, '2026-05-12 02:21:09.067000', 'afanti@baofu.com', 0, '2026-05-20 05:53:27.936000', NULL, 'zhangxuntao User', NULL, '$2a$10$Wp7JIb4QxwpiMgYr0F.0tOa5RQ4rrvKTAkx0XVEfQ75nHvTL5JXE.', NULL, 1, '2026-05-20 05:53:27.937000', 'zhangxuntao');

SET FOREIGN_KEY_CHECKS = 1;
