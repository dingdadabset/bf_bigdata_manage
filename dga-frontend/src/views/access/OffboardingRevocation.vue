<template>
  <div class="offboarding-page">
    <div class="page-toolbar">
      <div class="title-block">
        <div class="title-icon"><a-icon type="disconnect" /></div>
        <div>
          <div class="page-kicker">OFFBOARDING ACCESS REVOCATION</div>
          <h2>离职权限回收</h2>
        </div>
      </div>
      <div class="toolbar-actions">
        <div class="stat-pill">
          <span class="stat-value">{{ pendingTaskCount }}</span>
          <span class="stat-label">待处理</span>
        </div>
        <div class="stat-pill">
          <span class="stat-value">{{ dueTodayCount }}</span>
          <span class="stat-label">今日到期</span>
        </div>
        <div class="stat-pill">
          <span class="stat-value danger">{{ failedTaskCount }}</span>
          <span class="stat-label">失败</span>
        </div>
        <a-button icon="reload" :loading="loadingUsers || loadingTasks" @click="refreshAll">刷新</a-button>
      </div>
    </div>

    <section class="task-panel">
      <div class="panel-head">
        <div>
          <h3>任务台账</h3>
          <span>{{ tasks.length }} 条最近任务，点击可查看任务单或确认单</span>
        </div>
        <a-radio-group
          v-model="taskStatusFilter"
          button-style="solid"
          size="small"
          @change="handleTaskFilterChange"
        >
          <a-radio-button value="ALL">全部</a-radio-button>
          <a-radio-button value="PENDING">待执行</a-radio-button>
          <a-radio-button value="RUNNING">执行中</a-radio-button>
          <a-radio-button value="FAILED">失败</a-radio-button>
          <a-radio-button value="COMPLETED">已完成</a-radio-button>
        </a-radio-group>
      </div>

      <div class="task-list" :class="{ loading: loadingTasks }">
        <a-spin v-if="loadingTasks" />
        <a-empty v-else-if="!tasks.length" class="empty-state compact" description="暂无回收任务" />
        <button
          v-for="task in tasks"
          v-else
          :key="task.taskId || task.taskNo"
          type="button"
          class="task-row"
          :class="{ selected: selectedTask && selectedTask.taskId === task.taskId }"
          @click="selectTask(task)"
        >
          <span :class="['task-status-bar', taskStatusClass(task.status)]"></span>
          <span class="task-main">
            <strong>{{ task.username }}</strong>
            <em>{{ task.taskNo }}</em>
          </span>
          <span class="task-meta">
            <a-tag :color="taskStatusColor(task.status)">{{ taskStatusText(task.status) }}</a-tag>
            <a-tag>{{ task.cluster || '-' }}</a-tag>
            <a-tag>{{ formatDate(task.departureDate) }}</a-tag>
          </span>
          <span class="task-message">{{ task.message || task.archiveName || '等待任务更新' }}</span>
        </button>
      </div>

      <div v-if="selectedTask" class="task-detail-strip">
        <div>
          <span>当前查看</span>
          <strong>{{ selectedTask.taskNo }}</strong>
          <em>{{ selectedTask.message || '任务详情已加载' }}</em>
        </div>
        <div class="task-detail-actions">
          <a-tag :color="taskStatusColor(selectedTask.status)">{{ taskStatusText(selectedTask.status) }}</a-tag>
          <a-button
            v-if="selectedTask.confirmationText"
            icon="copy"
            size="small"
            @click="copyConfirmation"
          >
            复制单据
          </a-button>
        </div>
      </div>
    </section>

    <div class="workbench">
      <section class="user-panel">
        <div class="panel-head">
          <div>
            <h3>用户检索</h3>
            <span>{{ form.cluster || '全部集群' }}</span>
          </div>
          <a-tag color="blue">{{ users.length }} 条</a-tag>
        </div>

        <div class="filter-strip">
          <a-select
            v-model="form.cluster"
            class="cluster-select"
            placeholder="集群"
            allow-clear
            @change="handleClusterChange"
          >
            <a-select-option
              v-for="cluster in clusters"
              :key="cluster.id"
              :value="cluster.clusterCode || cluster.clusterName"
            >
              {{ cluster.clusterName }}{{ cluster.clusterCode ? ` (${cluster.clusterCode})` : '' }}
            </a-select-option>
          </a-select>
          <a-input-search
            v-model="keyword"
            class="keyword-input"
            placeholder="搜索用户名"
            enter-button
            allow-clear
            @search="searchUsers"
          />
          <a-date-picker
            :value="departureDateMoment"
            class="date-input"
            format="YYYY-MM-DD"
            placeholder="离职日期"
            @change="handleDepartureDateChange"
          />
        </div>

        <div class="user-list" :class="{ loading: loadingUsers }">
          <a-spin v-if="loadingUsers" />
          <a-empty v-else-if="!users.length" class="empty-state" description="暂无用户" />
          <button
            v-for="record in users"
            v-else
            :key="`${record.username}-${record.clusterName || ''}-${record.id || ''}`"
            type="button"
            class="user-row"
            :class="{ selected: selectedUser && sameUser(record, selectedUser) }"
            @click="selectUser(record)"
          >
            <a-avatar icon="user" :style="{ backgroundColor: avatarColor(record.username) }" />
            <span class="user-main">
              <strong>{{ record.username }}</strong>
              <span>{{ record.email || record.displayName || 'No Email' }}</span>
            </span>
            <span class="user-meta">
              <a-tag color="blue">{{ record.clusterName || '-' }}</a-tag>
              <a-tag>{{ sourceLabel(record.creationStrategy) }}</a-tag>
            </span>
            <span class="user-state">
              <span :class="['state-dot', record.ldapLocked ? 'danger' : 'ready']"></span>
              {{ record.ldapLocked ? '已锁定' : '可处理' }}
            </span>
          </button>
        </div>
      </section>

      <section class="action-panel">
        <div class="selected-card" :class="{ empty: !selectedUser }">
          <div class="selected-avatar">
            <a-icon :type="selectedUser ? 'user' : 'question-circle'" />
          </div>
          <div class="selected-copy">
            <span>处置对象</span>
            <strong>{{ selectedUser ? selectedUser.username : '请选择用户' }}</strong>
            <em>{{ selectedUser ? `${selectedUser.clusterName || form.cluster || '-'} / ${sourceLabel(selectedUser.creationStrategy)}` : '未选择集群与用户' }}</em>
          </div>
          <a-tag :color="departureDate ? 'green' : 'orange'">{{ departureDate || '未选离职日期' }}</a-tag>
        </div>

        <section v-if="selectedTask" class="task-inspector" :class="taskStatusClass(selectedTask.status)">
          <div class="inspector-head">
            <div>
              <span>当前任务</span>
              <strong>{{ selectedTask.taskNo }}</strong>
            </div>
            <div class="inspector-actions">
              <a-tag :color="taskStatusColor(selectedTask.status)">{{ taskStatusText(selectedTask.status) }}</a-tag>
              <a-button
                v-if="selectedTask.confirmationText"
                size="small"
                icon="copy"
                @click="copyConfirmation"
              >
                复制单据
              </a-button>
            </div>
          </div>
          <div class="inspector-message">{{ selectedTask.message || selectedTask.archiveName || '任务详情已加载' }}</div>
          <div class="inspector-grid">
            <div>
              <span>回收用户</span>
              <strong>{{ selectedTask.username || '-' }}</strong>
            </div>
            <div>
              <span>集群</span>
              <strong>{{ selectedTask.cluster || '-' }}</strong>
            </div>
            <div>
              <span>离职日期</span>
              <strong>{{ formatDate(selectedTask.departureDate) }}</strong>
            </div>
            <div>
              <span>计划执行</span>
              <strong>{{ formatDateTime(selectedTask.scheduledAt) }}</strong>
            </div>
          </div>
        </section>

        <div class="readiness-card" :class="{ ready: canCreateTask }">
          <div class="readiness-head">
            <span>下一步</span>
            <strong>{{ nextActionText }}</strong>
          </div>
          <div class="readiness-checks">
            <div
              v-for="check in readinessChecks"
              :key="check.key"
              class="readiness-item"
              :class="{ done: check.done, active: check.active }"
            >
              <a-icon :type="check.done ? 'check-circle' : (check.active ? 'clock-circle' : 'minus-circle')" />
              <span>{{ check.label }}</span>
              <em>{{ check.detail }}</em>
            </div>
          </div>
        </div>

        <div class="execution-form">
          <a-form-model layout="vertical">
            <div class="form-grid">
              <a-form-model-item label="直属主管">
                <a-input v-model="form.managerContact" placeholder="邮箱或企业微信账号" />
              </a-form-model-item>
              <a-form-model-item label="安全团队">
                <a-input v-model="form.securityContact" placeholder="邮箱或群组标识" />
              </a-form-model-item>
              <a-form-model-item label="回收原因">
                <a-input v-model="form.reason" placeholder="离职权限回收" />
              </a-form-model-item>
            </div>
            <div class="button-row">
              <a-tooltip :title="previewDisabledReason">
                <span class="button-shell">
                  <a-button
                    icon="search"
                    class="preview-button"
                    :loading="previewing"
                    :disabled="!canPreview"
                    @click="fetchPreview"
                  >
                    预检影响范围
                  </a-button>
                </span>
              </a-tooltip>
              <a-tooltip :title="createDisabledReason">
                <span class="button-shell">
                  <a-button
                    type="danger"
                    icon="disconnect"
                    class="execute-button"
                    :loading="executing"
                    :disabled="!canCreateTask"
                    @click="confirmExecute"
                  >
                    创建回收任务
                  </a-button>
                </span>
              </a-tooltip>
            </div>
            <div v-if="actionHint" class="action-hint">
              <a-icon type="info-circle" />
              <span>{{ actionHint }}</span>
            </div>
          </a-form-model>
        </div>

        <section
          v-if="preview"
          class="preview-panel"
          :class="{ blocked: !preview.executable, stale: preview && !previewIsCurrent }"
        >
          <div class="preview-head">
            <div>
              <h3>预检结果</h3>
              <span>{{ preview.revocationFlow || '待识别回收路径' }}</span>
            </div>
            <a-tag :color="preview.executable && previewIsCurrent ? 'green' : 'orange'">
              {{ preview.executable && previewIsCurrent ? '可创建任务' : (previewIsCurrent ? '需处理' : '需重新预检') }}
            </a-tag>
          </div>
          <div class="preview-metrics">
            <div>
              <span>账号状态</span>
              <strong>{{ preview.accountCheckMessage || '-' }}</strong>
            </div>
            <div>
              <span>实时权限</span>
              <strong>{{ preview.livePermissionCount === null || preview.livePermissionCount === undefined ? '未知' : `${preview.livePermissionCount} 条` }}</strong>
            </div>
            <div>
              <span>本地台账</span>
              <strong>{{ preview.recordedPermissionCount || 0 }} 条</strong>
            </div>
          </div>
          <div v-if="preview.blockers && preview.blockers.length" class="alert-list danger">
            <div v-for="item in preview.blockers" :key="item">
              <a-icon type="stop" />
              <span>{{ item }}</span>
            </div>
          </div>
          <div v-if="preview.warnings && preview.warnings.length" class="alert-list warning">
            <div v-for="item in preview.warnings" :key="item">
              <a-icon type="exclamation-circle" />
              <span>{{ item }}</span>
            </div>
          </div>
        </section>

        <div class="flow-panel">
          <div class="flow-head">
            <h3>回收步骤</h3>
            <a-tag :color="result ? 'green' : (preview ? 'blue' : 'default')">{{ result ? '已生成结果' : (preview ? '预检计划' : '待执行') }}</a-tag>
          </div>
          <div
            v-for="(step, index) in renderedSteps"
            :key="step.key"
            class="flow-step"
            :class="`is-${step.status.toLowerCase()}`"
          >
            <div class="flow-icon">
              <a-icon :type="stepIcon(step.status)" />
            </div>
            <div class="flow-content">
              <div class="flow-title">
                <span>{{ index + 1 }}. {{ step.title }}</span>
                <a-tag :color="stepColor(step.status)">{{ stepStatusText(step.status) }}</a-tag>
              </div>
              <div class="flow-message">{{ step.message }}</div>
            </div>
          </div>
        </div>

        <section v-if="activeConfirmation" class="confirmation-panel">
          <div class="confirmation-head">
            <div>
              <span>{{ activeConfirmation.taskStatus === 'PENDING' ? '任务单' : '确认单' }}</span>
              <strong>{{ activeConfirmation.taskStatus === 'PENDING' ? activeConfirmation.taskNo : activeConfirmation.confirmationNo }}</strong>
            </div>
            <a-button icon="copy" @click="copyConfirmation">复制</a-button>
          </div>
          <div class="confirmation-grid">
            <div>
              <span>{{ activeConfirmation.taskStatus === 'PENDING' ? '计划执行' : '归档文件' }}</span>
              <strong>{{ activeConfirmation.taskStatus === 'PENDING' ? formatDateTime(activeConfirmation.scheduledAt) : (activeConfirmation.archiveName || '-') }}</strong>
            </div>
            <div>
              <span>通知对象</span>
              <strong>{{ activeConfirmation.notificationRecipients && activeConfirmation.notificationRecipients.length ? activeConfirmation.notificationRecipients.join('，') : '待补充' }}</strong>
            </div>
          </div>
          <pre class="confirmation-text">{{ activeConfirmation.confirmationText }}</pre>
        </section>
      </section>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import moment from 'moment';

