<script setup lang="ts">
defineProps<{
  visible: boolean
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="emit('cancel')">
        <div class="modal-card">
          <h3 class="modal-title">{{ title || '确认操作' }}</h3>
          <p class="modal-message">{{ message }}</p>
          <div class="modal-actions">
            <button class="modal-btn modal-btn-cancel" @click="emit('cancel')">
              {{ cancelText || '取消' }}
            </button>
            <button
              class="modal-btn"
              :class="danger ? 'modal-btn-danger' : 'modal-btn-confirm'"
              @click="emit('confirm')"
            >
              {{ confirmText || '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.modal-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.12);
  padding: 28px;
}

.modal-title {
  font-family: var(--font-serif, "Noto Serif SC", serif);
  font-size: 1.2rem;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--primary, #2c2c2c);
}

.modal-message {
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--secondary, #8c8c8c);
  margin: 0 0 24px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-btn {
  height: 40px;
  padding: 0 20px;
  font-size: 0.92rem;
  font-weight: 600;
  border-radius: 999px;
  border: 1px solid var(--line, rgba(44, 44, 44, 0.08));
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.modal-btn-cancel {
  background: color-mix(in srgb, var(--surface, #ffffff) 74%, transparent);
  color: var(--primary, #2c2c2c);
}

.modal-btn-cancel:hover {
  background: var(--base, #f9f9f6);
  border-color: var(--secondary, #8c8c8c);
}

.modal-btn-confirm {
  background: var(--primary, #2c2c2c);
  color: #fff;
  border-color: var(--primary, #2c2c2c);
}

.modal-btn-confirm:hover {
  background: #444;
}

.modal-btn-danger {
  background: #DC2626;
  color: #fff;
  border-color: #DC2626;
}

.modal-btn-danger:hover {
  background: #B91C1C;
  border-color: #B91C1C;
}

/* Transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.modal-enter-active .modal-card,
.modal-leave-active .modal-card {
  transition: transform 0.25s cubic-bezier(0.25, 0.8, 0.25, 1), opacity 0.25s;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-card {
  transform: translateY(12px) scale(0.97);
  opacity: 0;
}

.modal-leave-to .modal-card {
  transform: translateY(-8px) scale(0.97);
  opacity: 0;
}
</style>
