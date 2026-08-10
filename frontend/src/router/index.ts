import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './permission'

// 静态路由（无需权限）
export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '运维大屏', icon: 'Odometer' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  },
  {
    path: '/grain',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { title: '粮库业务', icon: 'Files', permission: 'grain', requiresAuth: true },
    children: [
      {
        path: 'depot',
        name: 'GrainDepot',
        component: () => import('@/views/grain/depot/Index.vue'),
        meta: { title: '粮库管理', icon: 'OfficeBuilding', requiresAuth: true, permission: 'grain:depot:view' }
      },
      {
        path: 'warehouse',
        name: 'GrainWarehouse',
        component: () => import('@/views/grain/warehouse/Index.vue'),
        meta: { title: '粮仓管理', icon: 'House', requiresAuth: true, permission: 'grain:warehouse:view' }
      },
      {
        path: 'batch',
        name: 'GrainBatch',
        component: () => import('@/views/grain/batch/Index.vue'),
        meta: { title: '批次管理', icon: 'Document', requiresAuth: true, permission: 'grain:batch:view' }
      },
      {
        path: 'inbound',
        name: 'GrainInbound',
        component: () => import('@/views/grain/inbound/Index.vue'),
        meta: { title: '入库管理', icon: 'Download', requiresAuth: true, permission: 'grain:inbound:view' }
      },
      {
        path: 'outbound',
        name: 'GrainOutbound',
        component: () => import('@/views/grain/outbound/Index.vue'),
        meta: { title: '出库管理', icon: 'Upload', requiresAuth: true, permission: 'grain:outbound:view' }
      },
      {
        path: 'inventory',
        name: 'GrainInventory',
        component: () => import('@/views/grain/inventory/Index.vue'),
        meta: { title: '库存查询', icon: 'DataAnalysis', requiresAuth: true, permission: 'grain:inventory:view' }
      }
    ]
  },
  {
    path: '/iot',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { title: 'IoT 监控', icon: 'Monitor', requiresAuth: true, permission: 'iot' },
    children: [
      {
        path: 'monitor',
        name: 'IotMonitor',
        component: () => import('@/views/iot/monitor/Index.vue'),
        meta: { title: '实时监控', icon: 'Monitor', requiresAuth: true, permission: 'iot:reading:view' }
      },
      {
        path: 'sensor',
        name: 'IotSensor',
        component: () => import('@/views/iot/sensor/Index.vue'),
        meta: { title: '传感器管理', icon: 'Cpu', requiresAuth: true, permission: 'iot:sensor:view' }
      }
    ]
  },
  {
    path: '/alert',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { title: '告警管理', icon: 'Warning', requiresAuth: true, permission: 'alert' },
    children: [
      {
        path: 'record',
        name: 'AlertRecord',
        component: () => import('@/views/alert/record/Index.vue'),
        meta: { title: '告警记录', icon: 'BellFilled', requiresAuth: true, permission: 'alert:record:view' }
      },
      {
        path: 'stats',
        name: 'AlertStats',
        component: () => import('@/views/alert/stats/Index.vue'),
        meta: { title: '告警统计', icon: 'DataLine', requiresAuth: true, permission: 'alert:stats:view' }
      }
    ]
  },
  {
    path: '/system',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { title: '系统管理', icon: 'Setting', permission: 'system' },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/Index.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'system:user:view' }
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/Index.vue'),
        meta: { title: '角色管理', icon: 'UserFilled', permission: 'system:role:view' }
      },
      {
        path: 'permission',
        name: 'SystemPermission',
        component: () => import('@/views/system/permission/Index.vue'),
        meta: { title: '权限管理', icon: 'Key', permission: 'system:permission:view' }
      },
      {
        path: 'audit',
        name: 'SystemAudit',
        component: () => import('@/views/system/audit/Index.vue'),
        meta: { title: '审计日志', icon: 'Document', permission: 'audit:log:view' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', public: true }
  }
]

export const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 })
})

setupGuards(router)

export default router
