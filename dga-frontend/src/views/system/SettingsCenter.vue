<template>
  <div class="settings-center">
    <div class="settings-header">
      <div>
        <h1>设置中心</h1>
        <p>管理个人偏好、系统参数、数据地图、元数据采集和通知告警配置</p>
      </div>
      <a-button icon="reload" :loading="loading" @click="loadSettings">刷新</a-button>
    </div>

    <a-alert
      v-if="!canManageSystem"
      class="settings-alert"
      type="info"
      show-icon
      message="当前账号可修改个人设置"
      description="系统参数、数据地图设置、元数据采集设置和通知告警设置仅 admin 或超级管理员可修改。"
    />

    <a-spin :spinning="loading">
      <div class="settings-shell">
        <a-tabs :active-key="activeTab" tab-position="left" @change="activeTab = $event">
          <a-tab-pane key="personal" tab="个人设置">
            <section class="settings-panel">
              <div class="panel-heading">
                <div>
                  <h2>个人设置</h2>
                  <p>这些配置只影响当前登录账号。密码修改继续在个人中心处理。</p>
                </div>
                <a-button type="primary" icon="save" :loading="savingGroup === 'personal'" @click="savePersonal">
                  保存个人设置
                </a-button>
              </div>
              <a-form-model layout="vertical" :model="personalForm" class="settings-form">
                <a-row :gutter="16">
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="默认首页">
                      <a-select v-model="personalForm.defaultHome">
                        <a-select-option value="/datamap">数据地图</a-select-option>
                        <a-select-option value="/metadata">元数据管理</a-select-option>
                        <a-select-option value="/quality">数据质量</a-select-option>
                        <a-select-option value="/resources">资源导航</a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="表格密度">
                      <a-select v-model="personalForm.tableDensity">
                        <a-select-option value="default">默认</a-select-option>
                        <a-select-option value="middle">紧凑</a-select-option>
                        <a-select-option value="small">极简</a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="默认分页">
                      <a-input-number v-model="personalForm.defaultPageSize" :min="5" :max="100" style="width: 100%" />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="个人显示偏好">
                      <div class="switch-line">
                        <a-switch v-model="personalForm.showHints" />
                        <span>显示页面提示与说明</span>
                      </div>
                    </a-form-model-item>
                  </a-col>
                </a-row>
              </a-form-model>
            </section>
          </a-tab-pane>

          <a-tab-pane key="system" tab="系统参数">
            <section class="settings-panel">
              <div class="panel-heading">
                <div>
                  <h2>系统参数</h2>
                  <p>控制平台名称、页脚、维护公告和默认分页等基础行为。</p>
                </div>
                <a-button
                  type="primary"
                  icon="save"
                  :disabled="!canManageSystem"
                  :loading="savingGroup === 'system'"
                  @click="saveSystemGroup('system')"
                >
                  保存系统参数
                </a-button>
              </div>
              <a-form-model layout="vertical" :model="systemForm" class="settings-form">
                <a-row :gutter="16">
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="系统名称">
                      <a-input v-model="systemForm.systemName" :disabled="!canManageSystem" placeholder="DGA Platform" />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="默认分页大小">
                      <a-input-number
                        v-model="systemForm.defaultPageSize"
                        :disabled="!canManageSystem"
                        :min="5"
                        :max="100"
                        style="width: 100%"
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="AI 搜索开关">
                      <div class="switch-line">
                        <a-switch v-model="systemForm.aiSearchEnabled" :disabled="!canManageSystem" />
                        <span>{{ systemForm.aiSearchEnabled ? '已启用' : '未启用' }}</span>
                      </div>
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24">
                    <a-form-model-item label="页脚文案">
                      <a-input v-model="systemForm.footerText" :disabled="!canManageSystem" />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24">
                    <a-form-model-item label="维护公告">
                      <a-textarea
                        v-model="systemForm.maintenanceNotice"
                        :disabled="!canManageSystem"
                        :auto-size="{ minRows: 3, maxRows: 5 }"
                        placeholder="为空时不展示公告"
                      />
                    </a-form-model-item>
                  </a-col>
                </a-row>
              </a-form-model>
            </section>
          </a-tab-pane>

          <a-tab-pane key="dataMap" tab="数据地图设置">
            <section class="settings-panel">
              <div class="panel-heading">
                <div>
                  <h2>数据地图设置</h2>
                  <p>配置首页搜索词、搜索范围、最近浏览保留时间和结果展示方式。</p>
                </div>
                <a-button
                  type="primary"
                  icon="save"
                  :disabled="!canManageSystem"
                  :loading="savingGroup === 'dataMap'"
                  @click="saveSystemGroup('dataMap')"
                >
                  保存数据地图设置
                </a-button>
              </div>
              <a-form-model layout="vertical" :model="dataMapForm" class="settings-form">
                <a-row :gutter="16">
                  <a-col :xs="24">
                    <a-form-model-item label="热门搜索词">
                      <a-select
                        v-model="dataMapForm.hotKeywords"
                        mode="tags"
                        :disabled="!canManageSystem"
                        :token-separators="[',', '，']"
                        placeholder="输入后回车，可配置多个关键词"
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="默认搜索范围">
                      <a-select v-model="dataMapForm.defaultSearchScope" :disabled="!canManageSystem">
                        <a-select-option value="ALL">全部资产</a-select-option>
                        <a-select-option value="TABLE">表名</a-select-option>
                        <a-select-option value="COLUMN">字段</a-select-option>
                        <a-select-option value="BUSINESS">业务元数据</a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="搜索结果排序">
                      <a-select v-model="dataMapForm.resultSort" :disabled="!canManageSystem">
                        <a-select-option value="RELEVANCE">相关度优先</a-select-option>
                        <a-select-option value="SYNC_TIME_DESC">最近同步优先</a-select-option>
                        <a-select-option value="NAME_ASC">表名升序</a-select-option>
                      </a-select>
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="最近浏览保留天数">
                      <a-input-number
                        v-model="dataMapForm.recentRetentionDays"
                        :disabled="!canManageSystem"
                        :min="1"
                        :max="365"
                        style="width: 100%"
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="默认分页大小">
                      <a-input-number
                        v-model="dataMapForm.defaultPageSize"
                        :disabled="!canManageSystem"
                        :min="5"
                        :max="100"
                        style="width: 100%"
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="敏感字段展示">
                      <div class="switch-line">
                        <a-switch v-model="dataMapForm.showSensitiveFields" :disabled="!canManageSystem" />
                        <span>{{ dataMapForm.showSensitiveFields ? '展示负责人等敏感信息' : '隐藏敏感信息' }}</span>
                      </div>
                    </a-form-model-item>
                  </a-col>
                </a-row>
              </a-form-model>
            </section>
          </a-tab-pane>

          <a-tab-pane key="metadataCollection" tab="元数据采集设置">
            <section class="settings-panel">
              <div class="panel-heading">
                <div>
                  <h2>元数据采集设置</h2>
                  <p>控制 Hive 元数据采集调度、分区采集上限、失败重试和数据源同步。</p>
                </div>
                <a-button
                  type="primary"
                  icon="save"
                  :disabled="!canManageSystem"
                  :loading="savingGroup === 'metadataCollection'"
                  @click="saveSystemGroup('metadataCollection')"
                >
                  保存采集设置
                </a-button>
              </div>
              <a-form-model layout="vertical" :model="metadataCollectionForm" class="settings-form">
                <a-row :gutter="16">
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="采集 Cron">
                      <a-input
                        v-model="metadataCollectionForm.collectCron"
                        :disabled="!canManageSystem"
                        placeholder="0 0 2 * * ?"
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="分区采集上限">
                      <a-input-number
                        v-model="metadataCollectionForm.partitionLatestLimit"
                        :disabled="!canManageSystem"
                        :min="1"
                        :max="5000"
                        style="width: 100%"
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="失败重试次数">
                      <a-input-number
                        v-model="metadataCollectionForm.retryTimes"
                        :disabled="!canManageSystem"
                        :min="0"
                        :max="10"
                        style="width: 100%"
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="自动同步数据源">
                      <div class="switch-line">
                        <a-switch v-model="metadataCollectionForm.autoSyncDataSources" :disabled="!canManageSystem" />
                        <span>{{ metadataCollectionForm.autoSyncDataSources ? '采集前自动同步' : '仅采集已纳管数据源' }}</span>
                      </div>
                    </a-form-model-item>
                  </a-col>
                </a-row>
              </a-form-model>
            </section>
          </a-tab-pane>

          <a-tab-pane key="notification" tab="通知告警设置">
            <section class="settings-panel">
              <div class="panel-heading">
                <div>
                  <h2>通知告警设置</h2>
                  <p>第一版支持企业微信 Webhook，用于采集失败和质量异常通知。</p>
                </div>
                <div class="button-group">
                  <a-button
                    icon="message"
                    :disabled="!canManageSystem || !notificationForm.wecomEnabled"
                    :loading="testingWecom"
                    @click="testWecom"
                  >
                    测试发送
                  </a-button>
                  <a-button
                    type="primary"
                    icon="save"
                    :disabled="!canManageSystem"
                    :loading="savingGroup === 'notification'"
                    @click="saveSystemGroup('notification')"
                  >
                    保存告警设置
                  </a-button>
                </div>
              </div>
              <a-form-model layout="vertical" :model="notificationForm" class="settings-form">
                <a-row :gutter="16">
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="启用通知">
                      <div class="switch-line">
                        <a-switch v-model="notificationForm.wecomEnabled" :disabled="!canManageSystem" />
                        <span>{{ notificationForm.wecomEnabled ? '企业微信通知已启用' : '企业微信通知未启用' }}</span>
                      </div>
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="采集失败告警">
                      <div class="switch-line">
                        <a-switch v-model="notificationForm.collectFailureAlert" :disabled="!canManageSystem" />
                        <span>{{ notificationForm.collectFailureAlert ? '发送' : '不发送' }}</span>
                      </div>
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24">
                    <a-form-model-item label="企业微信 Webhook">
                      <a-input-password
                        v-model="notificationForm.wecomWebhook"
                        :disabled="!canManageSystem"
                        placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
                      />
                    </a-form-model-item>
                  </a-col>
                  <a-col :xs="24" :md="12">
                    <a-form-model-item label="质量异常告警">
                      <div class="switch-line">
                        <a-switch v-model="notificationForm.qualityIssueAlert" :disabled="!canManageSystem" />
                        <span>{{ notificationForm.qualityIssueAlert ? '发送' : '不发送' }}</span>
                      </div>
                    </a-form-model-item>
                  </a-col>
                </a-row>
              </a-form-model>
            </section>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-spin>
  </div>
