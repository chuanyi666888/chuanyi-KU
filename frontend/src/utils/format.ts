import dayjs from 'dayjs'

export function formatDateTime(value?: string | number | Date | null): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

export function formatDate(value?: string | number | Date | null): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD')
}

export function formatRelativeTime(value?: string | number | Date | null): string {
  if (!value) return '-'
  const now = dayjs()
  const target = dayjs(value)
  const diffSec = now.diff(target, 'second')
  if (diffSec < 60) return `${diffSec} 秒前`
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} 分钟前`
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} 小时前`
  if (diffSec < 2592000) return `${Math.floor(diffSec / 86400)} 天前`
  return target.format('YYYY-MM-DD')
}

export function formatNumber(value?: number | null, digits = 2): string {
  if (value === null || value === undefined) return '-'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function formatPercent(value?: number | null, digits = 1): string {
  if (value === null || value === undefined) return '-'
  return `${(value * 100).toFixed(digits)}%`
}
