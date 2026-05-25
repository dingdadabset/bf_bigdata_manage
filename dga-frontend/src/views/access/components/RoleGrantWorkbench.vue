<template>
  <a-card :bordered="false" class="workbench-card">
    <div class="workspace-header">
      <div>
        <div class="workspace-eyebrow">{{ workspaceEyebrow }}</div>
        <div class="workspace-title">{{ roleDisplayName(selectedRoleView) }}</div>
        <div class="workspace-subtitle">{{ workspaceSubtitle }}</div>
      </div>
      <div v-if="showRoleManagement" class="workspace-actions">
        <a-tag v-if="role" :color="role.status === 'ACTIVE' ? 'green' : 'default'">
          {{ role.status || 'UNKNOWN' }}
        </a-tag>
        <a-button size="small" icon="edit" :disabled="!role" @click="$emit('edit-role')">
          编辑角色
        </a-button>
        <a-popconfirm
          v-if="role"
          title="确认删除该角色？本操作只软删除 DGA 角色库记录，不自动回收所有后端权限。"
          ok-text="删除"
          cancel-text="取消"
          @confirm="$emit('delete-role')"
        >
          <a-button size="small" type="danger" icon="delete">删除角色</a-button>
        </a-popconfirm>
      </div>
    </div>

    <div class="summary-banner">
      <div class="summary-item">
        <span>当前角色</span>
        <strong>{{ roleDisplayName(selectedRoleView) }}</strong>
      </div>
      <div class="summary-item">
        <span>{{ showAuthorization ? '授权对象' : '角色状态' }}</span>
        <strong>{{ showAuthorization ? currentSubjectLabel : (role ? (role.status || 'UNKNOWN') : '-') }}</strong>
      </div>
      <div class="summary-item">
        <span>{{ showAuthorization ? '绑定状态' : '当前模式' }}</span>
        <strong>{{ showAuthorization ? (hasCurrentBinding ? '已绑定' : '未绑定') : operationModeLabel(state.mode) }}</strong>
      </div>
      <div class="summary-item">
        <span>已选权限</span>
        <strong>{{ selectedPermissionCount }}</strong>
      </div>
      <div v-if="showAuthorization" class="summary-item">
        <span>校验用户</span>
        <strong>{{ verificationUser || '未选择' }}</strong>
      </div>
    </div>

    <a-spin v-if="showAuthorization && state.subjectName" :spinning="subjectContext.loading" class="subject-context-spin">
      <div class="subject-context-panel">
        <div class="subject-context-head">
          <div>
            <div class="panel-section-title">当前主体上下文</div>
            <span>展示该用户/组已绑定角色、LDAP 组关系与当前权限，避免重复授权或误判多角色关系。</span>
          </div>
          <a-tag color="blue">{{ currentSubjectLabel }}</a-tag>
        </div>
        <div class="subject-context-grid">
          <div class="context-block">
            <span>已绑定角色</span>
            <strong>{{ subjectBoundRoles.length }}</strong>
            <div class="tag-row">
              <a-tag
                v-for="item in visibleSubjectBoundRoles"
                :key="subjectRoleKey(item)"
                :color="subjectRoleTagColor(item)"
              >
                {{ subjectRoleTagText(item) }}
              </a-tag>
              <a-tag v-if="subjectBoundRoles.length > visibleSubjectBoundRoles.length">+{{ subjectBoundRoles.length - visibleSubjectBoundRoles.length }}</a-tag>
              <span v-if="!subjectBoundRoles.length" class="muted-text">暂无绑定</span>
            </div>
          </div>
          <div class="context-block">
            <span>LDAP 组关系</span>
            <strong>{{ subjectGroupNames.length }}</strong>
            <div class="tag-row">
              <a-tag
                v-for="item in subjectGroupNames.slice(0, 5)"
                :key="item"
                :color="item === primaryGroupName ? 'blue' : 'default'"
              >
                {{ item }}{{ item === primaryGroupName ? ' · 主组' : '' }}
              </a-tag>
              <a-tag v-if="subjectGroupNames.length > 5">+{{ subjectGroupNames.length - 5 }}</a-tag>
              <span v-if="!subjectGroupNames.length" class="muted-text">未读取到 LDAP 组</span>
            </div>
          </div>
          <div class="context-block">
            <span>{{ permissionContextLabel }}</span>
            <strong>{{ subjectPermissionTotal }}</strong>
            <div class="tag-row">
              <a-tag color="green">一致 {{ permissionSummary.matched }}</a-tag>
              <a-tag color="orange">仅 live {{ permissionSummary.liveOnly }}</a-tag>
              <a-tag color="blue">仅 recorded {{ permissionSummary.recordedOnly }}</a-tag>
              <a-tag color="default">live {{ livePermissionCount }}</a-tag>
              <a-tag color="default">recorded {{ recordedPermissionCount }}</a-tag>
            </div>
          </div>
        </div>
        <div v-if="permissionPreviewRows.length" class="permission-preview">
          <div class="permission-preview-head">
            <div>
              <div class="panel-section-title">权限预览</div>
              <span>{{ permissionPreviewHint }}</span>
            </div>
            <a-tag color="blue">Top {{ permissionPreviewRows.length }}</a-tag>
          </div>
          <div class="permission-preview-list">
            <div v-for="item in permissionPreviewRows" :key="item.key" class="permission-preview-row">
              <span class="permission-preview-text">{{ previewPermissionText(item) }}</span>
              <a-tag :color="verificationStatusColor(item.status)">{{ previewStatusLabel(item.status) }}</a-tag>
            </div>
          </div>
        </div>
      </div>
    </a-spin>

    <a-alert
      v-if="showAuthorization && !state.subjectName"
      class="workspace-alert"
      type="info"
      show-icon
      message="请先在顶部步骤 1 选择用户或组主体，再执行绑定、授权与校验。"
    />
    <a-alert
      v-else-if="showAuthorization && !role"
      class="workspace-alert"
      type="info"
      show-icon
      message="请先在左侧步骤 2 选择角色。"
    />
    <a-alert
      v-else-if="requiresVerificationUser && !verificationUser"
      class="workspace-alert"
      type="warning"
      show-icon
      message="组选中时请先指定验证用户，再执行角色内授权与权限校验。"
    />

    <a-spin :spinning="loading.roleDetail">
      <a-tabs v-if="showRoleManagement" :active-key="activeTab" @change="$emit('change-tab', $event)">
        <a-tab-pane key="info" tab="角色信息">
          <div v-if="role" class="tab-panel">
            <a-descriptions size="small" :column="2" class="detail-desc">
              <a-descriptions-item label="角色编码">{{ role.roleCode || '-' }}</a-descriptions-item>
              <a-descriptions-item label="角色名称">{{ role.roleName || role.roleCode || '-' }}</a-descriptions-item>
              <a-descriptions-item label="后端">{{ role.authBackend || state.selectedAuthBackend || '-' }}</a-descriptions-item>
              <a-descriptions-item label="引擎">{{ role.engineType || capabilityEngineType || '-' }}</a-descriptions-item>
              <a-descriptions-item label="Owner">{{ role.owner || '-' }}</a-descriptions-item>
              <a-descriptions-item label="风险级别">{{ role.riskLevel || '-' }}</a-descriptions-item>
              <a-descriptions-item label="过期时间">{{ role.expiresAt || '长期有效' }}</a-descriptions-item>
              <a-descriptions-item label="描述">{{ role.description || '-' }}</a-descriptions-item>
            </a-descriptions>

            <div class="info-metrics">
              <div class="metric-card">
                <span>权限范围</span>
                <strong>{{ rolePermissions.length }}</strong>
              </div>
              <div class="metric-card">
                <span>当前绑定</span>
                <strong>{{ assignments.length }}</strong>
              </div>
              <div class="metric-card">
                <span>授权对象</span>
                <strong>{{ state.subjectName || '未选择' }}</strong>
              </div>
            </div>

            <div class="binding-overview">
              <div class="binding-overview-head">
                <div>
                  <div class="panel-section-title">绑定对象概览</div>
                  <span>展示当前角色已经绑定的用户和组，完整明细可切到“绑定对象”Tab 筛选查看。</span>
                </div>
                <a-tag color="blue">{{ assignments.length }} 个绑定</a-tag>
              </div>
              <div v-if="assignments.length" class="binding-overview-grid">
                <div class="binding-overview-block">
                  <div class="binding-block-title">
                    <span>用户</span>
                    <a-tag>{{ userAssignments.length }}</a-tag>
                  </div>
                  <div class="binding-tag-row">
                    <a-tag
                      v-for="item in visibleUserAssignments"
                      :key="assignmentKey(item)"
                      :color="assignmentStatusColor(item.backendSyncStatus)"
                    >
                      {{ item.subjectName }}
                    </a-tag>
                    <a-tag v-if="userAssignments.length > visibleUserAssignments.length">
                      +{{ userAssignments.length - visibleUserAssignments.length }}
                    </a-tag>
                    <span v-if="!userAssignments.length" class="muted-text">暂无绑定用户</span>
                  </div>
                </div>
                <div class="binding-overview-block">
                  <div class="binding-block-title">
                    <span>组</span>
                    <a-tag>{{ groupAssignments.length }}</a-tag>
                  </div>
                  <div class="binding-tag-row">
                    <a-tag
                      v-for="item in visibleGroupAssignments"
                      :key="assignmentKey(item)"
                      :color="assignmentStatusColor(item.backendSyncStatus)"
                    >
                      {{ item.subjectName }}
                    </a-tag>
                    <a-tag v-if="groupAssignments.length > visibleGroupAssignments.length">
                      +{{ groupAssignments.length - visibleGroupAssignments.length }}
                    </a-tag>
                    <span v-if="!groupAssignments.length" class="muted-text">暂无绑定组</span>
                  </div>
                </div>
              </div>
              <a-empty v-else class="compact-empty" description="当前角色还没有绑定用户或组" />
            </div>

          </div>
          <a-empty v-else description="选择一个角色查看角色信息" />
        </a-tab-pane>

        <a-tab-pane :key="'scope'" :tab="`权限范围 (${rolePermissions.length})`">
          <div v-if="role" class="tab-panel">
            <div class="tab-toolbar">
              <a-input-search v-model="permissionKeyword" allow-clear placeholder="搜索数据库、表或权限" style="width: 260px" />
              <a-select v-model="permissionFilter" style="width: 140px">
                <a-select-option value="ALL">全部权限</a-select-option>
                <a-select-option v-for="item in permissionOptions" :key="item" :value="item">
                  {{ item }}
                </a-select-option>
              </a-select>
              <a-button size="small" type="primary" icon="plus" @click="$emit('open-permission-modal')">
                添加权限范围
              </a-button>
              <a-button
                size="small"
                :disabled="filteredRolePermissions.length <= permissionPreviewLimit"
                @click="permissionExpanded = !permissionExpanded"
              >
                {{ permissionExpanded ? '收起' : '展开全部' }}
              </a-button>
            </div>
            <div class="tab-helper">
              共 {{ rolePermissions.length }} 项，当前展示 {{ visibleRolePermissions.length }} 项
              <span v-if="filteredRolePermissions.length !== rolePermissions.length">，筛选命中 {{ filteredRolePermissions.length }} 项</span>
            </div>
            <a-table
              size="small"
              :row-key="tablePermissionKey"
              :columns="permissionColumns"
              :data-source="visibleRolePermissions"
              :pagination="false"
              :locale="{ emptyText: '当前筛选条件下暂无权限范围' }"
            >
              <template slot="resourceType" slot-scope="text">
                <a-tag color="blue">{{ resourceTypeLabel(text) }}</a-tag>
              </template>
              <template slot="tableName" slot-scope="text">
                <span>{{ text || '*' }}</span>
              </template>
              <template slot="permission" slot-scope="text">
                <a-tag color="green">{{ text }}</a-tag>
              </template>
              <template slot="action" slot-scope="text, record">
                <a-popconfirm
                  title="确认删除该权限范围？会尝试同步回收后端角色权限。"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="$emit('delete-role-permission', { roleCode: role.roleCode, permissionId: record.id })"
                >
                  <a-button size="small" type="link" class="danger-link">删除</a-button>
                </a-popconfirm>
              </template>
            </a-table>
          </div>
          <a-empty v-else description="选择一个角色查看权限范围" />
        </a-tab-pane>

        <a-tab-pane :key="'bindings'" :tab="`绑定对象 (${assignments.length})`">
          <div v-if="role" class="tab-panel">
            <div class="tab-toolbar">
              <a-input-search v-model="bindingKeyword" allow-clear placeholder="搜索主体或后端状态" style="width: 240px" />
              <a-select v-model="bindingSubjectTypeFilter" style="width: 140px">
                <a-select-option value="ALL">全部类型</a-select-option>
                <a-select-option v-for="item in bindingSubjectTypeOptions" :key="item" :value="item">
                  {{ subjectTypeLabel(item) }}
                </a-select-option>
              </a-select>
              <a-select v-model="bindingStatusFilter" style="width: 220px">
                <a-select-option value="ALL">全部状态</a-select-option>
                <a-select-option v-for="item in bindingStatusOptions" :key="item" :value="item">
                  {{ assignmentSyncStatusLabel(item) }}
                </a-select-option>
              </a-select>
            </div>
            <a-table
              size="small"
              :row-key="assignmentKey"
              :columns="bindingColumns"
              :data-source="filteredAssignments"
              :pagination="{ pageSize: 6 }"
              :locale="{ emptyText: '当前角色暂无绑定对象' }"
            >
              <template slot="subjectType" slot-scope="text">
                <a-tag>{{ subjectTypeLabel(text) }}</a-tag>
              </template>
              <template slot="backendSyncStatus" slot-scope="text">
                <a-tag :color="assignmentStatusColor(text)">{{ assignmentSyncStatusLabel(text) }}</a-tag>
              </template>
            </a-table>
          </div>
          <a-empty v-else description="选择一个角色查看绑定对象" />
        </a-tab-pane>
      </a-tabs>

      <div v-else class="tab-panel authorization-flow">
        <div class="mode-toolbar">
          <a-radio-group :value="state.mode" button-style="solid" @change="update('mode', $event.target.value)">
            <a-radio-button value="ROLE" :disabled="!roleModeAvailable">角色模式</a-radio-button>
            <a-radio-button value="DIRECT_EXCEPTION" :disabled="!directModeAvailable">直接例外</a-radio-button>
          </a-radio-group>
          <a-tag :color="state.mode === 'ROLE' ? 'blue' : 'orange'">
            {{ state.mode === 'ROLE' ? '推荐路径：RBAC 角色授权' : '例外路径：Direct Exception' }}
          </a-tag>
        </div>

        <template v-if="state.mode === 'ROLE'">
          <div class="action-section">
            <div class="section-head">
              <div>
                <h4>步骤 3：绑定角色</h4>
                <span>角色绑定只确认该对象可使用的角色范围，不会默认下发整个角色权限。</span>
              </div>
              <a-tag :color="hasCurrentBinding ? 'green' : 'orange'">
                {{ hasCurrentBinding ? '已绑定' : '待绑定' }}
              </a-tag>
            </div>
            <a-alert
              v-if="role && state.subjectName && hasCurrentBinding"
              class="section-alert"
              type="success"
              show-icon
              :message="bindingStatusText"
            />
            <a-alert
              v-else-if="role && state.subjectName"
              class="section-alert"
              type="warning"
              show-icon
              message="当前对象尚未绑定该角色，请先绑定后再继续授权。"
            />
            <div class="action-row compact-row">
              <a-button type="primary" icon="link" :loading="loading.submitting" :disabled="!canAssignRole" @click="$emit('assign-role')">
                绑定角色
              </a-button>
              <a-popconfirm title="确认回收该主体的角色绑定？" ok-text="回收" cancel-text="取消" @confirm="$emit('revoke-role')">
                <a-button type="danger" icon="disconnect" :loading="loading.submitting" :disabled="!canAssignRole">
                  回收绑定
                </a-button>
              </a-popconfirm>
            </div>
          </div>

          <div class="action-section permission-subset-section">
            <div class="subset-section-hero">
              <div class="subset-title-block">
                <span class="step-badge">步骤 4</span>
                <div>
                  <h4>选择角色范围内的权限子集</h4>
                  <span>角色定义可授权上限，真正下发的只会是当前勾选的权限子集。</span>
                </div>
              </div>
              <div class="subset-stat-strip">
                <div class="subset-stat-item">
                  <span>已选</span>
                  <strong>{{ selectedSubsetCount }}</strong>
                </div>
                <div class="subset-stat-item">
                  <span>命中</span>
                  <strong>{{ filteredSubsetRolePermissions.length }}</strong>
                </div>
                <div class="subset-stat-item">
                  <span>总计</span>
                  <strong>{{ rolePermissions.length }}</strong>
                </div>
              </div>
            </div>
            <div class="permission-subset-toolbar">
              <div class="subset-search-wrap">
                <a-input-search
                  :value="subsetPermissionKeyword"
                  allow-clear
                  placeholder="搜索库名、表名、权限，如 baofoo_bi / SELECT"
                  class="subset-search"
                  @change="updateSubsetPermissionSearch($event.target.value)"
                  @search="updateSubsetPermissionSearch"
                />
                <a-select :value="subsetPermissionFilter" class="subset-filter" @change="updateSubsetPermissionFilter">
                  <a-select-option value="ALL">全部权限</a-select-option>
                  <a-select-option v-for="item in permissionOptions" :key="item" :value="item">
                    {{ item }}
                  </a-select-option>
                </a-select>
              </div>
              <div class="subset-action-group">
                <a-button size="small" icon="check-square" :disabled="!filteredSubsetRolePermissions.length" @click="selectFilteredSubsetPermissions">全选筛选结果</a-button>
                <a-button size="small" icon="minus-square" :disabled="!filteredSubsetRolePermissions.length" @click="clearFilteredSubsetPermissions">清空筛选结果</a-button>
                <a-button size="small" icon="delete" @click="update('selectedRolePermissionKeys', [])">清空全部</a-button>
              </div>
            </div>
            <a-alert
              v-if="!rolePermissions.length"
              class="section-alert"
              type="info"
              show-icon
              message="当前角色暂无可供选择的权限范围。"
            />
            <a-empty v-else-if="!filteredSubsetRolePermissions.length" class="permission-empty" description="没有匹配的权限范围" />
            <template v-else>
              <a-checkbox-group
                :value="state.selectedRolePermissionKeys"
                class="permission-grid"
                @change="update('selectedRolePermissionKeys', $event)"
              >
                <div
                  v-for="item in pagedSubsetRolePermissions"
                  :key="permissionKey(item)"
                  class="permission-item-wrap"
                >
                  <a-checkbox
                    :value="permissionKey(item)"
                    class="permission-item"
                  >
                    <span class="permission-item-main">{{ rolePermissionShortText(item) }}</span>
                  </a-checkbox>
                  <a-button
                    v-if="isDatabasePermission(item)"
                    size="small"
                    type="link"
                    icon="branches"
                    class="table-drilldown-btn"
                    :loading="tableExpansionLoading[permissionKey(item)]"
                    @click.stop="toggleTableExpansion(item)"
                  >
                    表
                  </a-button>
                  <div v-if="expandedTableMap[permissionKey(item)]" class="table-expansion-panel">
                    <a-spin :spinning="!!tableExpansionLoading[permissionKey(item)]" size="small">
                      <div v-if="expandedTableList[permissionKey(item)] && expandedTableList[permissionKey(item)].length" class="table-expansion-list">
                        <a-checkbox-group
                          :value="selectedExpandedTables[permissionKey(item)] || []"
                          @change="updateExpandedTableSelection(permissionKey(item), item, $event)"
                        >
                          <a-checkbox
                            v-for="table in expandedTableList[permissionKey(item)]"
                            :key="table"
                            :value="table"
                            class="table-expansion-item"
                          >
                            {{ item.databaseName }}.{{ table }}
                          </a-checkbox>
                        </a-checkbox-group>
                        <div class="table-expansion-actions">
                          <a-button size="small" @click="selectAllExpandedTables(permissionKey(item), item)">全选</a-button>
                          <a-button size="small" @click="clearExpandedTables(permissionKey(item))">清空</a-button>
                        </div>
                      </div>
                      <a-empty v-else-if="!tableExpansionLoading[permissionKey(item)]" class="compact-empty" description="暂无可用表" />
                    </a-spin>
                  </div>
                </div>
              </a-checkbox-group>
              <div class="permission-pagination-row">
                <span class="pagination-hint">当前页 {{ pagedSubsetRolePermissions.length }} 项，仅分页展示，不影响已勾选结果。</span>
                <a-pagination
                  size="small"
                  :current="subsetPermissionPage"
                  :page-size="subsetPermissionPageSize"
                  :page-size-options="['12', '24', '48', '96']"
                  :total="filteredSubsetRolePermissions.length"
                  show-size-changer
                  show-quick-jumper
                  @change="updateSubsetPermissionPage"
                  @showSizeChange="updateSubsetPermissionPage"
                />
              </div>
            </template>
            <div class="field-hint">{{ subsetHint }}</div>
          </div>

          <div class="execution-verification-grid">
            <div class="action-section execute-section">
              <div class="section-head">
                <div>
                  <h4>步骤 4：执行授权或回收</h4>
                  <span>系统会对上方勾选的角色权限子集执行下发或回收。</span>
                </div>
              </div>
              <div class="action-row">
                <a-button type="primary" icon="safety" :loading="loading.submitting" :disabled="!canSubmitSubset" @click="$emit('grant-subset')">
                  下发子集权限
                </a-button>
                <a-popconfirm title="确认回收当前勾选的角色子集权限？" ok-text="回收" cancel-text="取消" @confirm="$emit('revoke-subset')">
                  <a-button type="danger" icon="rollback" :loading="loading.submitting" :disabled="!canSubmitSubset">
                    回收子集权限
                  </a-button>
                </a-popconfirm>
                <a-tooltip v-if="showHistoricalAdoptionButton" :title="historicalAdoptionDisabledReason">
                  <span class="historical-adoption-action-wrap">
                    <a-button
                      icon="audit"
                      :loading="loading.submitting"
                      :disabled="!canPreviewHistoricalAdoption"
                      @click="$emit('preview-historical-adoption')"
                    >
                      历史权限接管
                    </a-button>
                  </span>
                </a-tooltip>
                <a-tooltip v-if="showForceUserRevokeButton" :title="forceRevokeDisabledReason">
                  <span class="force-revoke-action-wrap">
                    <a-button
                      type="danger"
                      ghost
                      icon="warning"
                      :loading="loading.submitting"
                      :disabled="!canForceRevokeByUser"
                      @click="openForceRevokeModal"
                    >
                      强制按用户回收
                    </a-button>
                  </span>
                </a-tooltip>
              </div>
              <div v-if="showHistoricalAdoptionButton" class="historical-adoption-hint">
                {{ historicalAdoptionHint }}
              </div>
              <div v-if="showForceUserRevokeButton" class="admin-force-hint">
                {{ forceRevokeUserHint }}
              </div>
            </div>

            <div class="action-section verification-section compact-verification-section">
              <div class="section-head">
                <div>
                  <h4>步骤 5：权限校验</h4>
                  <span>对照实时授权后端权限与 DGA 记录，确认本次绑定或授权后的最终结果。</span>
                </div>
              </div>
              <a-alert
                v-if="verificationUser"
                class="section-alert"
                type="success"
                show-icon
                message="动作完成后，可直接在下方核对 live 与 recorded 是否一致。"
              />
              <permission-verification-panel
                :verification-user="verificationUser"
                :subject-name="state.subjectName"
                :snapshot="verificationSnapshot"
                :loading="loading.verification"
                @refresh="$emit('refresh-verification')"
                @sync="$emit('sync-verification')"
                @use-subject-as-verification="$emit('use-subject-as-verification')"
              />
            </div>
          </div>

          <div v-if="state.subjectType === 'USER'" class="action-section secondary-section">
            <div class="section-head">
              <div>
                <h4>可选：批量用户补绑定</h4>
                <span>角色范围不变，只批量补用户绑定记录。</span>
              </div>
            </div>
            <a-textarea
              :value="state.batchUsers"
              :rows="5"
              placeholder="每行一个用户名，或逗号分隔"
              @change="update('batchUsers', $event.target.value)"
            />
            <div class="action-row">
              <a-button icon="eye" :loading="loading.dryRunning" :disabled="!batchUserCount" @click="$emit('dry-run-batch')">
                Dry-run
              </a-button>
              <a-popconfirm title="确认执行批量补绑定？" ok-text="执行" cancel-text="取消" @confirm="$emit('assign-batch')">
                <a-button type="primary" icon="team" :loading="loading.submitting" :disabled="!batchUserCount">
                  执行补绑定
                </a-button>
              </a-popconfirm>
            </div>
            <div v-if="batchResult" class="batch-result">
              <a-tag color="blue">总数 {{ batchResult.total || 0 }}</a-tag>
              <a-tag color="green">成功 {{ batchResult.successCount || 0 }}</a-tag>
              <a-tag color="orange">待组映射 {{ batchResult.pendingGroupMappingCount || 0 }}</a-tag>
              <a-tag color="red">失败 {{ batchResult.failedCount || 0 }}</a-tag>
              <a-tag color="default">重复 {{ batchResult.duplicateCount || 0 }}</a-tag>
              <div v-if="batchResult.items && batchResult.items.length" class="batch-items">
                <div v-for="item in batchResult.items.slice(0, 8)" :key="`${item.input}-${item.action}`" class="batch-item-row">
                  <span>{{ item.username || item.input || '-' }}</span>
                  <a-tag :color="batchStatusColor(item.status)">{{ batchActionText(item.action) }} / {{ item.status }}</a-tag>
                </div>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="action-section secondary-section">
          <a-alert
            type="warning"
            show-icon
            class="section-alert"
            message="直接例外会绕过角色范围，仅适用于临时场景，并要求完整治理信息。"
          />
          <a-alert
            v-if="state.subjectType === 'GROUP'"
            type="warning"
            show-icon
            message="直接例外授权当前按用户执行，请切换授权对象为用户。"
            class="section-alert"
          />
          <div class="section-head">
            <div>
              <h4>例外路径：直接授权</h4>
              <span>仅针对单个历史用户追加或回收临时权限。</span>
            </div>
          </div>
          <a-form-model layout="vertical">
            <div class="direct-resource-box">
              <div class="section-head compact-head">
                <div>
                  <h4>资源范围</h4>
                  <span>直接例外只在这里选择资源范围，不再占用全局上下文。</span>
                </div>
                <a-radio-group
                  :value="state.scopeLevel"
                  button-style="solid"
                  size="small"
                  :disabled="!capability"
                  @change="update('scopeLevel', $event.target.value)"
                >
                  <a-radio-button v-for="item in scopeOptions" :key="item" :value="item">
                    {{ resourceTypeLabel(item) }}
                  </a-radio-button>
                </a-radio-group>
              </div>

              <a-form-model-item v-if="state.scopeLevel === 'DATABASE'" label="数据库范围">
                <a-select
                  :value="state.directDatabases"
                  mode="multiple"
                  placeholder="请选择数据库，支持多选"
                  :loading="loading.databases"
                  :disabled="!capability"
                  @change="update('directDatabases', $event)"
                >
                  <a-select-option v-for="item in databases" :key="item" :value="item">
                    {{ item }}
                  </a-select-option>
                </a-select>
                <div class="inline-tools">
                  <a-button size="small" :disabled="!databases.length" @click="update('directDatabases', databases)">全选数据库</a-button>
                  <a-button size="small" @click="update('directDatabases', [])">清空</a-button>
                </div>
              </a-form-model-item>

              <div v-else class="table-scope-grid">
                <a-form-model-item label="数据库">
                  <a-select
                    :value="state.databaseName"
                    show-search
                    placeholder="请选择数据库"
                    :loading="loading.databases"
                    :disabled="!capability"
                    @change="update('databaseName', $event)"
                  >
                    <a-select-option v-for="item in databases" :key="item" :value="item">
                      {{ item }}
                    </a-select-option>
                  </a-select>
                </a-form-model-item>
                <a-form-model-item label="表范围">
                  <a-select
                    :value="state.tableNames"
                    mode="multiple"
                    placeholder="请选择表，支持多选"
                    :loading="loading.tables"
                    :disabled="!state.databaseName"
                    @change="update('tableNames', $event)"
                  >
                    <a-select-option v-for="item in tables" :key="item" :value="item">
                      {{ item }}
                    </a-select-option>
                  </a-select>
                  <div class="inline-tools">
                    <a-button size="small" :disabled="!tables.length" @click="update('tableNames', tables)">全选表</a-button>
                    <a-button size="small" @click="update('tableNames', [])">清空</a-button>
                  </div>
                </a-form-model-item>
              </div>
            </div>

            <a-form-model-item label="权限类型">
              <a-select
                :value="state.permissions"
                mode="multiple"
                placeholder="请选择权限"
                :disabled="!capability"
                @change="update('permissions', $event)"
              >
                <a-select-option v-for="item in permissionOptions" :key="item" :value="item">
                  {{ item }}
                </a-select-option>
              </a-select>
              <div class="inline-tools">
                <a-button size="small" :disabled="!permissionOptions.length" @click="update('permissions', permissionOptions)">全选权限</a-button>
                <a-button size="small" @click="update('permissions', [])">清空</a-button>
              </div>
            </a-form-model-item>
            <a-form-model-item label="例外原因">
              <a-textarea
                :value="state.exceptionReason"
                :rows="3"
                placeholder="说明为什么需要直接例外授权"
                @change="update('exceptionReason', $event.target.value)"
              />
            </a-form-model-item>
            <div class="governance-grid">
              <a-form-model-item label="工单号">
                <a-input
                  :value="state.ticketNo"
                  placeholder="例如 DGA-20260514-001"
                  @change="update('ticketNo', $event.target.value)"
                />
              </a-form-model-item>
              <a-form-model-item label="审批人">
                <a-input
                  :value="state.approver"
                  placeholder="审批人账号"
                  @change="update('approver', $event.target.value)"
                />
              </a-form-model-item>
              <a-form-model-item label="过期时间">
                <a-input
                  :value="state.expiresAt"
                  type="datetime-local"
                  @change="update('expiresAt', $event.target.value)"
                />
              </a-form-model-item>
              <a-form-model-item label="风险等级">
                <a-select :value="state.riskLevel" @change="update('riskLevel', $event)">
                  <a-select-option value="LOW">LOW</a-select-option>
                  <a-select-option value="MEDIUM">MEDIUM</a-select-option>
                  <a-select-option value="HIGH">HIGH</a-select-option>
                </a-select>
              </a-form-model-item>
            </div>
          </a-form-model>
          <div class="field-hint">{{ directHint }}</div>
          <div class="action-row">
            <a-button type="primary" icon="safety" :loading="loading.submitting" :disabled="!canSubmitDirect" @click="$emit('grant-direct')">
              授权
            </a-button>
            <a-popconfirm title="确认回收当前直接例外权限？" ok-text="回收" cancel-text="取消" @confirm="$emit('revoke-direct')">
              <a-button type="danger" icon="rollback" :loading="loading.submitting" :disabled="!canSubmitDirect">
                回收
              </a-button>
            </a-popconfirm>
          </div>
        </div>

      </div>
    </a-spin>

    <a-modal
      :visible="forceRevokeModalVisible"
      title="选择强制回收权限"
      ok-text="确认回收"
      cancel-text="取消"
      ok-type="danger"
      width="760px"
      destroy-on-close
      :confirm-loading="loading.submitting"
      :ok-button-props="{ props: { disabled: !forceRevokePermissionKeys.length } }"
      @ok="confirmForceRevokeUser"
      @cancel="closeForceRevokeModal"
    >
      <div class="force-revoke-modal">
        <a-alert
          class="force-revoke-alert"
          type="warning"
          show-icon
          :message="`将按用户 ${forceRevokeTargetUser || '-'} 直接回收所选权限；来自 LDAP 组角色继承的权限不会出现在这里，需回收组绑定或组权限。`"
        />
        <div class="force-revoke-summary">
          <div class="summary-chip">
            <span>目标用户</span>
            <strong>{{ forceRevokeTargetUser || '-' }}</strong>
          </div>
          <div class="summary-chip">
            <span>可按用户回收</span>
            <strong>{{ forceRevokeUserPermissions.length }}</strong>
          </div>
          <div class="summary-chip">
            <span>已选权限</span>
            <strong>{{ forceRevokePermissionKeys.length }}</strong>
          </div>
        </div>
        <div class="force-revoke-toolbar">
          <a-input-search
            v-model="forceRevokeKeyword"
            allow-clear
            placeholder="搜索库名、表名或权限"
            class="force-revoke-search"
          />
          <a-select v-model="forceRevokePermissionFilter" class="force-revoke-filter">
            <a-select-option value="ALL">全部权限</a-select-option>
            <a-select-option v-for="item in forceRevokePermissionOptions" :key="item" :value="item">
              {{ item }}
            </a-select-option>
          </a-select>
          <a-select v-model="forceRevokeStatusFilter" class="force-revoke-status-filter">
            <a-select-option value="ALL">全部状态</a-select-option>
            <a-select-option value="MATCHED">完全一致</a-select-option>
            <a-select-option value="LIVE_ONLY">仅后端存在</a-select-option>
            <a-select-option value="RECORDED_ONLY">仅 DGA 记录</a-select-option>
          </a-select>
        </div>
        <div class="force-revoke-actions">
          <span>筛选命中 {{ filteredForceRevokePermissions.length }} 项，共 {{ forceRevokeUserPermissions.length }} 项</span>
          <div>
            <a-button size="small" :disabled="!filteredForceRevokePermissions.length" @click="selectFilteredForceRevokePermissions">全选筛选结果</a-button>
            <a-button size="small" :disabled="!filteredForceRevokePermissions.length" @click="clearFilteredForceRevokePermissions">清空筛选结果</a-button>
            <a-button size="small" @click="restoreCurrentForceRevokeSelection">还原当前勾选</a-button>
          </div>
        </div>
        <a-empty v-if="!filteredForceRevokePermissions.length" class="force-revoke-empty" description="没有匹配的权限范围" />
        <a-checkbox-group
          v-else
          :value="forceRevokePermissionKeys"
          class="force-revoke-permission-list"
          @change="forceRevokePermissionKeys = $event"
        >
          <a-checkbox
            v-for="item in filteredForceRevokePermissions"
            :key="permissionKey(item)"
            :value="permissionKey(item)"
            class="force-revoke-permission-item"
          >
            <span class="permission-item-main">{{ forceRevokePermissionText(item) }}</span>
            <a-tag :color="verificationStatusColor(item.status)">{{ previewStatusLabel(item.status) }}</a-tag>
          </a-checkbox>
        </a-checkbox-group>
      </div>
    </a-modal>
  </a-card>