</template>

<script>
import axios from 'axios';
import { canDelete } from '../../utils/currentUser';

const defaults = {
  personal: {
    defaultHome: '/datamap',
    tableDensity: 'middle',
    defaultPageSize: 10,
    showHints: true
  },
  system: {
    systemName: 'DGA Platform',
    footerText: 'DGA Platform ©2026 Created by Data Engineering Team',
    maintenanceNotice: '',
    aiSearchEnabled: false,
    defaultPageSize: 10
  },
  dataMap: {
    hotKeywords: ['user', 'order', 'payment'],
    defaultSearchScope: 'ALL',
    recentRetentionDays: 30,
    resultSort: 'RELEVANCE',
    showSensitiveFields: false,
    defaultPageSize: 10
  },
  metadataCollection: {
    collectCron: '0 0 2 * * ?',
    partitionLatestLimit: 200,
    retryTimes: 1,
    autoSyncDataSources: true
  },
  notification: {
    wecomEnabled: false,
    wecomWebhook: '',
    collectFailureAlert: true,
    qualityIssueAlert: true
  }
};

export default {
  name: 'SettingsCenter',
  data() {
    return {
      loading: false,
      savingGroup: '',
      testingWecom: false,
      activeTab: 'personal',
      canManageSystem: canDelete(),
      personalForm: { ...defaults.personal },
      systemForm: { ...defaults.system },
      dataMapForm: { ...defaults.dataMap },
      metadataCollectionForm: { ...defaults.metadataCollection },
      notificationForm: { ...defaults.notification }
    };
  },
  created() {
    this.loadSettings();
  },
  methods: {
    async loadSettings() {
      this.loading = true;
      try {
        const [systemRes, personalRes] = await Promise.all([
          axios.get('/api/settings'),
          axios.get('/api/settings/me')
        ]);
        this.applySystemSettings(systemRes.data || {});
        this.personalForm = this.mergeGroup('personal', personalRes.data || {});
      } catch (e) {
        this.$message.error(e.response?.data?.message || '加载设置失败');
      } finally {
        this.loading = false;
      }
    },
    mergeGroup(group, values) {
      const merged = { ...defaults[group], ...(values || {}) };
      if (group === 'dataMap') {
        merged.hotKeywords = this.normalizeArray(merged.hotKeywords);
      }
      return merged;
    },
    normalizeArray(value) {
      if (Array.isArray(value)) {
        return value.filter(Boolean);
      }
      if (!value) {
        return [];
      }
      return String(value).split(/[,，]/).map(item => item.trim()).filter(Boolean);
    },
    applySystemSettings(settings) {
      this.systemForm = this.mergeGroup('system', settings.system);
      this.dataMapForm = this.mergeGroup('dataMap', settings.dataMap);
      this.metadataCollectionForm = this.mergeGroup('metadataCollection', settings.metadataCollection);
      this.notificationForm = this.mergeGroup('notification', settings.notification);
    },
    async savePersonal() {
      this.savingGroup = 'personal';
      try {
        const res = await axios.put('/api/settings/me', this.personalForm);
        this.personalForm = this.mergeGroup('personal', res.data || {});
        this.$message.success('个人设置已保存');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '保存个人设置失败');
      } finally {
        this.savingGroup = '';
      }
    },
    async saveSystemGroup(group) {
      if (!this.canManageSystem) {
        this.$message.warning('仅 admin 或超级管理员可修改系统设置');
        return;
      }
      this.savingGroup = group;
      try {
        const payload = this.getGroupPayload(group);
        const res = await axios.put(`/api/settings/${group}`, payload);
        this.applySystemSettings(res.data || {});
        this.$message.success('设置已保存');
      } catch (e) {
        this.$message.error(e.response?.data?.message || '保存设置失败');
      } finally {
        this.savingGroup = '';
      }
    },
    getGroupPayload(group) {
      if (group === 'system') {
        return { ...this.systemForm };
      }
      if (group === 'dataMap') {
        return {
          ...this.dataMapForm,
          hotKeywords: this.normalizeArray(this.dataMapForm.hotKeywords)
        };
      }
      if (group === 'metadataCollection') {
        return { ...this.metadataCollectionForm };
      }
      return { ...this.notificationForm };
    },
    async testWecom() {
      if (!this.canManageSystem) {
        this.$message.warning('仅 admin 或超级管理员可测试通知告警');
        return;
      }
      this.testingWecom = true;
      try {
        await axios.post('/api/settings/notifications/wecom/test');
        this.$message.success('企业微信测试消息已发送');
      } catch (e) {
        this.$message.error(e.response?.data?.message || e.response?.data || '企业微信测试发送失败');
      } finally {
        this.testingWecom = false;
      }
    }
  }
};
</script>

