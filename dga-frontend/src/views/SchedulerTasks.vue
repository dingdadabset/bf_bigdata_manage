<template>
  <div class="scheduler-view">
    <div class="module-header">
      <div>
        <h1>调度任务管理</h1>
        <p>以集群为一级分类查看调度器接入、任务归属、运行健康与资产关联</p>
      </div>
      <div class="header-actions">
        <a-select
          v-model="selectedClusterCode"
          placeholder="选择集群"
          style="width: 180px"
          :loading="loadingClusters"
          @change="onClusterChange"
        >
          <a-select-option v-for="cluster in clusterOptions" :key="cluster.value" :value="cluster.value">
            {{ cluster.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-model="selectedSchedulerType"
          placeholder="调度器类型"
          style="width: 180px"
          :loading="loadingClusters"
          @change="onSchedulerTypeChange"
        >
          <a-select-option v-for="item in schedulerTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-model="selectedEndpointKey"
          placeholder="选择调度端点"
          style="width: 280px"
          :loading="loadingClusters"
          @change="onEndpointChange"
        >
          <a-select-option value="ALL">全部端点</a-select-option>
          <a-select-opt-group v-for="group in groupedEndpoints" :key="group.type" :label="group.label">
            <a-select-option v-for="endpoint in group.options" :key="endpoint.key" :value="endpoint.key">
              {{ endpoint.label }}
            </a-select-option>
          </a-select-opt-group>
        </a-select>
        <a-button icon="reload" :loading="loadingOverview || loadingTasks || loadingGroups" @click="refreshAll">刷新</a-button>
      </div>
    </div>

    <a-alert
      v-if="!clusterOptions.length && !loadingClusters"
      type="warning"
      show-icon
      message="未发现 ACTIVE 调度端点"
      description="请先在环境资源注册中为集群配置 AZKABAN_DB 或 DOLPHINSCHEDULER_DB 端点。"
      style="margin-bottom: 16px"
    />

    <a-alert
      v-if="showUnsupportedAlert"
      type="info"
      show-icon
      message="当前集群包含尚未接入明细查询的调度器端点"
      description="本页已按集群组织入口；Azkaban 任务明细可查看，DolphinScheduler 端点先展示分组与入口。"
      style="margin-bottom: 16px"
    />

    <div v-if="selectedCluster" class="context-strip">
      <span class="context-title">{{ selectedCluster.clusterName || selectedCluster.clusterCode }}</span>
      <span class="context-meta">调度器：{{ visibleSchedulerTypesLabel }}</span>
      <span class="context-meta">端点：{{ visibleEndpoints.length }}</span>
      <span class="context-meta">当前视图：{{ currentScopeLabel }}</span>
      <span v-if="overview.generatedAt" class="context-meta">快照：{{ formatTime(overview.generatedAt) }}</span>
    </div>

    <div class="summary-row">
      <div class="summary-item">
        <span>任务数</span>
        <strong>{{ overview.totalTasks || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>项目/流程</span>
        <strong>{{ overview.totalProjects || 0 }} / {{ overview.totalFlows || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>失败任务</span>
        <strong class="danger-text">{{ overview.failedTasks || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>运行中</span>
        <strong>{{ overview.runningTasks || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>未归属</span>
        <strong class="warning-text">{{ overview.unownedTasks || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>活跃资产任务</span>
        <strong>{{ overview.activeAssetTasks || 0 }}</strong>
      </div>
    </div>

    <a-tabs :active-key="activeTab" @change="activeTab = $event">
      <a-tab-pane key="dashboard" tab="健康看板">
        <section class="panel trend-panel">
          <div class="panel-title">任务执行趋势</div>
          <div class="panel-hint">按近 7 天展示成功率折线与失败率面积图；点击某一天可联动下方高风险流程和任务</div>
          <div class="trend-toolbar">
            <a-tag v-if="selectedTrendDay" color="blue">已选择：{{ selectedTrendDay }}</a-tag>
            <a-button v-if="selectedTrendDay" type="link" size="small" @click="clearTrendSelection">返回近 7 天</a-button>
          </div>
          <div ref="trendChart" class="trend-chart"></div>
        </section>

        <div class="dashboard-grid">
          <section class="panel">
            <div class="panel-title">最近活跃流程</div>
            <div class="panel-hint">按最近开始时间倒序，仅展示最新 10 条流程运行记录</div>
            <a-table
              size="small"
              row-key="rowKey"
              :columns="runColumns"
              :data-source="overview.recentRuns || []"
              :pagination="false"
              :loading="loadingOverview"
            >
              <template slot="flow" slot-scope="text, record">
                <div class="primary-text">{{ record.projectName }} / {{ record.flowName }}</div>
                <div class="muted-text">{{ record.clusterName || record.clusterCode }} · {{ record.schedulerLabel }} · 提交人：{{ record.submitUser || '-' }}</div>
                <div class="muted-text">耗时：{{ formatDuration(record.durationMs) }}</div>
              </template>
              <template slot="status" slot-scope="text">
                <a-tag :color="statusColor(text)">{{ text || 'UNKNOWN' }}</a-tag>
              </template>
              <template slot="time" slot-scope="text">{{ formatTime(text) }}</template>
            </a-table>
          </section>

          <section class="panel">
            <div class="panel-title">高风险流程</div>
            <div class="panel-hint">当前口径：{{ trendSelectionLabel }}。最多展示最严重的前 10 个流程，点击某个流程可查看下方高风险子任务</div>
            <a-table
              size="small"
              row-key="flowRiskKey"
              :columns="riskColumns"
              :data-source="riskFlows"
              :pagination="false"
              :loading="loadingTasks"
              :custom-row="riskFlowRow"
              :row-class-name="riskFlowRowClassName"
            >
              <template slot="flowRisk" slot-scope="text, record">
                <div class="primary-text">{{ record.projectName }} / {{ record.flowName }}</div>
                <div class="muted-text">{{ record.clusterName || record.clusterCode }} · {{ record.schedulerLabel }}</div>
              </template>
              <template slot="failedRuns7d" slot-scope="text">
                <a-tag :color="numberRiskColor(text)">{{ text || 0 }}</a-tag>
              </template>
              <template slot="delayedRuns7d" slot-scope="text">
                <a-tag :color="numberRiskColor(text)">{{ text || 0 }}</a-tag>
              </template>
              <template slot="riskReason" slot-scope="text, record">
                <div class="risk-reason">{{ record.riskReason || '-' }}</div>
                <div class="muted-text">{{ selectedTrendDay ? '当天' : '近 7 天' }}运行 {{ record.runDisplay || 0 }} 次</div>
              </template>
            </a-table>

            <div class="subpanel-title">高风险任务</div>
            <div class="panel-hint">展示当前高风险流程下最严重的前 10 个子任务，也就是实际失败或延迟的 `...seq0` / 具体任务项</div>
            <a-table
              size="small"
              row-key="taskKey"
              :columns="riskTaskColumns"
              :data-source="selectedRiskTasks"
              :pagination="false"
              :loading="loadingTasks"
            >
              <template slot="task" slot-scope="text, record">
                <div class="primary-text">{{ record.taskName }}</div>
                <div class="muted-text">{{ record.projectName }} / {{ record.flowName }}</div>
                <div class="muted-text">{{ record.clusterName || record.clusterCode }} · {{ record.schedulerLabel }}</div>
              </template>
              <template slot="failedRuns7d" slot-scope="text">
                <a-tag :color="numberRiskColor(text)">{{ text || 0 }}</a-tag>
              </template>
              <template slot="delayedRuns7d" slot-scope="text">
                <a-tag :color="numberRiskColor(text)">{{ text || 0 }}</a-tag>
              </template>
              <template slot="riskReason" slot-scope="text, record">
                <div class="risk-reason">{{ record.riskReason || '-' }}</div>
                <div class="muted-text">{{ selectedTrendDay ? '当天' : '近 7 天' }}运行 {{ record.runDisplay || 0 }} 次</div>
              </template>
            </a-table>
          </section>
        </div>
      </a-tab-pane>

      <a-tab-pane key="tasks" tab="任务责任归属">
        <div class="toolbar">
          <a-input-search v-model="filters.keyword" allow-clear placeholder="搜索项目/流程/任务/负责人" style="width: 260px" @search="refreshAll" />
          <a-select v-model="filters.health" allowClear placeholder="健康状态" style="width: 140px" @change="refreshAll">
            <a-select-option value="HEALTHY">健康</a-select-option>
            <a-select-option value="FAILED">失败</a-select-option>
            <a-select-option value="RUNNING">运行中</a-select-option>
            <a-select-option value="UNKNOWN">未知</a-select-option>
          </a-select>
          <a-select v-model="filters.assetActive" allowClear placeholder="资产活跃" style="width: 140px" @change="refreshAll">
            <a-select-option :value="true">有活跃资产</a-select-option>
            <a-select-option :value="false">无活跃资产</a-select-option>
          </a-select>
          <a-input v-model="filters.owner" allow-clear placeholder="负责人" style="width: 180px" @pressEnter="refreshAll" />
          <a-button icon="search" @click="refreshAll">查询</a-button>
        </div>

        <div class="project-workbench">
          <section class="project-list-panel panel">
            <div class="panel-title">项目列表</div>
            <div class="panel-hint">按项目聚合调度任务，点击项目查看流程图和状态明细</div>
            <a-table
              row-key="projectKey"
              size="small"
              :columns="projectColumns"
              :data-source="projectRows"
              :loading="loadingTasks"
              :pagination="{ pageSize: 10 }"
              :scroll="{ x: 920 }"
              :expanded-row-keys="expandedProjectKeys"
              :custom-row="projectRow"
              :row-class-name="projectRowClassName"
              @expand="onProjectExpand"
            >
              <template slot="project" slot-scope="text, record">
                <div class="primary-text">{{ record.projectName }}</div>
                <div class="muted-text">{{ record.clusterName || record.clusterCode }} · {{ record.schedulerLabel }} · {{ record.endpointLabel }}</div>
              </template>
              <template slot="owner" slot-scope="text, record">
                <a-tag v-for="owner in record.owners" :key="owner" color="blue">{{ owner }}</a-tag>
                <span v-if="!record.owners.length" class="muted-text">未归属</span>
              </template>
              <template slot="health" slot-scope="text">
                <a-tag :color="healthColor(text)">{{ healthLabel(text) }}</a-tag>
              </template>
              <template slot="asset" slot-scope="text, record">
                <div class="asset-cell">
                  <a-tag :color="assetColor(record.assetActive)">{{ assetLabel(record.assetActive, record) }}</a-tag>
                  <span class="asset-meta">{{ assetSummary(record) }}</span>
                </div>
              </template>
              <template slot="time" slot-scope="text">{{ formatTime(text) }}</template>
              <template slot="expandedRowRender" slot-scope="record">
                <div class="project-inline-detail">
                  <div class="detail-header">
                    <div>
                      <div class="panel-title">{{ record.projectName }}</div>
                      <div class="panel-hint">{{ record.clusterName || record.clusterCode }} · {{ record.schedulerLabel }} · {{ record.endpointLabel }}</div>
                    </div>
                    <a-tag :color="healthColor(record.healthStatus)">{{ healthLabel(record.healthStatus) }}</a-tag>
                  </div>

                  <div class="project-metrics">
                    <div class="metric-item">
                      <span>流程</span>
                      <strong>{{ record.flowCount }}</strong>
                    </div>
                    <div class="metric-item">
                      <span>任务</span>
                      <strong>{{ record.taskCount }}</strong>
                    </div>
                    <div class="metric-item">
                      <span>失败</span>
                      <strong class="danger-text">{{ record.failedTasks }}</strong>
                    </div>
                    <div class="metric-item">
                      <span>运行</span>
                      <strong>{{ record.runningTasks }}</strong>
                    </div>
                  </div>

                  <div class="subpanel-title">项目流程图</div>
                  <div class="flow-map">
                    <div class="flow-root">
                      <div class="flow-node project-node">
                        <div class="flow-node-title">{{ record.projectName }}</div>
                        <div class="muted-text">项目入口</div>
                      </div>
                    </div>
                    <div class="flow-branches">
                      <button
                        v-for="flow in projectFlowsFor(record)"
                        :key="flow.flowKey"
                        type="button"
                        class="flow-node flow-card"
                        :class="flowNodeClass(flow.healthStatus)"
                      >
                        <a-tooltip :title="flow.flowName">
                          <div class="flow-node-title">{{ flow.flowName }}</div>
                        </a-tooltip>
                        <div class="flow-node-meta">
                          <a-tag :color="healthColor(flow.healthStatus)">{{ healthLabel(flow.healthStatus) }}</a-tag>
                          <span>{{ flow.taskCount }} 个任务</span>
                        </div>
                        <div class="muted-text">最近：{{ formatTime(flow.lastStartTime) }}</div>
                      </button>
                    </div>
                  </div>

                  <div class="subpanel-title">流程状态明细</div>
                  <a-table
                    row-key="flowKey"
                    size="small"
                    :columns="projectFlowColumns"
                    :data-source="projectFlowsFor(record)"
                    :pagination="{ pageSize: 8 }"
                    :scroll="{ x: 920 }"
                  >
                    <template slot="flow" slot-scope="text, flowRecord">
                      <a-tooltip :title="flowRecord.flowName">
                        <div class="primary-text flow-name-cell">{{ flowRecord.flowName }}</div>
                      </a-tooltip>
                      <div class="muted-text">任务 {{ flowRecord.taskCount }} · 运行 {{ flowRecord.runCount }}</div>
                    </template>
                    <template slot="health" slot-scope="text">
                      <a-tag :color="healthColor(text)">{{ healthLabel(text) }}</a-tag>
                    </template>
                    <template slot="owner" slot-scope="text, flowRecord">
                      <a-tag v-for="owner in flowRecord.owners" :key="owner" color="blue">{{ owner }}</a-tag>
                      <span v-if="!flowRecord.owners.length" class="muted-text">未归属</span>
                    </template>
                    <template slot="time" slot-scope="text">{{ formatTime(text) }}</template>
                    <template slot="action" slot-scope="text, flowRecord">
                      <a-button type="link" size="small" icon="user" @click.stop="showOwnerModal(flowRecord.sampleTask)">维护责任人</a-button>
                    </template>
                  </a-table>
                </div>
              </template>
            </a-table>
          </section>
        </div>
      </a-tab-pane>

      <a-tab-pane key="groups" tab="项目/流程资产分组">
        <a-table
          row-key="groupKey"
          :columns="groupColumns"
          :data-source="groupRows"
          :loading="loadingGroups"
          :scroll="{ x: 1280 }"
          :pagination="{ pageSize: 10 }"
        >
          <template slot="group" slot-scope="text, record">
            <div class="primary-text">{{ record.projectName }}</div>
            <div class="muted-text">{{ record.flowName }}</div>
            <div class="muted-text">{{ record.clusterName || record.clusterCode }} · {{ record.schedulerLabel }}</div>
          </template>
          <template slot="owners" slot-scope="owners">
            <a-tag v-for="owner in owners" :key="owner" color="blue">{{ owner }}</a-tag>
            <span v-if="!owners || !owners.length">-</span>
          </template>
          <template slot="assets" slot-scope="assets">
            <a-tooltip v-if="assets && assets.length" :title="assetTooltip(assets)">
              <span class="ellipsis">{{ assets.map(item => item.name).join(', ') }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      title="维护任务责任归属"
      :visible="ownerModalVisible"
      :confirm-loading="savingOwner"
      @ok="saveOwner"
      @cancel="ownerModalVisible = false"
    >
      <a-form-model layout="vertical">
        <a-form-model-item label="调度任务">
          <div class="readonly-box">{{ ownerForm.projectName }} / {{ ownerForm.flowName }} / {{ ownerForm.taskName }}</div>
        </a-form-model-item>
        <a-form-model-item label="主责任人">
          <a-input v-model="ownerForm.owner" placeholder="例如：zhangsan" />
        </a-form-model-item>
        <a-form-model-item label="协同责任人">
          <a-input v-model="ownerForm.collaboratorOwners" placeholder="多个账号用逗号分隔" />
        </a-form-model-item>
        <a-form-model-item label="备注">
          <a-textarea v-model="ownerForm.remark" :rows="3" placeholder="归属依据或交接说明" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios';
import * as echarts from 'echarts';

export default {
  data() {
    return {
      activeTab: 'dashboard',
      loadingClusters: false,
      loadingOverview: false,
      loadingTasks: false,
      loadingGroups: false,
      savingOwner: false,
      selectedRiskFlowKey: '',
      selectedProjectKey: '',
      expandedProjectKeys: [],
      selectedTrendDay: '',
      trendChart: null,
      clusters: [],
      schedulerEndpoints: [],
      selectedClusterCode: undefined,
      selectedSchedulerType: 'ALL',
      selectedEndpointKey: 'ALL',
      overview: {},
      allTasks: [],
      riskTasksRaw: [],
      tasks: [],
      groups: [],
      filters: {
        keyword: '',
        health: undefined,
        assetActive: undefined,
        owner: ''
      },
      ownerModalVisible: false,
      ownerForm: {},
      runColumns: [
        { title: '项目/流程', scopedSlots: { customRender: 'flow' }, width: 320 },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' }, width: 120 },
        { title: '开始时间', dataIndex: 'startTime', scopedSlots: { customRender: 'time' }, width: 170 },
        { title: '结束时间', dataIndex: 'endTime', scopedSlots: { customRender: 'time' }, width: 170 }
      ],
      riskColumns: [
        { title: '项目/流程', scopedSlots: { customRender: 'flowRisk' }, width: 280 },
        { title: '失败次数', dataIndex: 'failedDisplay', scopedSlots: { customRender: 'failedRuns7d' }, width: 110 },
        { title: '延迟次数', dataIndex: 'delayedDisplay', scopedSlots: { customRender: 'delayedRuns7d' }, width: 110 },
        { title: '风险说明', scopedSlots: { customRender: 'riskReason' }, width: 220 }
      ],
      riskTaskColumns: [
        { title: '子任务', scopedSlots: { customRender: 'task' }, width: 320 },
        { title: '负责人', dataIndex: 'owner', width: 120 },
        { title: '失败次数', dataIndex: 'failedDisplay', scopedSlots: { customRender: 'failedRuns7d' }, width: 110 },
        { title: '延迟次数', dataIndex: 'delayedDisplay', scopedSlots: { customRender: 'delayedRuns7d' }, width: 110 },
        { title: '风险说明', scopedSlots: { customRender: 'riskReason' }, width: 210 }
      ],
      projectColumns: [
        { title: '项目', scopedSlots: { customRender: 'project' }, width: 260 },
        { title: '流程/任务', dataIndex: 'flowTaskLabel', width: 110 },
        { title: '责任人', scopedSlots: { customRender: 'owner' }, width: 160 },
        { title: '健康状态', dataIndex: 'healthStatus', scopedSlots: { customRender: 'health' }, width: 100 },
        { title: '资产活跃', scopedSlots: { customRender: 'asset' }, width: 160 },
        { title: '最近开始', dataIndex: 'lastStartTime', scopedSlots: { customRender: 'time' }, width: 160 }
      ],
      projectFlowColumns: [
        { title: '流程', scopedSlots: { customRender: 'flow' }, width: 320 },
        { title: '状态', dataIndex: 'healthStatus', scopedSlots: { customRender: 'health' }, width: 100 },
        { title: '失败任务', dataIndex: 'failedTasks', width: 90 },
        { title: '活跃资产', dataIndex: 'activeAssetCount', width: 90 },
        { title: '责任人', scopedSlots: { customRender: 'owner' }, width: 180 },
        { title: '最近开始', dataIndex: 'lastStartTime', scopedSlots: { customRender: 'time' }, width: 160 },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 120 }
      ],
      groupColumns: [
        { title: '项目/流程', scopedSlots: { customRender: 'group' }, width: 340 },
        { title: '任务数', dataIndex: 'taskCount', width: 100 },
        { title: '失败任务', dataIndex: 'failedTasks', width: 100 },
        { title: '活跃资产', dataIndex: 'activeAssetCount', width: 100 },
        { title: '责任人', dataIndex: 'owners', scopedSlots: { customRender: 'owners' }, width: 220 },
        { title: '上下文资产', dataIndex: 'assets', scopedSlots: { customRender: 'assets' }, width: 360 }
      ]
    };
  },
  computed: {
    clusterOptions() {
      return this.clusters.map(cluster => ({
        value: cluster.clusterCode,
        label: cluster.clusterName || cluster.clusterCode
      }));
    },
    selectedCluster() {
      return this.clusters.find(item => item.clusterCode === this.selectedClusterCode) || null;
    },
    schedulerEndpointsForCluster() {
      return this.schedulerEndpoints.filter(item => item.clusterCode === this.selectedClusterCode);
    },
    schedulerTypeOptions() {
      const base = [{ value: 'ALL', label: '全部调度器' }];
      const types = new Set(this.schedulerEndpointsForCluster.map(item => item.schedulerType));
      if (types.has('AZKABAN')) base.push({ value: 'AZKABAN', label: 'Azkaban' });
      if (types.has('DOLPHIN')) base.push({ value: 'DOLPHIN', label: 'DolphinScheduler' });
      return base;
    },
    visibleEndpoints() {
      if (this.selectedSchedulerType === 'ALL') {
        return this.schedulerEndpointsForCluster;
      }
      return this.schedulerEndpointsForCluster.filter(item => item.schedulerType === this.selectedSchedulerType);
    },
    groupedEndpoints() {
      const groups = [];
      this.visibleEndpoints.forEach(endpoint => {
        let group = groups.find(item => item.type === endpoint.schedulerType);
        if (!group) {
          group = {
            type: endpoint.schedulerType,
            label: endpoint.schedulerLabel,
            options: []
          };
          groups.push(group);
        }
        group.options.push(endpoint);
      });
      return groups;
    },
    queryableEndpoints() {
      const source = this.selectedEndpointKey === 'ALL'
        ? this.visibleEndpoints
        : this.visibleEndpoints.filter(item => item.key === this.selectedEndpointKey);
      return source.filter(item => item.endpointType === 'AZKABAN_DB');
    },
    showUnsupportedAlert() {
      if (!this.selectedClusterCode) return false;
      const source = this.selectedEndpointKey === 'ALL'
        ? this.visibleEndpoints
        : this.visibleEndpoints.filter(item => item.key === this.selectedEndpointKey);
      return source.some(item => item.endpointType !== 'AZKABAN_DB');
    },
    visibleSchedulerTypesLabel() {
      const labels = [...new Set(this.visibleEndpoints.map(item => item.schedulerLabel))];
      return labels.length ? labels.join(' / ') : '-';
    },
    currentScopeLabel() {
      if (this.selectedEndpointKey !== 'ALL') {
        const endpoint = this.visibleEndpoints.find(item => item.key === this.selectedEndpointKey);
        return endpoint ? endpoint.label : '单端点';
      }
      if (this.selectedSchedulerType === 'ALL') return '集群全调度器';
      return `${this.selectedSchedulerType === 'AZKABAN' ? 'Azkaban' : 'DolphinScheduler'} 全端点`;
    },
    trendSelectionLabel() {
      return this.selectedTrendDay ? `${String(this.selectedTrendDay).slice(5)} 当天高风险` : '近 7 天高风险';
    },
    riskTaskCandidates() {
      return this.riskTasksRaw
        .map(item => {
          const riskParts = [];
          const failedCount = Number(item.failedCount || 0);
          const delayedCount = Number(item.delayedCount || 0);
          const riskThreshold = this.selectedTrendDay ? 1 : 2;
          let riskScore = failedCount * 2 + delayedCount;
          if (failedCount >= riskThreshold) {
            riskParts.push(`${this.selectedTrendDay ? '当日' : '近7天'}失败 ${failedCount} 次`);
          }
          if (delayedCount >= riskThreshold) {
            riskParts.push(`${this.selectedTrendDay ? '当日' : '近7天'}启动延迟 ${delayedCount} 次`);
          }
          if (!item.owner && riskScore > 0) {
            riskParts.push('责任人未维护');
          }
          return {
            ...item,
            failedDisplay: failedCount,
            delayedDisplay: delayedCount,
            runDisplay: Number(item.runCount || 0),
            riskScore,
            riskReason: riskParts.join(' / ')
          };
        })
        .filter(item => item.riskScore > 0)
        .sort((a, b) => {
          if (b.riskScore !== a.riskScore) return b.riskScore - a.riskScore;
          return String(b.lastStartTime || '').localeCompare(String(a.lastStartTime || ''));
        });
    },
    riskFlows() {
      const flowMap = new Map();
      this.riskTaskCandidates.forEach(task => {
        const key = `${task.clusterCode}/${task.schedulerType}/${task.projectName}/${task.flowName}`;
        if (!flowMap.has(key)) {
          flowMap.set(key, {
            flowRiskKey: key,
            clusterCode: task.clusterCode,
            clusterName: task.clusterName,
            schedulerType: task.schedulerType,
            schedulerLabel: task.schedulerLabel,
            projectName: task.projectName,
            flowName: task.flowName,
            failedDisplay: 0,
            delayedDisplay: 0,
            riskScore: 0,
            runDisplay: 0,
            taskCount: 0
          });
        }
        const flow = flowMap.get(key);
        flow.failedDisplay += Number(task.failedDisplay || 0);
        flow.delayedDisplay += Number(task.delayedDisplay || 0);
        flow.riskScore += Number(task.riskScore || 0);
        flow.runDisplay = Math.max(flow.runDisplay, Number(task.runDisplay || 0));
        flow.taskCount += 1;
      });
      return Array.from(flowMap.values())
        .map(flow => ({
          ...flow,
          riskReason: `${this.selectedTrendDay ? '当天' : '近7天'}失败 ${flow.failedDisplay} 次 / 延迟 ${flow.delayedDisplay} 次 / 风险子任务 ${flow.taskCount} 个`
        }))
        .sort((a, b) => {
          if (b.riskScore !== a.riskScore) return b.riskScore - a.riskScore;
          if (b.failedDisplay !== a.failedDisplay) return b.failedDisplay - a.failedDisplay;
          return b.delayedDisplay - a.delayedDisplay;
        })
        .slice(0, 10);
    },
    activeRiskFlowKey() {
      if (this.selectedRiskFlowKey && this.riskFlows.some(item => item.flowRiskKey === this.selectedRiskFlowKey)) {
        return this.selectedRiskFlowKey;
      }
      return this.riskFlows.length ? this.riskFlows[0].flowRiskKey : '';
    },
    selectedRiskTasks() {
      if (!this.activeRiskFlowKey) return [];
      return this.riskTaskCandidates
        .filter(item => `${item.clusterCode}/${item.schedulerType}/${item.projectName}/${item.flowName}` === this.activeRiskFlowKey)
        .slice(0, 10);
    },
    filteredTasks() {
      return this.allTasks
        .filter(task => !this.filters.health || this.filters.health === task.healthStatus)
        .filter(task => this.filters.assetActive === undefined || this.filters.assetActive === null || this.filters.assetActive === task.assetActive)
        .filter(task => !this.filters.owner || String(task.owner || '').includes(this.filters.owner))
        .filter(task => {
          if (!this.filters.keyword) return true;
          const keyword = this.filters.keyword.toLowerCase();
          return [task.projectName, task.flowName, task.taskName, task.owner]
            .filter(Boolean)
            .some(item => String(item).toLowerCase().includes(keyword));
        })
        .sort((a, b) => String(b.lastStartTime || '').localeCompare(String(a.lastStartTime || '')));
    },
    projectRows() {
      const projectMap = new Map();
      this.filteredTasks.forEach(task => {
        const key = `${task.clusterCode}/${task.schedulerType}/${task.endpointId}/${task.projectName}`;
        if (!projectMap.has(key)) {
          projectMap.set(key, {
            projectKey: key,
            clusterCode: task.clusterCode,
            clusterName: task.clusterName,
            schedulerType: task.schedulerType,
            schedulerLabel: task.schedulerLabel,
            endpointId: task.endpointId,
            endpointLabel: task.endpointLabel,
            projectName: task.projectName,
            flowNames: new Set(),
            taskCount: 0,
            failedTasks: 0,
            runningTasks: 0,
            unknownTasks: 0,
            runCount: 0,
            assetCount: 0,
            activeAssetCount: 0,
            assetActive: undefined,
            owners: [],
            lastStartTime: '',
            sampleTask: task
          });
        }
        const project = projectMap.get(key);
        project.flowNames.add(task.flowName);
        project.taskCount += 1;
        project.runCount += Number(task.runCount || 0);
        project.assetCount += Number(task.assetCount || 0);
        project.activeAssetCount += Number(task.activeAssetCount || 0);
        if (task.assetActive === true) project.assetActive = true;
        if (project.assetActive !== true && task.assetActive === false) project.assetActive = false;
        if (task.healthStatus === 'FAILED') project.failedTasks += 1;
        if (task.healthStatus === 'RUNNING') project.runningTasks += 1;
        if (task.healthStatus === 'UNKNOWN') project.unknownTasks += 1;
        if (task.owner && !project.owners.includes(task.owner)) project.owners.push(task.owner);
        if (String(task.lastStartTime || '').localeCompare(String(project.lastStartTime || '')) > 0) {
          project.lastStartTime = task.lastStartTime;
          project.sampleTask = task;
        }
      });
      return Array.from(projectMap.values())
        .map(project => {
          const flowCount = project.flowNames.size;
          return {
            ...project,
            flowCount,
            flowTaskLabel: `${flowCount} / ${project.taskCount}`,
            healthStatus: this.aggregateHealth(project),
            assetCount: project.assetCount,
            activeAssetCount: project.activeAssetCount
          };
        })
        .sort((a, b) => String(b.lastStartTime || '').localeCompare(String(a.lastStartTime || '')));
    },
    activeProjectKey() {
      if (this.selectedProjectKey && this.projectRows.some(item => item.projectKey === this.selectedProjectKey)) {
        return this.selectedProjectKey;
      }
      return this.projectRows.length ? this.projectRows[0].projectKey : '';
    },
    selectedProject() {
      return this.projectRows.find(item => item.projectKey === this.activeProjectKey) || null;
    },
    selectedProjectTasks() {
      if (!this.activeProjectKey) return [];
      return this.projectTasksForKey(this.activeProjectKey);
    },
    selectedProjectFlows() {
      return this.projectFlowsForKey(this.activeProjectKey);
    },
    groupRows() {
      return this.groups.map(item => ({
        ...item,
        groupKey: `${item.clusterCode}/${item.schedulerType}/${item.projectName}/${item.flowName}`
      }));
    }
  },
  mounted() {
    this.fetchClusters();
    window.addEventListener('resize', this.resizeTrendChart);
  },
  watch: {
    selectedTrendDay() {
      this.refreshRiskData();
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeTrendChart);
    if (this.trendChart) {
      this.trendChart.dispose();
      this.trendChart = null;
    }
  },
  methods: {
    projectTasksFor(project) {
      return this.projectTasksForKey(project && project.projectKey);
    },
    projectTasksForKey(projectKey) {
      if (!projectKey) return [];
      return this.filteredTasks.filter(task => `${task.clusterCode}/${task.schedulerType}/${task.endpointId}/${task.projectName}` === projectKey);
    },
    projectFlowsFor(project) {
      return this.projectFlowsForKey(project && project.projectKey);
    },
    projectFlowsForKey(projectKey) {
      const flowMap = new Map();
      this.projectTasksForKey(projectKey).forEach(task => {
        const key = `${task.clusterCode}/${task.schedulerType}/${task.endpointId}/${task.projectName}/${task.flowName}`;
        if (!flowMap.has(key)) {
          flowMap.set(key, {
            flowKey: key,
            flowName: task.flowName,
            taskCount: 0,
            failedTasks: 0,
            runningTasks: 0,
            unknownTasks: 0,
            runCount: 0,
            assetCount: 0,
            activeAssetCount: 0,
            owners: [],
            lastStartTime: '',
            sampleTask: task
          });
        }
        const flow = flowMap.get(key);
        flow.taskCount += 1;
        flow.runCount += Number(task.runCount || 0);
        flow.assetCount += Number(task.assetCount || 0);
        flow.activeAssetCount += Number(task.activeAssetCount || 0);
        if (task.healthStatus === 'FAILED') flow.failedTasks += 1;
        if (task.healthStatus === 'RUNNING') flow.runningTasks += 1;
        if (task.healthStatus === 'UNKNOWN') flow.unknownTasks += 1;
        if (task.owner && !flow.owners.includes(task.owner)) flow.owners.push(task.owner);
        if (String(task.lastStartTime || '').localeCompare(String(flow.lastStartTime || '')) > 0) {
          flow.lastStartTime = task.lastStartTime;
          flow.sampleTask = task;
        }
      });
      return Array.from(flowMap.values())
        .map(flow => ({
          ...flow,
          healthStatus: this.aggregateHealth(flow)
        }))
        .sort((a, b) => {
          const healthWeight = { FAILED: 3, RUNNING: 2, UNKNOWN: 1, HEALTHY: 0 };
          const diff = (healthWeight[b.healthStatus] || 0) - (healthWeight[a.healthStatus] || 0);
          if (diff !== 0) return diff;
          return String(b.lastStartTime || '').localeCompare(String(a.lastStartTime || ''));
        });
    },
    async fetchClusters() {
      this.loadingClusters = true;
      try {
        const res = await axios.get('/api/clusters');
        const endpoints = [];
        const clusters = [];
        (res.data || []).forEach(cluster => {
          const clusterItem = {
            clusterCode: cluster.clusterCode,
            clusterName: cluster.clusterName
          };
          (cluster.endpoints || []).forEach(endpoint => {
            if ((endpoint.endpointType === 'AZKABAN_DB' || endpoint.endpointType === 'DOLPHINSCHEDULER_DB') && endpoint.status === 'ACTIVE') {
              const schedulerType = endpoint.endpointType === 'AZKABAN_DB' ? 'AZKABAN' : 'DOLPHIN';
              endpoints.push({
                key: `${cluster.clusterCode}:${endpoint.id}`,
                clusterCode: cluster.clusterCode,
                clusterName: cluster.clusterName,
                endpointId: endpoint.id,
                endpointType: endpoint.endpointType,
                schedulerType,
                schedulerLabel: schedulerType === 'AZKABAN' ? 'Azkaban' : 'DolphinScheduler',
                label: endpoint.serviceName || endpoint.description || `${schedulerType === 'AZKABAN' ? 'Azkaban-ds' : 'Dolphin-ds'} #${endpoint.id}`
              });
            }
          });
          if (endpoints.some(item => item.clusterCode === cluster.clusterCode)) {
            clusters.push(clusterItem);
          }
        });
        this.clusters = clusters;
        this.schedulerEndpoints = endpoints;
        if (!this.selectedClusterCode && this.clusters.length) {
          this.selectedClusterCode = this.clusters[0].clusterCode;
          this.selectedSchedulerType = 'ALL';
          this.selectedEndpointKey = 'ALL';
          await this.refreshAll();
        }
      } catch (e) {
        this.$message.error(this.errorMessage(e, '获取调度端点失败'));
      } finally {
        this.loadingClusters = false;
      }
    },
    onClusterChange() {
      this.selectedSchedulerType = 'ALL';
      this.selectedEndpointKey = 'ALL';
      this.refreshAll();
    },
    onSchedulerTypeChange() {
      this.selectedEndpointKey = 'ALL';
      this.refreshAll();
    },
    onEndpointChange() {
      this.refreshAll();
    },
    syncRiskSelection() {
      if (!this.riskFlows.length) {
        this.selectedRiskFlowKey = '';
        return;
      }
      if (!this.riskFlows.some(item => item.flowRiskKey === this.selectedRiskFlowKey)) {
        this.selectedRiskFlowKey = this.riskFlows[0].flowRiskKey;
      }
    },
    async refreshRiskData() {
      if (!this.queryableEndpoints.length) {
        this.riskTasksRaw = [];
        this.selectedRiskFlowKey = '';
        return;
      }
      try {
        const riskResponses = await Promise.all(this.queryableEndpoints.map(endpoint =>
          axios.get('/api/scheduler/risk-tasks', {
            params: {
              clusterCode: endpoint.clusterCode,
              endpointId: endpoint.endpointId,
              day: this.selectedTrendDay || undefined
            }
          }).then(res => ({ endpoint, data: res.data || [] }))
        ));
        this.riskTasksRaw = riskResponses.flatMap(item =>
          item.data.map(task => ({
            ...task,
            clusterName: item.endpoint.clusterName,
            schedulerType: item.endpoint.schedulerType,
            schedulerLabel: item.endpoint.schedulerLabel,
            endpointLabel: item.endpoint.label
          }))
        );
        this.syncRiskSelection();
      } catch (e) {
        this.$message.error(this.errorMessage(e, '获取高风险流程失败'));
      }
    },
    async refreshAll() {
      if (!this.selectedClusterCode) return;
      if (!this.queryableEndpoints.length) {
        this.overview = {};
        this.allTasks = [];
        this.riskTasksRaw = [];
        this.tasks = [];
        this.groups = [];
        this.selectedRiskFlowKey = '';
        return;
      }
      this.loadingOverview = true;
      this.loadingTasks = true;
      this.loadingGroups = true;
      try {
        const overviewResponses = await Promise.all(this.queryableEndpoints.map(endpoint =>
          axios.get('/api/scheduler/overview', {
            params: {
              clusterCode: endpoint.clusterCode,
              endpointId: endpoint.endpointId
            }
          }).then(res => ({ endpoint, data: res.data || {} }))
        ));
        const taskResponses = await Promise.all(this.queryableEndpoints.map(endpoint =>
          axios.get('/api/scheduler/tasks', {
            params: {
              clusterCode: endpoint.clusterCode,
              endpointId: endpoint.endpointId
            }
          }).then(res => ({ endpoint, data: res.data || [] }))
        ));
        this.allTasks = taskResponses.flatMap(item =>
          item.data.map(task => ({
            ...task,
            clusterName: item.endpoint.clusterName,
            schedulerType: item.endpoint.schedulerType,
            schedulerLabel: item.endpoint.schedulerLabel,
            endpointLabel: item.endpoint.label
          }))
        );
        this.tasks = this.filteredTasks;
        const recentRuns = overviewResponses.flatMap(item =>
          (item.data.recentRuns || []).map(run => ({
            ...run,
            rowKey: `${item.endpoint.key}:${run.runId}:${run.startTime || ''}`,
            clusterCode: item.endpoint.clusterCode,
            clusterName: item.endpoint.clusterName,
            schedulerType: item.endpoint.schedulerType,
            schedulerLabel: item.endpoint.schedulerLabel,
            endpointLabel: item.endpoint.label
          }))
        ).sort((a, b) => String(b.startTime || '').localeCompare(String(a.startTime || ''))).slice(0, 10);
        const dailyTrend = this.mergeDailyTrend(overviewResponses);
        this.overview = this.buildOverview(recentRuns, dailyTrend);
        this.groups = this.buildGroupsFromTasks(this.allTasks);
        await this.refreshRiskData();
        this.$nextTick(() => this.renderTrendChart());
      } catch (e) {
        this.$message.error(this.errorMessage(e, '获取调度视图失败'));
      } finally {
        this.loadingOverview = false;
        this.loadingTasks = false;
        this.loadingGroups = false;
      }
    },
    buildOverview(recentRuns, dailyTrend) {
      const projectSet = new Set();
      const flowSet = new Set();
      this.allTasks.forEach(item => {
        projectSet.add(item.projectName);
        flowSet.add(`${item.projectName}/${item.flowName}`);
      });
      return {
        clusterCode: this.selectedClusterCode,
        totalTasks: this.allTasks.length,
        totalProjects: projectSet.size,
        totalFlows: flowSet.size,
        failedTasks: this.allTasks.filter(item => item.healthStatus === 'FAILED').length,
        runningTasks: this.allTasks.filter(item => item.healthStatus === 'RUNNING').length,
        unownedTasks: this.allTasks.filter(item => !item.owner).length,
        activeAssetTasks: this.allTasks.filter(item => item.assetActive === true).length,
        recentRuns,
        dailyTrend,
        generatedAt: new Date().toISOString()
      };
    },
    mergeDailyTrend(overviewResponses) {
      const merged = new Map();
      overviewResponses.forEach(item => {
        (item.data.dailyTrend || []).forEach(point => {
          const day = point.day;
          if (!merged.has(day)) {
            merged.set(day, { day, successCount: 0, failedCount: 0 });
          }
          const row = merged.get(day);
          row.successCount += Number(point.successCount || 0);
          row.failedCount += Number(point.failedCount || 0);
        });
      });
      return Array.from(merged.values()).sort((a, b) => String(a.day).localeCompare(String(b.day)));
    },
    renderTrendChart() {
      if (!this.$refs.trendChart) return;
      const trend = this.overview.dailyTrend || [];
      if (!this.trendChart) {
        this.trendChart = echarts.init(this.$refs.trendChart);
      }
      const option = {
        grid: { left: 40, right: 24, top: 24, bottom: 32 },
        tooltip: { trigger: 'axis' },
        legend: {
          top: 0,
          right: 0,
          data: ['成功率', '失败率']
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: trend.map(item => String(item.day || '').slice(5))
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLabel: {
            formatter: '{value}%'
          }
        },
        series: [
          {
            name: '成功率',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 7,
            lineStyle: { width: 3, color: '#52c41a' },
            itemStyle: { color: '#52c41a' },
            areaStyle: { color: 'rgba(82, 196, 26, 0.12)' },
            data: trend.map(item => this.rateOf(item.successCount, item.failedCount))
          },
          {
            name: '失败率',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 7,
            lineStyle: { width: 3, color: '#f5222d' },
            itemStyle: { color: '#f5222d' },
            areaStyle: { color: 'rgba(245, 34, 45, 0.1)' },
            data: trend.map(item => this.rateOf(item.failedCount, item.successCount))
          }
        ]
      };
      this.trendChart.setOption(option, true);
      this.trendChart.off('click');
      this.trendChart.on('click', params => {
        const point = trend[params.dataIndex];
        if (!point || !point.day) return;
        this.selectTrendDay(point.day);
      });
    },
    selectTrendDay(day) {
      if (this.selectedTrendDay === day) {
        this.refreshRiskData();
        return;
      }
      this.selectedTrendDay = day;
    },
    clearTrendSelection() {
      this.selectedTrendDay = '';
    },
    resizeTrendChart() {
      this.trendChart && this.trendChart.resize();
    },
    rateOf(numerator, denominatorOther) {
      const a = Number(numerator || 0);
      const b = Number(denominatorOther || 0);
      const total = a + b;
      if (!total) return 0;
      return Number(((a / total) * 100).toFixed(1));
    },
    buildGroupsFromTasks(tasks) {
      const groups = new Map();
      tasks.forEach(task => {
        const key = `${task.clusterCode}/${task.schedulerType}/${task.projectName}/${task.flowName}`;
        if (!groups.has(key)) {
          groups.set(key, {
            clusterCode: task.clusterCode,
            clusterName: task.clusterName,
            sourceType: task.sourceType,
            schedulerType: task.schedulerType,
            schedulerLabel: task.schedulerLabel,
            projectName: task.projectName,
            flowName: task.flowName,
            taskCount: 0,
            failedTasks: 0,
            activeAssetCount: 0,
            owners: [],
            assets: []
          });
        }
        const group = groups.get(key);
        group.taskCount += 1;
        if (task.healthStatus === 'FAILED') group.failedTasks += 1;
        if (task.assetActive === true) group.activeAssetCount += 1;
        if (task.owner && !group.owners.includes(task.owner)) group.owners.push(task.owner);
        (task.assets || []).forEach(asset => {
          if (!group.assets.find(item => item.id === asset.id)) {
            group.assets.push(asset);
          }
        });
      });
      return Array.from(groups.values());
    },
    showOwnerModal(record) {
      this.ownerForm = {
        clusterCode: record.clusterCode,
        endpointId: record.endpointId,
        projectName: record.projectName,
        flowName: record.flowName,
        taskName: record.taskName,
        owner: record.owner || '',
        collaboratorOwners: record.collaboratorOwners || '',
        remark: ''
      };
      this.ownerModalVisible = true;
    },
    async saveOwner() {
      this.savingOwner = true;
      try {
        await axios.put('/api/scheduler/tasks/owner', this.ownerForm);
        this.$message.success('任务责任人已更新');
        this.ownerModalVisible = false;
        await this.refreshAll();
      } catch (e) {
        this.$message.error(this.errorMessage(e, '保存任务责任人失败'));
      } finally {
        this.savingOwner = false;
      }
    },
    assetTooltip(assets) {
      return assets.map(item => `${item.name}${item.owner ? ` (${item.owner})` : ''}`).join('\n');
    },
    riskFlowRow(record) {
      return {
        on: {
          click: () => {
            this.selectedRiskFlowKey = record.flowRiskKey;
          }
        }
      };
    },
    riskFlowRowClassName(record) {
      return record.flowRiskKey === this.activeRiskFlowKey ? 'risk-flow-row-active' : '';
    },
    projectRow(record) {
      return {
        on: {
          click: () => {
            this.selectedProjectKey = record.projectKey;
            this.expandedProjectKeys = [record.projectKey];
          }
        }
      };
    },
    onProjectExpand(expanded, record) {
      this.selectedProjectKey = record.projectKey;
      this.expandedProjectKeys = expanded ? [record.projectKey] : [];
    },
    projectRowClassName(record) {
      return record.projectKey === this.activeProjectKey ? 'project-row-active' : '';
    },
    aggregateHealth(item) {
      if (item.failedTasks > 0) return 'FAILED';
      if (item.runningTasks > 0) return 'RUNNING';
      if (item.unknownTasks > 0) return 'UNKNOWN';
      return 'HEALTHY';
    },
    flowNodeClass(status) {
      const normalized = String(status || '').toLowerCase();
      return `flow-node-${normalized}`;
    },
    healthLabel(value) {
      const map = { HEALTHY: '健康', FAILED: '失败', RUNNING: '运行中', UNKNOWN: '未知' };
      return map[value] || value || '未知';
    },
    healthColor(value) {
      const map = { HEALTHY: 'green', FAILED: 'red', RUNNING: 'blue', UNKNOWN: 'default' };
      return map[value] || 'default';
    },
    numberRiskColor(value) {
      const count = Number(value || 0);
      if (count >= 3) return 'red';
      if (count >= 2) return 'orange';
      return 'default';
    },
    statusColor(value) {
      const text = String(value || '').toUpperCase();
      if (text.includes('SUCCEEDED') || text.includes('SUCCESS')) return 'green';
      if (text.includes('FAILED') || text.includes('KILLED')) return 'red';
      if (text.includes('RUNNING') || text.includes('QUEUED') || text.includes('PREPARING')) return 'blue';
      return 'default';
    },
    assetLabel(value, record = {}) {
      const assetCount = record.assetCount || 0;
      if (value === true) return '活跃';
      if (value === false && assetCount > 0) return '不活跃';
      if (assetCount > 0) return '已关联';
      return '未关联';
    },
    assetColor(value) {
      if (value === true) return 'green';
      if (value === false) return 'orange';
      return 'default';
    },
    assetSummary(record = {}) {
      const total = record.assetCount || 0;
      const active = record.activeAssetCount || 0;
      if (total <= 0) return '未识别到上下文资产';
      return `活跃 ${active} / 总计 ${total}`;
    },
    formatTime(value) {
      if (!value) return '-';
      return String(value).replace('T', ' ').slice(0, 19);
    },
    formatDuration(value) {
      if (value == null || value === '') return '-';
      const totalSeconds = Math.max(0, Math.floor(Number(value) / 1000));
      const minutes = Math.floor(totalSeconds / 60);
      const seconds = totalSeconds % 60;
      if (minutes <= 0) return `${seconds}s`;
      return `${minutes}m ${seconds}s`;
    },
    errorMessage(e, fallback) {
      return (e.response && e.response.data && (e.response.data.message || e.response.data.error)) || fallback;
    }
  }
};
</script>

<style scoped>
.scheduler-view {
  color: #1f2933;
}

.module-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.module-header h1 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 650;
}

.module-header p,
.muted-text {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
}

.header-actions,
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.context-strip {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.context-title {
  font-weight: 600;
  color: #111827;
}

.context-meta {
  color: #6b7280;
  font-size: 12px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.summary-item,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.summary-item {
  padding: 14px 16px;
}

.summary-item span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 6px;
}

.summary-item strong {
  font-size: 24px;
  line-height: 1;
}

.danger-text {
  color: #cf1322;
}

.warning-text {
  color: #d48806;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

.project-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.project-list-panel,
.project-detail-panel {
  min-width: 0;
  overflow: hidden;
}

.panel {
  padding: 16px;
}

.trend-panel {
  margin-bottom: 16px;
}

.trend-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.trend-chart {
  width: 100%;
  height: 280px;
}

.panel-title {
  font-weight: 650;
  margin-bottom: 4px;
}

.panel-hint {
  margin-bottom: 12px;
  color: #6b7280;
  font-size: 12px;
}

.subpanel-title {
  margin: 16px 0 4px;
  font-weight: 650;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.project-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.metric-item {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.metric-item span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 4px;
}

.metric-item strong {
  color: #111827;
  font-size: 20px;
  line-height: 1;
}

.flow-map {
  display: grid;
  grid-template-columns: minmax(160px, 210px) minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 14px;
  margin-bottom: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fbfdff;
}

.flow-root {
  position: relative;
}

.flow-root::after {
  content: '';
  position: absolute;
  top: 50%;
  right: -16px;
  width: 16px;
  height: 1px;
  background: #cbd5e1;
}

.flow-branches {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}

.flow-node {
  width: 100%;
  min-height: 86px;
  padding: 10px 12px;
  text-align: left;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  background: #fff;
}

button.flow-node {
  cursor: default;
}

.project-node {
  border-color: #91caff;
  background: #e6f4ff;
}

.flow-node-title {
  margin-bottom: 6px;
  color: #1f2933;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-name-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-node-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.flow-node-failed {
  border-color: #ffccc7;
  background: #fff1f0;
}

.flow-node-running {
  border-color: #91caff;
  background: #e6f4ff;
}

.flow-node-unknown {
  border-color: #d9d9d9;
  background: #fafafa;
}

.flow-node-healthy {
  border-color: #b7eb8f;
  background: #f6ffed;
}

.toolbar {
  margin-bottom: 14px;
}

.primary-text {
  font-weight: 600;
}

.risk-reason {
  margin-top: 4px;
  color: #b45309;
  font-size: 12px;
}

.asset-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  line-height: 1.2;
}

.asset-meta {
  color: #6b7280;
  font-size: 12px;
  white-space: nowrap;
}

:deep(.risk-flow-row-active > td) {
  background: #f0f7ff !important;
}

:deep(.project-row-active > td),
:deep(.ant-table-tbody > tr.project-row-active:hover > td),
:deep(.ant-table-tbody > tr.risk-flow-row-active:hover > td) {
  background: #e6f4ff !important;
}

.ellipsis {
  display: inline-block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.readonly-box {
  padding: 8px 10px;
  background: #f5f7fa;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .summary-row,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .module-header {
    display: block;
  }

  .header-actions {
    margin-top: 12px;
  }

  .summary-row,
  .dashboard-grid,
  .project-workbench,
  .flow-map {
    grid-template-columns: 1fr;
  }

  .project-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flow-root::after {
    display: none;
  }
}
</style>