</template>

<script>
import axios from 'axios';
import PermissionVerificationPanel from './PermissionVerificationPanel.vue';
import { canDelete } from '../../../utils/currentUser';
import {
  assignmentKey,
  assignmentStatusColor,
  batchActionText,
  batchStatusColor,
  canDirectExceptionPrincipal,
  filterRoleAssignments,
  filterRolePermissions,
  hasUsableRoleAssignment,
  isDatabasePermission,
  isGroupInheritedGrant,
  operationModeLabel,
  permissionKey,
  resourceTypeLabel,
  roleDisplayName,
  rolePermissionShortText,
  subjectTypeLabel,
  supportsDirectGrant,
  supportsRoles,
  supportsRoleSubsetGrant,
  uniqueRolePermissions,
  verificationDiffRows,
  verificationStatusColor,
  verificationSummary
} from '../authorizationCenterHelpers';

export default {
  name: 'RoleGrantWorkbench',
  components: {
    PermissionVerificationPanel
  },
  props: {
    capability: {
      type: Object,
      default: null
    },
    selectedRoleCode: {
      type: String,
      default: ''
    },
    selectedRoleView: {
      type: Object,
      default: null
    },
    selectedPrincipal: {
      type: Object,
      default: null
    },
    verificationPrincipal: {
      type: Object,
      default: null
    },
    verificationUser: {
      type: String,
      default: ''
    },
    verificationSnapshot: {
      type: Object,
      default: null
    },
    subjectContext: {
      type: Object,
      default: () => ({ loading: false, roles: [], ldapProfile: null })
    },
    databases: {
      type: Array,
      default: () => []
    },
    tables: {
      type: Array,
      default: () => []
    },
    activeTab: {
      type: String,
      default: 'info'
    },
    state: {
      type: Object,
      required: true
    },
    loading: {
      type: Object,
      default: () => ({})
    },
    batchResult: {
      type: Object,
      default: null
    },
    mode: {
      type: String,
      default: 'full'
    }
  },
  data() {
    return {
      permissionKeyword: '',
      permissionFilter: 'ALL',
      permissionExpanded: false,
      subsetPermissionKeyword: '',
      subsetPermissionFilter: 'ALL',
      subsetPermissionPage: 1,
      subsetPermissionPageSize: 12,
      expandedTableMap: {},
      expandedTableList: {},
      selectedExpandedTables: {},
      tableExpansionLoading: {},
      forceRevokeModalVisible: false,
      forceRevokeKeyword: '',
      forceRevokePermissionFilter: 'ALL',
      forceRevokeStatusFilter: 'ALL',
      forceRevokePermissionKeys: [],
      bindingKeyword: '',
      bindingStatusFilter: 'ALL',
      bindingSubjectTypeFilter: 'ALL',
      permissionPreviewLimit: 10,
      permissionColumns: [
        { title: '资源维度', dataIndex: 'resourceType', key: 'resourceType', scopedSlots: { customRender: 'resourceType' } },
        { title: '数据库', dataIndex: 'databaseName', key: 'databaseName' },
        { title: '表', dataIndex: 'tableName', key: 'tableName', scopedSlots: { customRender: 'tableName' } },
        { title: '权限', dataIndex: 'permission', key: 'permission', scopedSlots: { customRender: 'permission' } },
        { title: '操作', key: 'action', width: 90, scopedSlots: { customRender: 'action' } }
      ],
      bindingColumns: [
        { title: '类型', dataIndex: 'subjectType', key: 'subjectType', scopedSlots: { customRender: 'subjectType' } },
        { title: '主体', dataIndex: 'subjectName', key: 'subjectName' },
        { title: '后端', dataIndex: 'authBackend', key: 'authBackend' },
        { title: '状态', dataIndex: 'backendSyncStatus', key: 'backendSyncStatus', scopedSlots: { customRender: 'backendSyncStatus' } }
      ]
    };
  },
  computed: {
    role() {
      return this.selectedRoleView && this.selectedRoleView.role ? this.selectedRoleView.role : null;
    },
    currentSubjectLabel() {
      if (!this.state.subjectName) return '未选择';
      return `${subjectTypeLabel(this.state.subjectType)} ${this.state.subjectName}`;
    },
    showRoleManagement() {
      return this.mode !== 'authorization';
    },
    showAuthorization() {
      return this.mode !== 'role-management';
    },
    showForceUserRevokeButton() {
      return this.showAuthorization && canDelete();
    },
    showHistoricalAdoptionButton() {
      return this.showAuthorization && String(this.state.selectedAuthBackend || '').trim().toUpperCase() === 'SENTRY';
    },
    workspaceEyebrow() {
      return this.showAuthorization ? 'Authorization Workspace' : 'Role Management';
    },
    workspaceSubtitle() {
      if (!this.role) {
        return this.showAuthorization ? '选择角色后执行绑定、授权与校验' : '选择角色后维护信息、权限范围与绑定对象';
      }
      return this.showAuthorization ? `${this.role.roleCode || '-'} · 用于当前授权对象` : this.role.roleCode || '-';
    },
    capabilityEngineType() {
      return this.capability && this.capability.engineType ? this.capability.engineType : '';
    },
    rolePermissions() {
      return uniqueRolePermissions(this.selectedRoleView);
    },
    filteredRolePermissions() {
      return filterRolePermissions(this.rolePermissions, this.permissionKeyword, this.permissionFilter);
    },
    filteredSubsetRolePermissions() {
      return filterRolePermissions(this.rolePermissions, this.subsetPermissionKeyword, this.subsetPermissionFilter);
    },
    forceRevokeAllPermissionRows() {
      return verificationDiffRows(this.verificationSnapshot)
        .filter(item => item.status === 'MATCHED' || item.status === 'LIVE_ONLY' || item.status === 'RECORDED_ONLY');
    },
    forceRevokeUserPermissions() {
      return this.forceRevokeAllPermissionRows.filter(item => !isGroupInheritedGrant(item.live || item.recorded));
    },
    forceRevokeInheritedPermissionCount() {
      return this.forceRevokeAllPermissionRows.length - this.forceRevokeUserPermissions.length;
    },
    forceRevokePermissionOptions() {
      return Array.from(new Set(this.forceRevokeUserPermissions.map(item => item.permission).filter(Boolean)));
    },
    filteredForceRevokePermissions() {
      const normalizedKeyword = String(this.forceRevokeKeyword || '').trim().toLowerCase();
      const normalizedPermission = String(this.forceRevokePermissionFilter || 'ALL').toUpperCase();
      const normalizedStatus = String(this.forceRevokeStatusFilter || 'ALL').toUpperCase();
      return this.forceRevokeUserPermissions.filter(item => {
        if (normalizedPermission !== 'ALL' && String(item.permission || '').toUpperCase() !== normalizedPermission) return false;
        if (normalizedStatus !== 'ALL' && String(item.status || '').toUpperCase() !== normalizedStatus) return false;
        if (!normalizedKeyword) return true;
        return [
          item.databaseName,
          item.tableName,
          item.permission,
          item.resourceType,
          item.authBackend,
          item.status,
          item.source,
          item.sourceRole,
          item.sourceGroup,
          item.grantText
        ].some(field => String(field || '').toLowerCase().includes(normalizedKeyword));
      });
    },
    pagedSubsetRolePermissions() {
      const start = (this.subsetPermissionPage - 1) * this.subsetPermissionPageSize;
      return this.filteredSubsetRolePermissions.slice(start, start + this.subsetPermissionPageSize);
    },
    selectedSubsetCount() {
      return Array.isArray(this.state.selectedRolePermissionKeys) ? this.state.selectedRolePermissionKeys.length : 0;
    },
    visibleRolePermissions() {
      return this.permissionExpanded
        ? this.filteredRolePermissions
        : this.filteredRolePermissions.slice(0, this.permissionPreviewLimit);
    },
    assignments() {
      return this.selectedRoleView && Array.isArray(this.selectedRoleView.assignments)
        ? this.selectedRoleView.assignments
        : [];
    },
    userAssignments() {
      return this.assignments.filter(item => String(item?.subjectType || '').toUpperCase() === 'USER');
    },
    groupAssignments() {
      return this.assignments.filter(item => String(item?.subjectType || '').toUpperCase() === 'GROUP');
    },
    visibleUserAssignments() {
      return this.userAssignments.slice(0, 8);
    },
    visibleGroupAssignments() {
      return this.groupAssignments.slice(0, 8);
    },
    filteredAssignments() {
      return filterRoleAssignments(this.assignments, this.bindingKeyword, this.bindingStatusFilter, this.bindingSubjectTypeFilter);
    },
    bindingStatusOptions() {
      return Array.from(new Set(this.assignments.map(item => String(item?.backendSyncStatus || 'UNKNOWN').toUpperCase())));
    },
    bindingSubjectTypeOptions() {
      return Array.from(new Set(this.assignments.map(item => String(item?.subjectType || 'UNKNOWN').toUpperCase())));
    },
    roleModeAvailable() {
      return supportsRoles(this.capability);
    },
    directModeAvailable() {
      return supportsDirectGrant(this.capability);
    },
    permissionOptions() {
      return Array.isArray(this.capability?.permissions) && this.capability.permissions.length
        ? this.capability.permissions
        : ['SELECT'];
    },
    scopeOptions() {
      const types = Array.isArray(this.capability?.resourceTypes)
        ? this.capability.resourceTypes.map(item => String(item || '').toUpperCase())
        : [];
      const filtered = types.filter(item => item === 'DATABASE' || item === 'TABLE');
      return filtered.length ? filtered : ['DATABASE', 'TABLE'];
    },
    hasCurrentBinding() {
      return Boolean(
        this.selectedRoleView
        && this.state.subjectName
        && hasUsableRoleAssignment(this.selectedRoleView, this.state.subjectType, this.state.subjectName, this.state.selectedAuthBackend)
      );
    },
    bindingStatusText() {
      return `${this.currentSubjectLabel} 已绑定当前角色；绑定只表示可授权范围，真正下发以后续勾选的权限子集为准。`;
    },
    canAssignRole() {
      return Boolean(this.selectedRoleView && this.state.subjectName);
    },
    canSubmitSubset() {
      return Boolean(
        this.selectedRoleView
        && this.state.subjectName
        && Array.isArray(this.state.selectedRolePermissionKeys)
        && this.state.selectedRolePermissionKeys.length
        && supportsRoleSubsetGrant(this.capability, this.state.subjectType)
        && this.hasCurrentBinding
        && (!this.requiresVerificationUser || this.state.verificationUser)
      );
    },
    historicalAdoptionTargetUser() {
      if (this.state.subjectType === 'USER') {
        return String(this.state.subjectName || '').trim();
      }
      return String(this.state.verificationUser || '').trim();
    },
    canPreviewHistoricalAdoption() {
      return Boolean(
        this.showHistoricalAdoptionButton
        && this.selectedRoleView
        && this.state.subjectName
        && this.historicalAdoptionTargetUser
        && this.rolePermissions.length
      );
    },
    historicalAdoptionDisabledReason() {
      if (this.canPreviewHistoricalAdoption) return '读取用户 live 权限，与当前 DGA 角色范围对账后仅写入本地接管记录。';
      if (!this.selectedRoleView) return '请先选择要对账的 DGA 角色。';
      if (!this.state.subjectName) return '请先选择要绑定的授权对象。';
      if (!this.historicalAdoptionTargetUser) return '组选中时需要先指定一个校验用户，用于读取历史 live 权限。';
      if (!this.rolePermissions.length) return '当前角色没有权限范围，无法与历史权限对账。';
      return '历史权限接管当前仅支持 Hive + Sentry。';
    },
    historicalAdoptionHint() {
      if (this.historicalAdoptionDisabledReason && !this.canPreviewHistoricalAdoption) {
        return `历史权限接管不可用：${this.historicalAdoptionDisabledReason}`;
      }
      return `历史权限接管：对用户 ${this.historicalAdoptionTargetUser} 的现有 Hive/Sentry 权限与当前角色范围做对账，只写入 DGA 记录和 LOCAL_ONLY 绑定。`;
    },
    forceRevokeTargetUser() {
      if (this.state.subjectType === 'USER') {
        return String(this.state.subjectName || '').trim();
      }
      return String(this.state.verificationUser || '').trim();
    },
    canForceRevokeByUser() {
      return Boolean(
        this.showForceUserRevokeButton
        && this.forceRevokeUserPermissions.length
        && this.forceRevokeTargetUser
      );
    },
    forceRevokeDisabledReason() {
      if (this.canForceRevokeByUser) return '';
      if (!this.forceRevokeTargetUser) {
        return this.state.subjectType === 'GROUP'
          ? '组选中时需要先指定校验用户，才能按用户强制回收。'
          : '请先选择目标用户，再执行强制回收。';
      }
      if (!this.forceRevokeAllPermissionRows.length) {
        return `当前用户 ${this.forceRevokeTargetUser} 暂无可回收的 live 或 DGA 记录权限，请先刷新权限校验。`;
      }
      if (!this.forceRevokeUserPermissions.length && this.forceRevokeInheritedPermissionCount) {
        return `当前用户 ${this.forceRevokeTargetUser} 的权限均继承自 LDAP 组，不能按用户强制回收；请回收组绑定或组权限。`;
      }
      return `当前用户 ${this.forceRevokeTargetUser} 暂无可按用户直接回收的权限。`;
    },
    forceRevokeUserHint() {
      if (this.forceRevokeDisabledReason) {
        return `管理员操作不可用：${this.forceRevokeDisabledReason}`;
      }
      if (this.forceRevokeInheritedPermissionCount) {
        return `管理员操作：将直接按用户 ${this.forceRevokeTargetUser} 回收可选权限；已排除 ${this.forceRevokeInheritedPermissionCount} 项 LDAP 组继承权限。`;
      }
      return `管理员操作：将直接按用户 ${this.forceRevokeTargetUser} 回收所选用户已有权限，不依赖角色绑定状态。`;
    },
    canSubmitDirect() {
      if (this.state.subjectType !== 'USER' || !this.state.subjectName) return false;
      if (!canDirectExceptionPrincipal(this.selectedPrincipal)) return false;
      if (!Array.isArray(this.state.permissions) || !this.state.permissions.length) return false;
      if (this.state.scopeLevel === 'DATABASE') {
        return Array.isArray(this.state.directDatabases) && this.state.directDatabases.length > 0;
      }
      return Boolean(this.state.databaseName && Array.isArray(this.state.tableNames) && this.state.tableNames.length > 0);
    },
    subsetHint() {
      if (!this.selectedRoleView) return '仅允许勾选当前角色范围内已有的权限。';
      if (this.state.subjectName && !this.hasCurrentBinding) {
        return '请先完成角色绑定，再执行角色范围内授权/回收。';
      }
      return '仅允许勾选当前角色范围内已有的权限。';
    },
    directHint() {
      if (this.state.subjectType !== 'USER') {
        return '直接例外授权当前按用户执行，请切换授权对象为用户。';
      }
      if (this.selectedPrincipal && !canDirectExceptionPrincipal(this.selectedPrincipal)) {
        return '该主体会被视为新用户，必须先绑定角色后才能执行直接例外授权/回收。';
      }
      return '直接例外会绕过角色范围，必须填写原因、工单、审批人和过期时间，便于审计追踪。';
    },
    batchUserCount() {
      return String(this.state.batchUsers || '')
        .split(/[\n,]+/)
        .map(item => item.trim())
        .filter(Boolean)
        .length;
    },
    requiresVerificationUser() {
      return String(this.state.subjectType || '').toUpperCase() === 'GROUP';
    },
    selectedPermissionCount() {
      return Array.isArray(this.state.selectedRolePermissionKeys) ? this.state.selectedRolePermissionKeys.length : 0;
    },
    subjectBoundRoles() {
      return Array.isArray(this.subjectContext?.roles) ? this.subjectContext.roles : [];
    },
    visibleSubjectBoundRoles() {
      return this.subjectBoundRoles.slice(0, 4);
    },
    primaryGroupName() {
      return this.groupNameOf(this.subjectContext?.ldapProfile?.primaryGroup)
        || this.groupNameOf(this.subjectContext?.ldapProfile?.primaryGroupName)
        || '';
    },
    subjectGroupNames() {
      const names = [];
      const pushName = value => {
        const name = this.groupNameOf(value);
        if (name && !names.some(item => item.toLowerCase() === name.toLowerCase())) {
          names.push(name);
        }
      };
      pushName(this.subjectContext?.ldapProfile?.primaryGroup);
      pushName(this.subjectContext?.ldapProfile?.primaryGroupName);
      pushName(this.subjectContext?.ldapProfile?.ldapGroup);
      pushName(this.subjectContext?.ldapProfile?.groupName);
      const supplementaryGroups = Array.isArray(this.subjectContext?.ldapProfile?.supplementaryGroups)
        ? this.subjectContext.ldapProfile.supplementaryGroups
        : [];
      supplementaryGroups.forEach(pushName);
      if (String(this.state.subjectType || '').toUpperCase() === 'GROUP') {
        pushName(this.state.subjectName);
      }
      return names;
    },
    permissionContextLabel() {
      return String(this.state.subjectType || '').toUpperCase() === 'GROUP' ? '校验用户权限视角' : '当前用户权限';
    },
    permissionPreviewHint() {
      return String(this.state.subjectType || '').toUpperCase() === 'GROUP'
        ? '基于当前校验用户展示 live / recorded 权限差异。'
        : '展示当前用户在 live / recorded 中的权限差异。';
    },
    permissionSummary() {
      return verificationSummary(this.verificationSnapshot);
    },
    permissionPreviewRows() {
      return verificationDiffRows(this.verificationSnapshot).slice(0, 5);
    },
    livePermissionCount() {
      return Array.isArray(this.verificationSnapshot?.grants) ? this.verificationSnapshot.grants.length : 0;
    },
    recordedPermissionCount() {
      return Array.isArray(this.verificationSnapshot?.recordedGrants) ? this.verificationSnapshot.recordedGrants.length : 0;
    },
    subjectPermissionTotal() {
      return this.permissionSummary.total;
    }
  },
  watch: {
    selectedRoleCode() {
      this.expandedTableMap = {};
      this.expandedTableList = {};
      this.selectedExpandedTables = {};
    }
  },
  methods: {
    assignmentKey,
    assignmentStatusColor,
    batchActionText,
    batchStatusColor,
    isDatabasePermission,
    operationModeLabel,
    permissionKey,
    resourceTypeLabel,
    roleDisplayName,
    rolePermissionShortText,
    subjectTypeLabel,
    verificationStatusColor,
    subjectRoleKey(roleView) {
      return roleView?.role?.roleCode || roleView?.roleCode || roleDisplayName(roleView);
    },
    subjectRoleTagColor(roleView) {
      const roleCode = roleView?.role?.roleCode || '';
      if (roleCode && roleCode === this.selectedRoleCode) return 'blue';
      return roleView?.bindingMode === 'GROUP_INHERITED' ? 'cyan' : 'green';
    },
    subjectRoleTagText(roleView) {
      const suffix = roleView?.bindingMode === 'GROUP_INHERITED' ? '来自组' : '直绑';
      return `${roleDisplayName(roleView)} · ${suffix}`;
    },
    previewPermissionText(row) {
      const database = row?.databaseName || '*';
      const table = row?.tableName ? `.${row.tableName}` : '.*';
      return `${resourceTypeLabel(row?.resourceType)} ${database}${table} ${row?.permission || ''}`;
    },
    previewStatusLabel(status) {
      if (status === 'MATCHED') return '一致';
      if (status === 'LIVE_ONLY') return '仅后端存在';
      if (status === 'RECORDED_ONLY') return '仅 DGA 记录';
      return status || '-';
    },
    assignmentSyncStatusLabel(status) {
      const normalized = String(status || 'UNKNOWN').toUpperCase();
      if (normalized === 'SUCCESS') return '已下发';
      if (normalized === 'LOCAL_ONLY') return '仅绑定范围';
      if (normalized === 'PENDING_GROUP_MAPPING') return '待组映射';
      if (normalized === 'FAILED') return '下发失败';
      if (normalized === 'REVOKED') return '已回收';
      if (normalized === 'LOCAL_REVOKED') return '本地已回收';
      return normalized;
    },
    groupNameOf(value) {
      if (value == null) return '';
      if (typeof value === 'string') return value.trim();
      return String(value.name || value.cn || value.groupName || value.primaryGroupName || value.value || '').trim();
    },
    tablePermissionKey(permission) {
      return permission.id || permissionKey(permission);
    },
    updateSubsetPermissionSearch(value) {
      this.subsetPermissionKeyword = value;
      this.subsetPermissionPage = 1;
    },
    updateSubsetPermissionFilter(value) {
      this.subsetPermissionFilter = value;
      this.subsetPermissionPage = 1;
    },
    updateSubsetPermissionPage(page, pageSize) {
      this.subsetPermissionPage = page;
      this.subsetPermissionPageSize = pageSize;
    },
    selectFilteredSubsetPermissions() {
      const existing = Array.isArray(this.state.selectedRolePermissionKeys) ? this.state.selectedRolePermissionKeys : [];
      const additions = this.filteredSubsetRolePermissions.map(item => permissionKey(item));
      this.update('selectedRolePermissionKeys', Array.from(new Set([...existing, ...additions])));
    },
    clearFilteredSubsetPermissions() {
      const removeKeys = new Set(this.filteredSubsetRolePermissions.map(item => permissionKey(item)));
      const existing = Array.isArray(this.state.selectedRolePermissionKeys) ? this.state.selectedRolePermissionKeys : [];
      this.update('selectedRolePermissionKeys', existing.filter(item => !removeKeys.has(item)));
    },
    openForceRevokeModal() {
      this.forceRevokeKeyword = '';
      this.forceRevokePermissionFilter = 'ALL';
      this.forceRevokeStatusFilter = 'ALL';
      this.restoreCurrentForceRevokeSelection();
      this.forceRevokeModalVisible = true;
    },
    closeForceRevokeModal() {
      this.forceRevokeModalVisible = false;
    },
    restoreCurrentForceRevokeSelection() {
      this.forceRevokePermissionKeys = this.forceRevokeUserPermissions.map(item => permissionKey(item));
    },
    selectFilteredForceRevokePermissions() {
      const existing = Array.isArray(this.forceRevokePermissionKeys) ? this.forceRevokePermissionKeys : [];
      const additions = this.filteredForceRevokePermissions.map(item => permissionKey(item));
      this.forceRevokePermissionKeys = Array.from(new Set([...existing, ...additions]));
    },
    clearFilteredForceRevokePermissions() {
      const removeKeys = new Set(this.filteredForceRevokePermissions.map(item => permissionKey(item)));
      const existing = Array.isArray(this.forceRevokePermissionKeys) ? this.forceRevokePermissionKeys : [];
      this.forceRevokePermissionKeys = existing.filter(item => !removeKeys.has(item));
    },
    confirmForceRevokeUser() {
      const selectedKeys = new Set(this.forceRevokePermissionKeys || []);
      const permissions = this.forceRevokeUserPermissions.filter(item => selectedKeys.has(permissionKey(item)));
      if (!permissions.length) {
        return;
      }
      this.$emit('force-revoke-user', { permissions });
      this.forceRevokeModalVisible = false;
    },
    forceRevokePermissionText(item) {
      const database = item?.databaseName || '*';
      const table = item?.tableName ? `.${item.tableName}` : '.*';
      return `${database}${table} ${item?.permission || ''}`;
    },
    update(field, value) {
      this.$emit('change', { field, value });
    },
    async toggleTableExpansion(item) {
      const key = permissionKey(item);
      if (this.expandedTableMap[key]) {
        this.$set(this.expandedTableMap, key, false);
        return;
      }
      this.$set(this.expandedTableMap, key, true);
      if (this.expandedTableList[key]) {
        return;
      }
      this.$set(this.tableExpansionLoading, key, true);
      try {
        const roleCode = this.role?.roleCode;
        const permissionId = item.id;
        const params = {};
        if (this.state.selectedCluster) params.cluster = this.state.selectedCluster;
        if (this.state.selectedAuthBackend) params.authBackend = this.state.selectedAuthBackend;
        const response = await axios.get(`/api/access/roles/${roleCode}/permissions/${permissionId}/tables`, { params });
        this.$set(this.expandedTableList, key, Array.isArray(response.data) ? response.data : []);
      } catch (e) {
        this.$message.error('加载表列表失败: ' + (e?.response?.data?.message || e.message || '未知错误'));
        this.$set(this.expandedTableList, key, []);
      } finally {
        this.$set(this.tableExpansionLoading, key, false);
      }
    },
    updateExpandedTableSelection(key, item, tables) {
      this.$set(this.selectedExpandedTables, key, tables);
      this.$emit('table-expansion-change', { parentPermission: item, tables, key });
    },
    selectAllExpandedTables(key, item) {
      const tables = this.expandedTableList[key] || [];
      this.$set(this.selectedExpandedTables, key, [...tables]);
      this.$emit('table-expansion-change', { parentPermission: item, tables: [...tables], key });
    },
    clearExpandedTables(key) {
      this.$set(this.selectedExpandedTables, key, []);
      this.$emit('table-expansion-change', { parentPermission: null, tables: [], key });
    }
  }
};
</script>