const DEFAULT_STEPS = [
  { key: 'task-created', title: '创建离职权限回收任务', status: 'WAIT', message: '等待提交' },
  { key: 'notify-created', title: '发送任务创建提醒', status: 'WAIT', message: '等待提交' },
  { key: 'scheduled-wait', title: '等待离职日期到达', status: 'WAIT', message: '离职日期未到时由定时任务自动扫描' },
  { key: 'ldap-disable', title: 'OpenLDAP 账户禁用', status: 'WAIT', message: '等待执行' },
  { key: 'group-remove', title: '用户组移除', status: 'WAIT', message: '等待执行' },
  { key: 'bigdata-revoke', title: '大数据平台权限撤销', status: 'WAIT', message: '等待执行' },
  { key: 'cloud-accesskey-delete', title: '云账号 AccessKey 删除', status: 'WAIT', message: '等待执行' },
  { key: 'residual-account-governance', title: '离职后账号残留风险登记', status: 'WAIT', message: '等待回收完成后复核' },
  { key: 'notify', title: '发送回收完成通知', status: 'WAIT', message: '等待权限回收完成' },
  { key: 'archive', title: '生成《权限回收确认单》归档', status: 'WAIT', message: '等待执行' }
];

const COMMON_NATIVE_STEPS = [
  { key: 'task-created', title: '创建离职权限回收任务', status: 'WAIT', message: '等待提交' },
  { key: 'notify-created', title: '发送任务创建提醒', status: 'WAIT', message: '等待提交' },
  { key: 'scheduled-wait', title: '等待离职日期到达', status: 'WAIT', message: '离职日期未到时由定时任务自动扫描' },
  { key: 'native-user-check', title: '原生账号校验', status: 'WAIT', message: '等待执行' },
  { key: 'native-account-drop', title: '原生账号删除与权限撤销', status: 'WAIT', message: '等待执行' },
  { key: 'local-user-expire', title: 'DGA 本地用户状态标记', status: 'WAIT', message: '等待执行' },
  { key: 'cloud-accesskey-delete', title: '云账号 AccessKey 删除', status: 'WAIT', message: '等待执行' },
  { key: 'residual-account-governance', title: '离职后账号残留风险登记', status: 'WAIT', message: '等待回收完成后复核' },
  { key: 'notify', title: '发送回收完成通知', status: 'WAIT', message: '等待权限回收完成' },
  { key: 'archive', title: '生成《权限回收确认单》归档', status: 'WAIT', message: '等待执行' }
];

