<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    await router.push((route.query.redirect as string) || '/admin/posts')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败，请检查用户名和密码是否正确'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-card animate-slide-up">
      <div class="login-header">
        <h1>管理员登录</h1>
        <p class="muted">请输入您的账号信息</p>
      </div>

      <form class="form" autocomplete="off" @submit.prevent="submit">
        <div class="form-group">
          <label class="form-label" for="username">用户名</label>
          <input
            id="username"
            v-model="username"
            name="qianyu-admin-username"
            type="text"
            class="form-input"
            autocomplete="off"
            placeholder="请输入用户名"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="password">密码</label>
          <input
            id="password"
            v-model="password"
            name="qianyu-admin-password"
            type="password"
            class="form-input"
            autocomplete="new-password"
            placeholder="请输入密码"
            required
          />
        </div>

        <div v-if="error" class="alert alert-error">
          {{ error }}
        </div>

        <button type="submit" class="btn btn-primary btn-lg w-full" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  background:
    radial-gradient(circle at 10% 10%, rgba(255, 255, 255, 0.04), transparent 28rem),
    var(--base);
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: var(--space-8);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.login-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.login-header h1 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: var(--space-2);
}

.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.w-full {
  width: 100%;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
