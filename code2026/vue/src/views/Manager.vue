<template>
  <div class="manager-shell">
    <header class="manager-header">
      <div class="brand"><img src="@/assets/imgs/logo.png" alt="logo"><span>个人记忆库管理台</span></div>
      <div class="header-actions">
        <el-button link class="front-link" @click="router.push('/front/overview')">前台总览</el-button>
        <el-dropdown>
          <div class="user-trigger"><img :src="data.user.avatar || defaultAvatar"><span>{{ data.user.name || data.user.username || '管理员' }}</span></div>
          <template #dropdown><el-dropdown-menu><el-dropdown-item @click="router.push('/manager/person')">个人信息</el-dropdown-item><el-dropdown-item @click="router.push('/manager/password')">修改密码</el-dropdown-item><el-dropdown-item divided @click="logout">退出系统</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </div>
    </header>
    <div class="manager-body">
      <aside class="sidebar">
        <el-menu router :default-active="router.currentRoute.value.path">
          <el-menu-item index="/manager/person"><el-icon><User /></el-icon><span>个人信息</span></el-menu-item>
          <el-menu-item index="/manager/password"><el-icon><Lock /></el-icon><span>修改密码</span></el-menu-item>
          <el-menu-item @click="logout"><el-icon><SwitchButton /></el-icon><span>退出系统</span></el-menu-item>
        </el-menu>
      </aside>
      <main class="manager-content"><router-view @updateUser="updateUser" /></main>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import router from '@/router'
import { ElMessage } from 'element-plus'
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const data = reactive({ user: JSON.parse(localStorage.getItem('system-user') || '{}') })
if (!data.user?.id) { ElMessage.error('请登录！'); router.push('/login') }
const updateUser = () => { data.user = JSON.parse(localStorage.getItem('system-user') || '{}') }
const logout = () => { localStorage.removeItem('system-user'); router.push('/login'); ElMessage.success('退出成功') }
</script>

<style scoped>
.manager-shell { min-height: 100vh; background: #f5f7fb; }.manager-header { height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 26px; background: #1f2937; color: white; }.brand, .user-trigger { display: flex; align-items: center; gap: 10px; }.brand { font-size: 20px; font-weight: 700; }.brand img, .user-trigger img { width: 36px; height: 36px; border-radius: 50%; object-fit: contain; }.header-actions { display: flex; align-items: center; gap: 18px; }.front-link { color: #bfdbfe; }.user-trigger { cursor: pointer; }.manager-body { display: flex; min-height: calc(100vh - 64px); }.sidebar { width: 220px; flex: 0 0 220px; background: white; border-right: 1px solid #e5e7eb; }.sidebar :deep(.el-menu) { border-right: 0; }.sidebar :deep(.el-menu-item.is-active) { background: #eaf2ff; color: #2563eb; }.manager-content { flex: 1; min-width: 0; padding: 18px; }
</style>
