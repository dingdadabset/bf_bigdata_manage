package com.dga.quality.service;

import com.dga.access.security.CurrentUser;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.metadata.entity.ColumnMetadata;
import com.dga.metadata.entity.PartitionMetadata;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.ColumnMetadataRepository;
import com.dga.metadata.repository.PartitionMetadataRepository;
import com.dga.metadata.repository.TableMetadataRepository;
import com.dga.quality.entity.QualityExecution;
import com.dga.quality.entity.QualityIssue;
import com.dga.quality.entity.QualityRule;
import com.dga.quality.repository.QualityExecutionRepository;
import com.dga.quality.repository.QualityIssueRepository;
import com.dga.quality.repository.QualityRuleRepository;
import com.dga.settings.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.criteria.Predicate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class QualityExecutionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String ISSUE_OPEN = "OPEN";
    private static final String ISSUE_RESOLVED = "RESOLVED";

    @Autowired
    private QualityRuleRepository ruleRepository;

    @Autowired
    private QualityExecutionRepository executionRepository;

    @Autowired
    private QualityIssueRepository issueRepository;

    @Autowired
    private TableMetadataRepository tableRepository;

    @Autowired
    private ColumnMetadataRepository columnRepository;

    @Autowired
    private PartitionMetadataRepository partitionRepository;

    @Autowired
    private DataSourceConfigRepository dataSourceRepository;

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private NotificationService notificationService;

    public List<QualityRule> findRules(Long dataSourceId, Long tableId, String status) {
        return ruleRepository.findAll((Specification<QualityRule>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (dataSourceId != null) {
                predicates.add(cb.equal(root.get("dataSourceId"), dataSourceId));
            }
            if (tableId != null) {
                predicates.add(cb.equal(root.get("tableId"), tableId));
            }
            if (!isBlank(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            query.orderBy(cb.desc(root.get("updatedAt")), cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    @Transactional
    public QualityRule createRule(QualityRule request) {
        request.setId(null);
        normalizeRule(request);
        request.setCreatedBy(CurrentUser.usernameOrUnknown());
        return ruleRepository.save(request);
    }

    @Transactional
    public QualityRule updateRule(Long id, QualityRule request) {
        QualityRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "质量规则不存在: " + id));
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setTableId(request.getTableId());
        rule.setColumnName(request.getColumnName());
        rule.setSeverity(request.getSeverity());
        rule.setStatus(request.getStatus());
        rule.setScanScope(request.getScanScope());
        rule.setThreshold(request.getThreshold());
        rule.setExpectedValue(request.getExpectedValue());
        rule.setMinValue(request.getMinValue());
        rule.setMaxValue(request.getMaxValue());
        rule.setRegexPattern(request.getRegexPattern());
        rule.setActionType(request.getActionType());
        rule.setOwner(request.getOwner());
        normalizeRule(rule);
        return ruleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    @Transactional
    public QualityExecution executeRule(Long ruleId) {
        QualityRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "质量规则不存在: " + ruleId));
        if (!STATUS_ACTIVE.equals(rule.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "质量规则未启用，无法执行");
        }
        TableMetadata table = tableRepository.findById(rule.getTableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "元数据表不存在: " + rule.getTableId()));

        QualityExecution execution = new QualityExecution();
        execution.setRuleId(rule.getId());
        execution.setTableId(table.getId());
        execution.setThreshold(effectiveThreshold(rule));
        execution.setScanScope(effectiveScanScope(rule));
        execution.setExecutedBy(CurrentUser.usernameOrUnknown());
        execution.setExecutedAt(LocalDateTime.now());
        long start = System.currentTimeMillis();

        try {
            QualitySql qualitySql = buildSql(rule, table);
            execution.setScanFilter(qualitySql.scanFilter);
            execution.setExecutedSql(qualitySql.sql);

            Double result = qualitySql.resultValue;
            if (result == null) {
                result = executeMetricSql(table, qualitySql.sql);
            }
            execution.setResultValue(result);
            execution.setStatus(isPassed(rule, result) ? STATUS_SUCCESS : STATUS_FAILED);
        } catch (Exception e) {
            execution.setStatus(STATUS_FAILED);
            execution.setErrorMessage(message(e));
        } finally {
            execution.setDurationMs(System.currentTimeMillis() - start);
        }

        QualityExecution saved = executionRepository.save(execution);
        updateRuleAfterExecution(rule, saved);
        updateIssue(rule, table, saved);
        return saved;
    }

    @Transactional
    public List<QualityExecution> executeTable(Long tableId) {
        List<QualityExecution> executions = new ArrayList<>();
        for (QualityRule rule : findRules(null, tableId, STATUS_ACTIVE)) {
            executions.add(executeRule(rule.getId()));
        }
        return executions;
    }

    private void normalizeRule(QualityRule rule) {
        TableMetadata table = tableRepository.findById(rule.getTableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的元数据表"));
        rule.setDataSourceId(table.getDataSourceId());
        rule.setDbName(table.getDbName());
        rule.setTableName(table.getTableName());
        rule.setRuleType(upperOrDefault(rule.getRuleType(), "NULL_RATE"));
        rule.setSeverity(upperOrDefault(rule.getSeverity(), "MEDIUM"));
        rule.setStatus(upperOrDefault(rule.getStatus(), STATUS_ACTIVE));
        rule.setScanScope(upperOrDefault(rule.getScanScope(), "LATEST_PARTITION"));
        rule.setActionType(upperOrDefault(rule.getActionType(), "ALARM"));
        if (rule.getThreshold() == null && requiresFailureRate(rule.getRuleType())) {
            rule.setThreshold(0.0);
        }
        if (isBlank(rule.getRuleName())) {
            rule.setRuleName(defaultRuleName(rule, table));
        } else {
            rule.setRuleName(rule.getRuleName().trim());
        }
        if (isBlank(rule.getOwner())) {
            rule.setOwner(firstNonBlank(table.getOwner(), table.getSourceOwner()));
        } else {
            rule.setOwner(rule.getOwner().trim());
        }
        if (requiresColumn(rule.getRuleType())) {
            validateColumn(rule.getTableId(), rule.getColumnName());
        } else {
            rule.setColumnName(null);
        }
        if ("VALUE_RANGE".equals(rule.getRuleType()) && rule.getMinValue() == null && rule.getMaxValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数值范围规则至少需要填写最小值或最大值");
        }
        if ("REGEX_MATCH".equals(rule.getRuleType()) && isBlank(rule.getRegexPattern())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正则匹配规则需要填写正则表达式");
        }
        if (("ROW_COUNT".equals(rule.getRuleType()) || "FRESHNESS".equals(rule.getRuleType())) && isBlank(rule.getExpectedValue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "行数或新鲜度规则需要填写期望值");
        }
    }

    private void validateColumn(Long tableId, String columnName) {
        if (isBlank(columnName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该规则需要选择字段");
        }
        for (ColumnMetadata column : columnRepository.findByTableId(tableId)) {
            if (columnName.trim().equalsIgnoreCase(column.getColumnName())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "字段不存在: " + columnName);
    }

    private QualitySql buildSql(QualityRule rule, TableMetadata table) {
        if ("FRESHNESS".equals(rule.getRuleType())) {
            LocalDateTime latest = latestDataTime(table);
            double hours = latest == null ? Double.MAX_VALUE : Duration.between(latest, LocalDateTime.now()).toMinutes() / 60.0;
            return new QualitySql(null, null, hours);
        }

        String tableExpr = quoteIdent(table.getDbName()) + "." + quoteIdent(table.getTableName());
        String scanFilter = buildLatestPartitionFilter(rule, table);
        String where = isBlank(scanFilter) ? "" : " WHERE " + scanFilter;
        String column = quoteIdent(rule.getColumnName());
        String sql;

        switch (rule.getRuleType()) {
            case "NULL_RATE":
                sql = "SELECT CASE WHEN COUNT(1)=0 THEN 0.0 ELSE "
                        + "SUM(CASE WHEN " + column + " IS NULL THEN 1 ELSE 0 END) / COUNT(1) END AS metric "
                        + "FROM " + tableExpr + where;
                break;
            case "UNIQUE_RATE":
                sql = "SELECT CASE WHEN COUNT(1)=0 THEN 0.0 ELSE "
                        + "(COUNT(1) - COUNT(DISTINCT " + column + ")) / COUNT(1) END AS metric "
                        + "FROM " + tableExpr + where;
                break;
            case "VALUE_RANGE":
                sql = "SELECT CASE WHEN COUNT(1)=0 THEN 0.0 ELSE "
                        + "SUM(CASE WHEN " + rangeFailureExpression(column, rule) + " THEN 1 ELSE 0 END) / COUNT(1) END AS metric "
                        + "FROM " + tableExpr + where;
                break;
            case "ROW_COUNT":
                sql = "SELECT COUNT(1) AS metric FROM " + tableExpr + where;
                break;
            case "REGEX_MATCH":
                sql = "SELECT CASE WHEN COUNT(1)=0 THEN 0.0 ELSE "
                        + "SUM(CASE WHEN " + column + " IS NOT NULL AND NOT (" + column + " RLIKE " + stringLiteral(rule.getRegexPattern()) + ") THEN 1 ELSE 0 END) / COUNT(1) END AS metric "
                        + "FROM " + tableExpr + where;
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的规则类型: " + rule.getRuleType());
        }
        return new QualitySql(sql, scanFilter, null);
    }

    private Double executeMetricSql(TableMetadata table, String sql) throws Exception {
        ClusterEndpoint endpoint = resolveHiveServer2Endpoint(table);
        Class.forName("org.apache.hive.jdbc.HiveDriver");
        DriverManager.setLoginTimeout(30);
        try (Connection connection = DriverManager.getConnection(
                endpoint.getUrl(),
                nullToEmpty(endpoint.getUsername()),
                nullToEmpty(endpoint.getPassword()));
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (!rs.next()) {
                return null;
            }
            return rs.getDouble(1);
        }
    }

    private ClusterEndpoint resolveHiveServer2Endpoint(TableMetadata table) {
        String clusterCode = table.getClusterCode();
        if (isBlank(clusterCode) && table.getDataSourceId() != null) {
            Optional<DataSourceConfig> dataSource = dataSourceRepository.findById(table.getDataSourceId());
            if (dataSource.isPresent()) {
                clusterCode = dataSource.get().getClusterCode();
            }
        }
        if (isBlank(clusterCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前表缺少集群编码，无法定位 HIVE_SERVER2 端点");
        }
        List<ClusterEndpoint> endpoints = endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                clusterCode, ClusterEndpoint.TYPE_HIVE_SERVER2, STATUS_ACTIVE);
        if (endpoints.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "集群未配置 ACTIVE HIVE_SERVER2 端点: " + clusterCode);
        }
        return endpoints.get(0);
    }

    private String buildLatestPartitionFilter(QualityRule rule, TableMetadata table) {
        if (!"LATEST_PARTITION".equals(effectiveScanScope(rule))) {
            return null;
        }
        if (table.getPartitionCount() == null || table.getPartitionCount() <= 0) {
            return null;
        }
        List<PartitionMetadata> partitions = partitionRepository.findByTableIdOrderByLastModifyTimeDescIdDesc(table.getId());
        if (partitions.isEmpty() || isBlank(partitions.get(0).getPartitionSpec())) {
            return null;
        }
        String[] parts = partitions.get(0).getPartitionSpec().split("/");
        List<String> filters = new ArrayList<>();
        for (String part : parts) {
            int index = part.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = part.substring(0, index);
            String value = part.substring(index + 1);
            if ("__HIVE_DEFAULT_PARTITION__".equals(value)) {
                filters.add(quoteIdent(key) + " IS NULL");
            } else {
                filters.add(quoteIdent(key) + " = " + stringLiteral(value));
            }
        }
        return filters.isEmpty() ? null : String.join(" AND ", filters);
    }

    private LocalDateTime latestDataTime(TableMetadata table) {
        List<PartitionMetadata> partitions = partitionRepository.findByTableIdOrderByLastModifyTimeDescIdDesc(table.getId());
        if (!partitions.isEmpty() && partitions.get(0).getLastModifyTime() != null) {
            return partitions.get(0).getLastModifyTime();
        }
        if (table.getSyncTime() != null) {
            return table.getSyncTime();
        }
        return table.getUpdatedAt();
    }

    private boolean isPassed(QualityRule rule, Double result) {
        if (result == null) {
            return false;
        }
        if ("ROW_COUNT".equals(rule.getRuleType())) {
            return result >= parseDouble(rule.getExpectedValue(), 0.0);
        }
        if ("FRESHNESS".equals(rule.getRuleType())) {
            return result <= parseDouble(rule.getExpectedValue(), 24.0);
        }
        return result <= effectiveThreshold(rule);
    }

    private void updateRuleAfterExecution(QualityRule rule, QualityExecution execution) {
        rule.setLastExecutionStatus(execution.getStatus());
        rule.setLastResultValue(execution.getResultValue());
        rule.setLastErrorMessage(execution.getErrorMessage());
        rule.setLastExecutedAt(execution.getExecutedAt());
        ruleRepository.save(rule);
    }

    private void updateIssue(QualityRule rule, TableMetadata table, QualityExecution execution) {
        if (STATUS_SUCCESS.equals(execution.getStatus())) {
            Optional<QualityIssue> issue = issueRepository.findFirstByRuleIdAndStatusOrderByLastSeenAtDesc(rule.getId(), ISSUE_OPEN);
            if (issue.isPresent()) {
                QualityIssue item = issue.get();
                item.setStatus(ISSUE_RESOLVED);
                item.setResolvedAt(LocalDateTime.now());
                item.setLastExecutionId(execution.getId());
                issueRepository.save(item);
            }
            return;
        }

        QualityIssue issue = issueRepository.findFirstByRuleIdAndStatusOrderByLastSeenAtDesc(rule.getId(), ISSUE_OPEN)
                .orElseGet(QualityIssue::new);
        issue.setRuleId(rule.getId());
        issue.setTableId(table.getId());
        issue.setIssueTitle(rule.getRuleName());
        issue.setIssueDescription(issueDescription(rule, table, execution));
        issue.setStatus(ISSUE_OPEN);
        issue.setSeverity(rule.getSeverity());
        issue.setOwner(rule.getOwner());
        issue.setResultValue(execution.getResultValue());
        issue.setThreshold(execution.getThreshold());
        issue.setLastExecutionId(execution.getId());
        issue.setLastSeenAt(LocalDateTime.now());
        issueRepository.save(issue);
        notificationService.notifyQualityIssue(issue.getIssueDescription());
    }

    private String issueDescription(QualityRule rule, TableMetadata table, QualityExecution execution) {
        String target = table.getDbName() + "." + table.getTableName();
        if (!isBlank(rule.getColumnName())) {
            target += "." + rule.getColumnName();
        }
        if (!isBlank(execution.getErrorMessage())) {
            return target + " 质量规则执行失败: " + execution.getErrorMessage();
        }
        return target + " 未通过质量规则 " + rule.getRuleType()
                + "，结果值=" + execution.getResultValue()
                + "，阈值/期望=" + thresholdText(rule);
    }

    private String rangeFailureExpression(String column, QualityRule rule) {
        List<String> parts = new ArrayList<>();
        if (rule.getMinValue() != null) {
            parts.add(column + " < " + rule.getMinValue());
        }
        if (rule.getMaxValue() != null) {
            parts.add(column + " > " + rule.getMaxValue());
        }
        return column + " IS NOT NULL AND (" + String.join(" OR ", parts) + ")";
    }

    private boolean requiresColumn(String ruleType) {
        return "NULL_RATE".equals(ruleType)
                || "UNIQUE_RATE".equals(ruleType)
                || "VALUE_RANGE".equals(ruleType)
                || "REGEX_MATCH".equals(ruleType);
    }

    private boolean requiresFailureRate(String ruleType) {
        return "NULL_RATE".equals(ruleType)
                || "UNIQUE_RATE".equals(ruleType)
                || "VALUE_RANGE".equals(ruleType)
                || "REGEX_MATCH".equals(ruleType);
    }

    private double effectiveThreshold(QualityRule rule) {
        return rule.getThreshold() == null ? 0.0 : rule.getThreshold();
    }

    private String effectiveScanScope(QualityRule rule) {
        return isBlank(rule.getScanScope()) ? "LATEST_PARTITION" : rule.getScanScope();
    }

    private String defaultRuleName(QualityRule rule, TableMetadata table) {
        String name = table.getDbName() + "." + table.getTableName();
        if (!isBlank(rule.getColumnName())) {
            name += "." + rule.getColumnName();
        }
        return name + " " + rule.getRuleType();
    }

    private String thresholdText(QualityRule rule) {
        if ("ROW_COUNT".equals(rule.getRuleType()) || "FRESHNESS".equals(rule.getRuleType())) {
            return rule.getExpectedValue();
        }
        return String.valueOf(effectiveThreshold(rule));
    }

    private String quoteIdent(String value) {
        if (value == null) {
            return "``";
        }
        return "`" + value.replace("`", "``") + "`";
    }

    private String stringLiteral(String value) {
        return "'" + nullToEmpty(value).replace("'", "''") + "'";
    }

    private String upperOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return isBlank(value) ? defaultValue : Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        return isBlank(second) ? null : second.trim();
    }

    private String message(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class QualitySql {
        private final String sql;
        private final String scanFilter;
        private final Double resultValue;

        private QualitySql(String sql, String scanFilter, Double resultValue) {
            this.sql = sql;
            this.scanFilter = scanFilter;
            this.resultValue = resultValue;
        }
    }
}
