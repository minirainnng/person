<template>
  <div class="front-shell">
    <header class="front-header">
      <el-menu router :default-active="router.currentRoute.value.path" mode="horizontal" class="front-menu">
        <el-menu-item index="/front/overview" @click="router.push('/front/overview')">总览</el-menu-item>
        <el-menu-item index="/front/memories" @click="router.push('/front/memories')">我的记忆库</el-menu-item>
        <el-menu-item index="/front/sops">SOP 手册</el-menu-item>
      </el-menu>
      <div class="user-area">
        <el-dropdown>
          <div class="user-trigger">
            <img :src="data.user.avatar || defaultAvatar" alt="avatar">
            <span>{{ data.user.name || data.user.username || '我的账户' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/front/person')">个人信息</el-dropdown-item>
              <el-dropdown-item @click="router.push('/front/password')">修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="front-content"><router-view @updateUser="updateUser" /></main>
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
.front-shell { min-height: 100vh; background: #f5f7fb; }
.front-header { height: 64px; display: flex; align-items: center; padding: 0 28px; background: #1f2937; color: white; }
.front-menu { flex: 1; border-bottom: 0; background: transparent; }
.front-menu :deep(.el-menu-item) { color: #dbeafe; height: 64px; border-bottom: 0; }
.front-menu :deep(.el-menu-item.is-active) { color: white; background: #2563eb; }
.user-area { width: 220px; display: flex; justify-content: flex-end; }
.user-trigger { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-trigger img { width: 36px; height: 36px; border-radius: 50%; }
.front-content { min-height: calc(100vh - 64px); }
</style>