export default {
  name: 'OffboardingRevocation',
  data() {
    return {
      clusters: [],
      users: [],
      tasks: [],
      keyword: '',
      departureDate: null,
      selectedUser: null,
      selectedTask: null,
      taskStatusFilter: 'ALL',
      loadingUsers: false,
      loadingTasks: false,
      loadingTaskDetail: false,
      previewing: false,
      executing: false,
      preview: null,
      previewPayloadKey: '',
      result: null,
      form: {
        cluster: '',
        managerContact: '',
        securityContact: 'security-team',
        reason: '离职权限回收'
      }
    };
  },
  computed: {
    departureDateMoment() {
      return this.departureDate ? moment(this.departureDate, 'YYYY-MM-DD') : null;
    },
    canExecute() {
      return Boolean(this.selectedUser && this.departureDate && (this.selectedUser.clusterName || this.form.cluster));
    },
    canPreview() {
      return Boolean(this.selectedUser && (this.selectedUser.clusterName || this.form.cluster));
    },
    currentPreviewKey() {
      return JSON.stringify(this.buildPayload());
    },
    previewIsCurrent() {
      return Boolean(this.preview && this.previewPayloadKey === this.currentPreviewKey);
    },
    canCreateTask() {
      return Boolean(this.canExecute && this.preview && this.previewIsCurrent && this.preview.executable);
    },
    hasClusterContext() {
      return Boolean(this.selectedUser && (this.selectedUser.clusterName || this.form.cluster));
    },
    previewDisabledReason() {
      if (this.canPreview) {
        return '';
      }
      if (!this.selectedUser) {
        return '先在左侧选择一个离职用户';
      }
      return '当前用户缺少集群信息，请先选择集群';
    },
    createDisabledReason() {
      if (this.canCreateTask) {
        return '';
      }
      if (!this.selectedUser) {
        return '先在左侧选择一个离职用户';
      }
      if (!this.hasClusterContext) {
        return '当前用户缺少集群信息，请先选择集群';
      }
      if (!this.departureDate) {
        return '请选择离职日期';
      }
      if (!this.preview) {
        return '先执行预检，确认影响范围';
      }
      if (!this.previewIsCurrent) {
        return '用户、集群或离职日期已变化，请重新预检';
      }
      if (!this.preview.executable) {
        return '预检存在阻断项，请处理后再创建任务';
      }
      return '等待前置条件完成';
    },
    actionHint() {
      return this.canCreateTask ? '预检已通过，可以创建回收任务。' : this.createDisabledReason;
    },
    nextActionText() {
      if (!this.selectedUser) {
        return '选择离职用户';
      }
      if (!this.hasClusterContext) {
        return '确认所属集群';
      }
      if (!this.departureDate) {
        return '选择离职日期';
      }
      if (!this.preview) {
        return '预检影响范围';
      }
      if (!this.previewIsCurrent) {
        return '重新预检';
      }
      if (!this.preview.executable) {
        return '处理预检阻断项';
      }
      return '创建回收任务';
    },
    readinessChecks() {
      const hasUser = Boolean(this.selectedUser);
      const hasCluster = this.hasClusterContext;
      const hasDepartureDate = Boolean(this.departureDate);
      const hasPreview = Boolean(this.preview);
      const previewCurrent = Boolean(this.preview && this.previewIsCurrent);
      const previewExecutable = Boolean(previewCurrent && this.preview && this.preview.executable);
      return [
        {
          key: 'user',
          label: '处置对象',
          detail: hasUser ? this.selectedUser.username : '未选择',
          done: hasUser,
          active: !hasUser
        },
        {
          key: 'cluster',
          label: '所属集群',
          detail: hasCluster ? (this.selectedUser.clusterName || this.form.cluster) : '待确认',
          done: hasCluster,
          active: hasUser && !hasCluster
        },
        {
          key: 'date',
          label: '离职日期',
          detail: hasDepartureDate ? this.departureDate : '未选择',
          done: hasDepartureDate,
          active: hasCluster && !hasDepartureDate
        },
        {
          key: 'preview',
          label: '影响预检',
          detail: previewExecutable ? '通过' : (hasPreview && !previewCurrent ? '已过期' : (hasPreview ? '有阻断项' : '未预检')),
          done: previewExecutable,
          active: hasDepartureDate && !previewExecutable
        }
      ];
    },
    pendingTaskCount() {
      return this.tasks.filter(task => ['PENDING', 'RUNNING'].includes(String(task.status || '').toUpperCase())).length;
    },
    failedTaskCount() {
      return this.tasks.filter(task => String(task.status || '').toUpperCase() === 'FAILED').length;
    },
    dueTodayCount() {
      const end = moment().endOf('day');
      return this.tasks.filter(task => {
        const status = String(task.status || '').toUpperCase();
        return status === 'PENDING' && task.scheduledAt && moment(task.scheduledAt).isSameOrBefore(end);
      }).length;
    },
    activeConfirmation() {
      if (this.result) {
        return this.result;
      }
      if (!this.selectedTask || !this.selectedTask.confirmationText) {
        return null;
      }
      return {
        taskStatus: this.selectedTask.status,
        taskNo: this.selectedTask.taskNo,
        confirmationNo: this.selectedTask.confirmationNo || this.selectedTask.taskNo,
        scheduledAt: this.selectedTask.scheduledAt,
        archiveName: this.selectedTask.archiveName,
        confirmationText: this.selectedTask.confirmationText,
        notificationRecipients: this.taskRecipients(this.selectedTask)
      };
    },
    selectedNativeEngine() {
      const parts = [
        this.selectedUser && this.selectedUser.creationStrategy,
        this.selectedUser && this.selectedUser.clusterName,
        this.form.cluster
      ];
      const value = parts.filter(Boolean).join(' ').toUpperCase();
      if (value.includes('DORIS')) {
        return 'Doris';
      }
      if (value.includes('STARROCK') || value === 'SR') {
        return 'StarRocks';
      }
      return null;
    },
    defaultSteps() {
      if (!this.selectedNativeEngine) {
        return DEFAULT_STEPS;
      }
      return COMMON_NATIVE_STEPS.map(step => {
        if (step.key === 'native-user-check') {
          return { ...step, title: `${this.selectedNativeEngine} 原生账号校验` };
        }
        if (step.key === 'native-account-drop') {
          return { ...step, title: `${this.selectedNativeEngine} 原生账号删除与权限撤销` };
        }
        return step;
      });
    },
    renderedSteps() {
      if (!this.result || !Array.isArray(this.result.steps)) {
        if (this.preview && Array.isArray(this.preview.plannedSteps) && this.preview.plannedSteps.length) {
          return this.preview.plannedSteps;
        }
        return this.defaultSteps;
      }
      if (this.result.steps.length) {
        return this.result.steps;
      }
      return this.defaultSteps;
    }
  },
  mounted() {
    this.fetchClusters();
    this.searchUsers();
    this.fetchTasks();
  },
  methods: {
    refreshAll() {
      this.searchUsers();
      this.fetchTasks();
    },
    async fetchClusters() {
      try {
        const res = await axios.get('/api/clusters');
        this.clusters = res.data || [];
      } catch (e) {
        this.$message.error('加载集群失败');
      }
    },
    async fetchTasks() {
      this.loadingTasks = true;
      try {
        const params = { limit: 30 };
        if (this.taskStatusFilter && this.taskStatusFilter !== 'ALL') {
          params.status = this.taskStatusFilter;
        }
        const res = await axios.get('/api/access/offboarding/revocations', { params });
        this.tasks = res.data || [];
        if (this.selectedTask && !this.tasks.some(task => task.taskId === this.selectedTask.taskId)) {
          this.selectedTask = null;
        }
      } catch (e) {
        this.tasks = [];
        this.$message.error(e.response?.data?.message || '加载离职回收任务失败');
      } finally {
        this.loadingTasks = false;
      }
    },
    handleTaskFilterChange() {
      this.selectedTask = null;
      this.fetchTasks();
    },
    async selectTask(task) {
      if (!task || !task.taskId) {
        return;
      }
      this.loadingTaskDetail = true;
      try {
        const res = await axios.get(`/api/access/offboarding/revocations/${task.taskId}`);
        this.selectedTask = res.data || task;
        this.result = null;
      } catch (e) {
        this.selectedTask = task;
        this.$message.error(e.response?.data?.message || '加载任务详情失败');
      } finally {
        this.loadingTaskDetail = false;
      }
    },
    async searchUsers() {
      this.loadingUsers = true;
      try {
        const params = {
          page: 0,
          size: 8,
          q: this.keyword || undefined,
          cluster: this.form.cluster || undefined
        };
        const res = await axios.get('/api/access/users', { params });
        const data = res.data || {};
        this.users = data.content || [];
        if (this.selectedUser && !this.users.some(user => this.sameUser(user, this.selectedUser))) {
          this.selectedUser = null;
        }
      } catch (e) {
        this.users = [];
        this.selectedUser = null;
        this.$message.error(e.response?.data?.message || '后端服务不可用，请确认 8081 已启动');
      } finally {
        this.loadingUsers = false;
      }
    },
    handleClusterChange() {
      this.selectedUser = null;
      this.result = null;
      this.resetPreview();
      this.users = [];
      this.searchUsers();
    },
    handleDepartureDateChange(date, dateString) {
      this.departureDate = date ? dateString : null;
      this.result = null;
      this.resetPreview();
    },
    selectUser(record) {
      this.selectedUser = record;
      this.result = null;
      this.selectedTask = null;
      this.resetPreview();
      if (!this.form.cluster && record.clusterName) {
        this.form.cluster = record.clusterName;
      }
    },
    resetPreview() {
      this.preview = null;
      this.previewPayloadKey = '';
    },
    buildPayload() {
      return {
        username: this.selectedUser ? this.selectedUser.username : undefined,
        cluster: this.selectedUser ? (this.selectedUser.clusterName || this.form.cluster) : this.form.cluster,
        departureDate: this.departureDate,
        managerContact: this.form.managerContact,
        securityContact: this.form.securityContact,
        reason: this.form.reason
      };
    },
    async fetchPreview() {
      if (!this.canPreview) {
        this.$message.warning('请选择用户和集群');
        return null;
      }
      this.previewing = true;
      try {
        const payload = this.buildPayload();
        const res = await axios.post('/api/access/offboarding/revocations/preview', payload);
        this.preview = res.data || null;
        this.previewPayloadKey = this.currentPreviewKey;
        this.result = null;
        if (this.preview && this.preview.executable) {
          this.$message.success('预检通过，可以创建回收任务');
        } else {
          this.$message.warning('预检存在阻断项，请处理后再创建任务');
        }
        return this.preview;
      } catch (e) {
        this.preview = null;
        this.previewPayloadKey = '';
        this.$message.error(e.response?.data?.message || '预检失败');
        return null;
      } finally {
        this.previewing = false;
      }
    },
    async confirmExecute() {
      if (!this.canExecute) {
        this.$message.warning('请选择用户和离职日期');
        return;
      }
      if (!this.previewIsCurrent || !this.preview) {
        await this.fetchPreview();
      }
      if (!this.canCreateTask) {
        this.$message.warning('请先完成预检并处理阻断项');
        return;
      }
      this.$confirm({
        title: '确认创建离职权限回收任务?',
        content: `${this.selectedUser.username} / ${this.selectedUser.clusterName || this.form.cluster} / ${this.departureDate} / ${this.preview.revocationFlow || '回收路径待确认'}`,
        okText: '创建任务',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => this.executeRevocation()
      });
    },
    async executeRevocation() {
      this.executing = true;
      try {
        const payload = this.buildPayload();
        const res = await axios.post('/api/access/offboarding/revocations', payload);
        this.result = res.data || null;
        this.$message.success(this.result && this.result.taskStatus === 'PENDING'
          ? '离职权限回收任务已创建'
          : '离职权限回收流程已执行');
        this.preview = null;
        this.previewPayloadKey = '';
        this.searchUsers();
        this.fetchTasks();
      } catch (e) {
        this.$message.error(e.response?.data?.message || '离职权限回收失败');
      } finally {
        this.executing = false;
      }
    },
    async copyConfirmation() {
      const confirmation = this.activeConfirmation;
      if (!confirmation || !confirmation.confirmationText) return;
      try {
        await navigator.clipboard.writeText(confirmation.confirmationText);
        this.$message.success('单据已复制');
      } catch (e) {
        this.$message.warning('当前浏览器不支持自动复制');
      }
    },
    sameUser(left, right) {
      return left && right
        && left.username === right.username
        && (left.clusterName || '') === (right.clusterName || '');
    },
    avatarColor(username) {
      const colors = ['#1677ff', '#13a8a8', '#2f54eb', '#fa8c16', '#52c41a'];
      let hash = 0;
      const value = username || '';
      for (let i = 0; i < value.length; i++) {
        hash = value.charCodeAt(i) + ((hash << 5) - hash);
      }
      return colors[Math.abs(hash) % colors.length];
    },
    formatDate(value) {
      return value ? moment(value).format('YYYY-MM-DD') : '-';
    },
    formatDateTime(value) {
      return value ? moment(value).format('YYYY-MM-DD HH:mm') : '-';
    },
    taskStatusClass(status) {
      return String(status || '').toLowerCase();
    },
    taskStatusColor(status) {
      const value = String(status || '').toUpperCase();
      if (value === 'COMPLETED') return 'green';
      if (value === 'FAILED') return 'red';
      if (value === 'RUNNING') return 'blue';
      if (value === 'PENDING') return 'orange';
      return 'default';
    },
    taskStatusText(status) {
      const labels = {
        PENDING: '待执行',
        RUNNING: '执行中',
        COMPLETED: '已完成',
        FAILED: '失败'
      };
      const value = String(status || '').toUpperCase();
      return labels[value] || (status || '未知');
    },
    taskRecipients(task) {
      const recipients = [];
      if (task.managerContact) recipients.push(task.managerContact);
      if (task.securityContact && !recipients.includes(task.securityContact)) {
        recipients.push(task.securityContact);
      }
      return recipients;
    },
    sourceLabel(strategy) {
      const value = String(strategy || '').toUpperCase();
      const labels = {
        OPENLDAP: 'OpenLDAP',
        LDAP: 'LDAP',
        LDAP_IMPORT: 'LDAP 导入',
        STARROCKS: 'StarRocks',
        STARROCKS_IMPORT: 'StarRocks 导入',
        DORIS: 'Doris',
        DORIS_IMPORT: 'Doris 导入',
        LIVE_AUTH_BACKEND: '授权后端'
      };
      return labels[value] || (strategy || '未知来源');
    },
    stepIcon(status) {
      if (status === 'SUCCESS') return 'check';
      if (status === 'FAILED') return 'close';
      if (status === 'PENDING') return 'clock-circle';
      return 'minus';
    },
    stepColor(status) {
      if (status === 'SUCCESS') return 'green';
      if (status === 'FAILED') return 'red';
      if (status === 'PENDING') return 'orange';
      return 'default';
    },
    stepStatusText(status) {
      const labels = {
        SUCCESS: '完成',
        FAILED: '失败',
        PENDING: '待处理',
        WAIT: '待执行'
      };
      return labels[status] || status;
    }
  }
};
</script>