<style scoped>
.workbench-card {
  overflow: hidden;
  border-radius: 14px;
}
.workspace-header,
.workspace-actions,
.section-head,
.action-row,
.head-actions,
.batch-item-row,
.mode-toolbar,
.compact-row {
  display: flex;
  gap: 12px;
}
.workspace-header,
.section-head,
.batch-item-row,
.mode-toolbar {
  justify-content: space-between;
}
.mode-toolbar {
  align-items: center;
  flex-wrap: wrap;
}
.workspace-header {
  align-items: flex-start;
  margin: -2px -2px 18px;
  padding: 4px 2px 16px;
  border-bottom: 1px solid #edf2f7;
}
.workspace-eyebrow {
  margin-bottom: 6px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #667085;
}
.workspace-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}
.workspace-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #667085;
}
.workspace-actions {
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.summary-banner {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.summary-item,
.metric-card,
.action-section,
.subject-context-panel,
.context-block {
  border: 1px solid #e7edf5;
  border-radius: 10px;
  background: #fbfdff;
}
.summary-item {
  padding: 12px 14px;
}
.summary-item span,
.metric-card span,
.field-hint,
.section-head span,
.tab-helper {
  display: block;
  font-size: 12px;
  color: #667085;
}
.summary-item strong,
.metric-card strong {
  display: block;
  margin-top: 6px;
  color: #1f2d3d;
}
.workspace-alert {
  margin-bottom: 16px;
}
.subject-context-spin {
  display: block;
  margin-bottom: 16px;
}
.subject-context-panel {
  padding: 14px 16px;
}
.subject-context-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.subject-context-head span,
.context-block span,
.muted-text {
  color: #667085;
  font-size: 12px;
}
.subject-context-grid {
  display: grid;
  grid-template-columns: 1.2fr 1.5fr 0.8fr;
  gap: 12px;
}
.context-block {
  min-width: 0;
  padding: 12px;
  background: #fff;
}
.context-block strong {
  display: block;
  margin: 4px 0 8px;
  color: #1f2d3d;
  font-size: 18px;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.permission-preview {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #edf2f7;
}
.permission-preview-head,
.permission-preview-row {
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.permission-preview-head {
  align-items: flex-start;
  margin-bottom: 10px;
}
.permission-preview-head span,
.permission-preview-text {
  color: #667085;
  font-size: 12px;
}
.permission-preview-list {
  display: grid;
  gap: 8px;
}
.permission-preview-row {
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}
.permission-preview-text {
  min-width: 0;
  flex: 1;
}
.tab-panel {
  padding-top: 4px;
}
.authorization-flow {
  display: grid;
  gap: 16px;
}
.detail-desc {
  margin-bottom: 16px;
}
.info-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.metric-card {
  padding: 14px 16px;
}
.binding-overview {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}
.binding-overview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.binding-overview-head span,
.binding-block-title span {
  color: #667085;
  font-size: 12px;
}
.binding-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.binding-overview-block {
  min-width: 0;
  padding: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
}
.binding-block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.binding-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.compact-empty {
  padding: 8px 0;
}
.panel-section-title,
.section-head h4 {
  margin: 0;
  color: #1f2d3d;
}
.tab-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.tab-helper {
  margin-bottom: 12px;
}
.danger-link {
  color: #cf1322;
}
.action-section {
  padding: 16px;
  border-radius: 12px;
}
.secondary-section {
  background: #fffdf8;
}
.execution-verification-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.75fr) minmax(360px, 1.25fr);
  gap: 16px;
  align-items: stretch;
}
.execute-section {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.execute-section .action-row {
  margin-top: 18px;
}
.force-revoke-action-wrap,
.historical-adoption-action-wrap {
  display: inline-flex;
}
.admin-force-hint,
.historical-adoption-hint {
  margin-top: 10px;
  color: #8c6d1f;
  font-size: 12px;
  line-height: 1.5;
}
.historical-adoption-hint {
  color: #175cd3;
}
.force-revoke-alert {
  margin-bottom: 12px;
}
.force-revoke-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.summary-chip {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #ffe0b2;
  border-radius: 10px;
  background: #fffaf0;
}
.summary-chip span {
  display: block;
  color: #8a6d3b;
  font-size: 12px;
}
.summary-chip strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #1f2d3d;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.force-revoke-toolbar,
.force-revoke-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}
.force-revoke-toolbar {
  margin-bottom: 10px;
}
.force-revoke-search {
  flex: 1;
}
.force-revoke-filter {
  width: 140px;
}
.force-revoke-status-filter {
  width: 150px;
}
.force-revoke-actions {
  margin-bottom: 12px;
  color: #667085;
  font-size: 12px;
}
.force-revoke-actions > div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.force-revoke-empty {
  padding: 24px 0;
  border: 1px dashed #e7edf5;
  border-radius: 10px;
  background: #fbfdff;
}
.force-revoke-permission-list {
  display: grid;
  max-height: 360px;
  overflow-y: auto;
  gap: 8px;
  padding: 2px 4px 2px 0;
}
.force-revoke-permission-item {
  display: flex;
  align-items: center;
  min-height: 40px;
  margin-left: 0;
  padding: 8px 10px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}
.verification-section {
  background: #f8fbff;
  min-height: 58vh;
}
.compact-verification-section {
  min-height: 0;
}
.section-alert {
  margin-bottom: 12px;
}
.action-section + .action-section {
  margin-top: 16px;
}
.section-head {
  align-items: flex-start;
  margin-bottom: 12px;
}
.head-actions {
  align-items: center;
  justify-content: flex-end;
}
.permission-subset-section {
  overflow: hidden;
  padding: 0;
  background: #fff;
}
.subset-section-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e7edf5;
  background: linear-gradient(135deg, #f7fbff 0%, #ffffff 60%, #f8fbff 100%);
}
.subset-title-block {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  min-width: 0;
}
.step-badge {
  flex: 0 0 auto;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eaf3ff;
  color: #1677ff;
  font-size: 12px;
  font-weight: 650;
}
.subset-title-block h4 {
  margin: 0 0 6px;
  color: #1f2d3d;
  font-size: 16px;
}
.subset-title-block span:not(.step-badge),
.subset-stat-item span {
  color: #667085;
  font-size: 12px;
}
.subset-stat-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(72px, 1fr));
  gap: 8px;
  flex: 0 0 auto;
}
.subset-stat-item {
  min-width: 72px;
  padding: 8px 10px;
  border: 1px solid #edf2f7;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.86);
  text-align: center;
}
.subset-stat-item strong {
  display: block;
  margin-top: 4px;
  color: #1f2d3d;
  font-size: 18px;
  line-height: 1;
}
.permission-subset-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin: 0 16px 14px;
  padding: 14px 0;
  border-bottom: 1px dashed #e7edf5;
}
.subset-search-wrap {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 150px;
  gap: 10px;
  flex: 1 1 auto;
  min-width: 0;
}
.subset-search,
.subset-filter {
  width: 100%;
}
.subset-action-group {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.permission-empty {
  padding: 18px 0;
  border: 1px dashed #e7edf5;
  border-radius: 12px;
  background: #fbfdff;
}
.permission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
  padding: 0 16px;
}
.permission-item-wrap {
  position: relative;
}
.permission-item {
  display: flex;
  align-items: center;
  min-height: 42px;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  padding: 9px 12px;
  margin-left: 0;
  background: #fff;
  transition: all 0.2s ease;
}
.permission-item:hover {
  border-color: #91caff;
  background: #f8fbff;
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.08);
}
.table-drilldown-btn {
  position: absolute;
  top: 8px;
  right: 4px;
  font-size: 11px;
  padding: 2px 6px;
}
.table-expansion-panel {
  margin-top: 4px;
  padding: 12px 14px;
  border: 1px solid #d9e8ff;
  border-radius: 8px;
  background: #f8fbff;
}
.table-expansion-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}
.table-expansion-item {
  display: block;
  margin-left: 0;
  padding: 8px 12px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}