<style scoped>
.settings-center {
  min-height: calc(100vh - 160px);
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.settings-header h1 {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 24px;
  font-weight: 650;
  letter-spacing: 0;
}

.settings-header p {
  margin: 0;
  color: #667085;
}

.settings-alert {
  margin-bottom: 16px;
}

.settings-shell {
  min-height: 560px;
  border: 1px solid #eeeeee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.settings-shell >>> .ant-tabs {
  min-height: 560px;
}

.settings-shell >>> .ant-tabs-left-bar {
  width: 200px;
  margin-right: 0;
  border-right: 1px solid #f0f0f0;
}

.settings-shell >>> .ant-tabs-left-content {
  min-height: 560px;
  padding: 0;
  border-left: 0;
}

.settings-shell >>> .ant-tabs-tab {
  height: 44px;
  margin: 4px 8px !important;
  padding: 0 16px !important;
  border-radius: 6px;
  line-height: 44px;
  text-align: left;
}

.settings-shell >>> .ant-tabs-tab-active {
  background: #e6f7ff;
}

.settings-panel {
  padding: 24px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-heading h2 {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 650;
}

.panel-heading p {
  max-width: 720px;
  margin: 0;
  color: #667085;
  line-height: 1.6;
}

.settings-form {
  max-width: 920px;
}

.switch-line {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 32px;
  color: #475467;
}

.button-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .settings-header,
  .panel-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .settings-shell >>> .ant-tabs-left-bar {
    width: 160px;
  }
}

@media (max-width: 640px) {
  .settings-shell >>> .ant-tabs {
    display: block;
  }

  .settings-shell >>> .ant-tabs-left-bar {
    float: none;
    width: 100%;
    border-right: 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .settings-shell >>> .ant-tabs-left-content {
    margin-left: 0;
  }

  .settings-shell >>> .ant-tabs-tab {
    display: inline-block;
    width: auto;
  }

  .settings-panel {
    padding: 18px;
  }
}
</style>
