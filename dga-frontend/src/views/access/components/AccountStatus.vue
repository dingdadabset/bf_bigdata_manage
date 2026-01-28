<template>
  <a-form-model :label-col="{ span: 4 }" :wrapper-col="{ span: 14 }">
    <a-form-model-item label="角色模板">
      <a-select default-value="Data Engineer" style="width: 300px">
        <a-select-option value="Data Engineer">Data Engineer (Standard)</a-select-option>
        <a-select-option value="Data Analyst">Data Analyst (Read-Only)</a-select-option>
        <a-select-option value="Admin">Cluster Admin</a-select-option>
      </a-select>
      <p style="color: #999; font-size: 12px; margin-top: 4px">切换角色将自动同步 IPA 组和基础 Ranger 策略</p>
    </a-form-model-item>
    <a-divider />
    <a-form-model-item label="IPA 状态">
      <a-badge status="success" text="Synced" />
      <span style="margin-left: 16px; color: #999">Last sync: 10 mins ago</span>
    </a-form-model-item>
    <a-form-model-item label="Kerberos Principal">
      <code>{{ user.username }}@DGA.COM</code>
    </a-form-model-item>
    <a-form-model-item label="Password Expiry">
      Never
    </a-form-model-item>
  </a-form-model>
</template>

<script>
export default {
  name: 'AccountStatus',
  props: {
    user: {
      type: Object,
      required: true
    }
  }
};
</script>