<style scoped>
.offboarding-page {
  min-height: calc(100vh - 160px);
  padding: 8px 10px 18px;
  color: #1f2937;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.title-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e8f3ff;
  color: #1677ff;
  font-size: 20px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-pill {
  min-width: 92px;
  height: 34px;
  padding: 4px 10px;
  border: 1px solid #e5eaf0;
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
}

.stat-value {
  color: #1677ff;
  font-size: 16px;
  font-weight: 700;
}

.stat-value.danger {
  color: #cf1322;
}

.stat-label {
  color: #6b7280;
  font-size: 12px;
}

.page-kicker {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

h2,
h3 {
  margin: 0;
  color: #111827;
  font-weight: 700;
}

.workbench {
  display: grid;
  grid-template-columns: minmax(520px, 1.15fr) minmax(420px, 0.85fr);
  gap: 14px;
  align-items: start;
}

.task-panel {
  padding: 14px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid #e5eaf0;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.user-panel,
.action-panel {
  background: #fff;
  border: 1px solid #e5eaf0;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.user-panel {
  padding: 14px;
}

.action-panel {
  padding: 14px;
  position: sticky;
  top: 12px;
}

.panel-head,
.flow-head,
.confirmation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head h3,
.flow-head h3 {
  font-size: 15px;
}

.panel-head span {
  color: #64748b;
  font-size: 12px;
}

.filter-strip {
  display: grid;
  grid-template-columns: minmax(160px, 220px) minmax(220px, 1fr) minmax(150px, 180px);
  gap: 10px;
  margin-bottom: 12px;
}

.cluster-select,
.keyword-input,
.date-input {
  width: 100%;
}

.user-list {
  min-height: 420px;
  display: grid;
  gap: 8px;
  align-content: start;
}

.user-list.loading,
.empty-state {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state.compact {
  min-height: 118px;
}

.task-list {
  min-height: 118px;
  display: grid;
  gap: 8px;
}

.task-list.loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-row {
  width: 100%;
  min-height: 58px;
  padding: 10px 12px 10px 10px;
  border: 1px solid #eef2f6;
  border-radius: 8px;
  background: #fff;
  display: grid;
  grid-template-columns: 4px minmax(170px, 0.72fr) minmax(260px, auto) minmax(220px, 1fr);
  gap: 12px;
  align-items: center;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.task-row:hover,
.task-row.selected {
  border-color: #91caff;
  background: #f7fbff;
}

.task-row.selected {
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.08);
}

.task-status-bar {
  width: 4px;
  height: 34px;
  border-radius: 4px;
  background: #cbd5e1;
}

.task-status-bar.pending {
  background: #faad14;
}

.task-status-bar.running {
  background: #1677ff;
}

.task-status-bar.completed {
  background: #52c41a;
}

.task-status-bar.failed {
  background: #ff4d4f;
}

.task-main {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.task-main strong {
  color: #111827;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-main em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.task-message {
  min-width: 0;
  color: #475569;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-detail-strip {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e5eaf0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.task-detail-strip span,
.task-detail-strip em {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.task-detail-strip strong {
  display: block;
  color: #111827;
  font-size: 13px;
  margin-top: 2px;
}

.task-detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user-row {
  width: 100%;
  min-height: 64px;
  padding: 10px 12px;
  border: 1px solid #eef2f6;
  border-left: 3px solid transparent;
  border-radius: 8px;
  background: #fff;
  display: grid;
  grid-template-columns: 36px minmax(160px, 1fr) minmax(220px, auto) 82px;
  gap: 10px;
  align-items: center;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.user-row:hover {
  border-color: #b9dcff;
  background: #f7fbff;
}

.user-row.selected {
  border-color: #91caff;
  border-left-color: #1677ff;
  background: #f0f8ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.08);
}

.user-main,
.user-meta,
.user-state {
  display: flex;
  align-items: center;
}

.user-main {
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
}

.user-main strong {
  max-width: 100%;
  color: #1f2937;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-main span {
  max-width: 100%;
  color: #64748b;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-meta {
  gap: 8px;
  justify-content: flex-start;
}

.user-state {
  justify-content: flex-end;
  gap: 6px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.state-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.state-dot.ready {
  background: #52c41a;
}

.state-dot.danger {
  background: #ff4d4f;
}

.selected-card {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 74px;
  padding: 12px;
  border-radius: 8px;
  background: #f7faff;
  border: 1px solid #dbeafe;
  margin-bottom: 12px;
}

.selected-card.empty {
  background: #f8fafc;
  border-color: #e5eaf0;
}

.selected-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: #1677ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.selected-card.empty .selected-avatar {
  background: #94a3b8;
}

.selected-copy {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.selected-copy span,
.selected-copy em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.selected-copy strong {
  color: #111827;
  font-size: 18px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-inspector {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #dbeafe;
  background: #f7fbff;
  border-left: 4px solid #1677ff;
}

.task-inspector.failed {
  border-color: #ffd8d6;
  border-left-color: #ff4d4f;
  background: #fff7f6;
}

.task-inspector.completed {
  border-color: #d9f7be;
  border-left-color: #52c41a;
  background: #f6ffed;
}

.task-inspector.pending {
  border-color: #ffe7ba;
  border-left-color: #faad14;
  background: #fffaf0;
}

.inspector-head,
.inspector-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.inspector-head span,
.inspector-grid span,
.readiness-head span,
.readiness-item em {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.inspector-head strong {
  display: block;
  color: #111827;
  font-size: 14px;
  margin-top: 2px;
  word-break: break-all;
}

.inspector-message {
  margin-top: 8px;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.inspector-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.inspector-grid > div {
  min-width: 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.inspector-grid strong {
  display: block;
  margin-top: 2px;
  color: #111827;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.readiness-card {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #e5eaf0;
  background: #f8fafc;
}

.readiness-card.ready {
  border-color: #b7eb8f;
  background: #f6ffed;
}

.readiness-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.readiness-head strong {
  color: #111827;
  font-size: 14px;
}

.readiness-checks {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.readiness-item {
  min-width: 0;
  min-height: 72px;
  padding: 9px 10px;
  border-radius: 8px;
  border: 1px solid #e5eaf0;
  background: #fff;
  color: #94a3b8;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 3px 6px;
  align-content: start;
}

.readiness-item.done {
  border-color: #b7eb8f;
  color: #389e0d;
}

.readiness-item.active {
  border-color: #91caff;
  color: #1677ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.08);
}

.readiness-item span {
  min-width: 0;
  color: #1f2937;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.readiness-item em {
  grid-column: 2;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.execution-form {
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2f6;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 10px;
}

.form-grid .ant-form-item:last-child {
  grid-column: span 2;
}

.execute-button {
  flex: 1;
  height: 38px;
}

.preview-button {
  flex: 1;
  height: 38px;
}

.button-row {
  display: flex;
  gap: 10px;
}

.button-shell {
  flex: 1;
  display: inline-flex;
}

.button-shell .ant-btn {
  width: 100%;
}

.action-hint {
  min-height: 28px;
  margin-top: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.preview-panel {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f7fbff;
}

.preview-panel.blocked {
  border-color: #ffd8bf;
  background: #fffaf2;
}

.preview-panel.stale {
  border-color: #e5eaf0;
  background: #f8fafc;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-head h3 {
  font-size: 15px;
}

.preview-head span {
  color: #64748b;
  font-size: 12px;
}

.preview-metrics {
  display: grid;
  grid-template-columns: 1.2fr 0.7fr 0.7fr;
  gap: 8px;
  margin-bottom: 10px;
}

.preview-metrics > div {
  min-width: 0;
  padding: 9px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #eef2f6;
}

.preview-metrics span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.preview-metrics strong {
  display: block;
  color: #111827;
  font-size: 13px;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-list {
  display: grid;
  gap: 6px;
}

.alert-list + .alert-list {
  margin-top: 8px;
}

.alert-list > div {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: 7px;
  align-items: start;
  font-size: 12px;
  line-height: 1.45;
}

.alert-list.danger {
  color: #cf1322;
}

.alert-list.warning {
  color: #ad6800;
}

.flow-panel {
  padding-top: 12px;
}

.flow-step {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 9px;
  align-items: start;
  padding: 9px 0;
  border-bottom: 1px solid #eef2f6;
}

.flow-step:last-child {
  border-bottom: none;
}

.flow-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef3f8;
  color: #64748b;
  font-size: 12px;
}

.flow-step.is-success .flow-icon {
  background: #f0ffe9;
  color: #389e0d;
}

.flow-step.is-failed .flow-icon {
  background: #fff1f0;
  color: #cf1322;
}

.flow-step.is-pending .flow-icon {
  background: #fff7e6;
  color: #d46b08;
}

.flow-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #1f2937;
  font-weight: 600;
  line-height: 1.4;
}

.flow-message {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.confirmation-panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eef2f6;
}

.confirmation-head span,
.confirmation-grid span {
  color: #64748b;
  font-size: 12px;
}

.confirmation-head strong,
.confirmation-grid strong {
  display: block;
  color: #1f2937;
  font-size: 13px;
  margin-top: 2px;
  word-break: break-all;
}

.confirmation-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.confirmation-text {
  max-height: 180px;
  overflow: auto;
  margin: 10px 0 0;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

::v-deep .ant-form-item {
  margin-bottom: 10px;
}

::v-deep .ant-form-item-label {
  line-height: 1.2;
  padding-bottom: 5px;
}

::v-deep .ant-form-item-label > label {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

::v-deep .ant-tag {
  margin-right: 0;
  border-radius: 4px;
}

@media (max-width: 960px) {
  .page-toolbar,
  .toolbar-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .workbench,
  .filter-strip,
  .form-grid,
  .confirmation-grid,
  .inspector-grid,
  .readiness-checks,
  .preview-metrics {
    grid-template-columns: 1fr;
  }

  .task-row {
    grid-template-columns: 4px minmax(0, 1fr);
  }

  .task-meta,
  .task-message {
    grid-column: 2;
  }

  .task-detail-strip,
  .button-row {
    align-items: stretch;
    flex-direction: column;
  }

  .action-panel {
    position: static;
  }

  .form-grid .ant-form-item:last-child {
    grid-column: span 1;
  }

  .user-row {
    grid-template-columns: 36px minmax(0, 1fr);
  }

  .user-meta,
  .user-state {
    grid-column: 2;
    justify-content: flex-start;
  }
}
</style>