.table-expansion-item:hover {
  border-color: #91caff;
  background: #f0f7ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.06);
}
.table-expansion-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #e7edf5;
}
.permission-item-main {
  display: inline-block;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  color: #344054;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.permission-pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 14px 16px 0;
  padding: 12px 0 2px;
  border-top: 1px dashed #e7edf5;
}
.pagination-hint {
  color: #8a94a6;
  font-size: 12px;
}
.permission-subset-section > .field-hint {
  margin: 12px 16px 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
}
.action-row {
  align-items: center;
  flex-wrap: wrap;
  margin-top: 12px;
}
.compact-row {
  margin-top: 0;
}
.direct-resource-box {
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid #d9e8ff;
  border-radius: 10px;
  background: #f8fbff;
}
.compact-head {
  align-items: center;
}
.table-scope-grid {
  display: grid;
  grid-template-columns: minmax(180px, 0.7fr) minmax(260px, 1.3fr);
  gap: 12px;
}
.inline-tools {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.governance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}
.batch-result {
  margin-top: 12px;
}
.batch-items {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}
.batch-item-row {
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}
@media (max-width: 1600px) {
  .summary-banner {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 1200px) {
  .summary-banner,
  .info-metrics,
  .governance-grid,
  .table-scope-grid,
  .binding-overview-grid,
  .subject-context-grid,
  .execution-verification-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .subset-section-hero,
  .permission-subset-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .subset-stat-strip,
  .subset-search-wrap,
  .subset-action-group {
    width: 100%;
  }
  .subset-action-group {
    justify-content: flex-start;
  }
}
@media (max-width: 768px) {
  .workspace-header,
  .permission-preview-head,
  .permission-preview-row {
    flex-direction: column;
  }
  .summary-banner,
  .info-metrics,
  .governance-grid,
  .table-scope-grid,
  .binding-overview-grid,
  .subject-context-grid,
  .execution-verification-grid,
  .subset-search-wrap,
  .subset-stat-strip {
    grid-template-columns: 1fr;
  }
  .permission-pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}

</style>
