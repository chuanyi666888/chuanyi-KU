import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-permission 指令
 * 用法：
 *   v-permission="'system:user:create'"
 *   v-permission="['system:user:create', 'system:user:update']"
 *
 * 无权限时元素被移除
 */
export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const userStore = useUserStore()
    const value = binding.value
    let codes: string[] = []
    if (typeof value === 'string') {
      codes = [value]
    } else if (Array.isArray(value)) {
      codes = value
    }
    if (!userStore.hasAnyPermission(codes)) {
      el.parentNode?.removeChild(el)
    }
  }
}
