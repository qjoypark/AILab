import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { MenuItem } from '@/types/user'

// 默认菜单配置（当后端未返回菜单时使用）
const defaultMenus: MenuItem[] = [
  {
    id: 1,
    path: '/dashboard',
    name: 'Dashboard',
    title: '首页',
    icon: 'HomeFilled',
    meta: { title: '首页', icon: 'HomeFilled' }
  },
  {
    id: 2,
    path: '/system',
    name: 'System',
    title: '系统管理',
    icon: 'Setting',
    meta: { title: '系统管理', icon: 'Setting', permissions: ['system:user:view', 'system:role:view'] },
    children: [
      {
        id: 21,
        path: '/system/users',
        name: 'SystemUsers',
        title: '用户管理',
        meta: { title: '用户管理', permissions: ['system:user:view'] }
      },
      {
        id: 22,
        path: '/system/roles',
        name: 'SystemRoles',
        title: '角色管理',
        meta: { title: '角色管理', permissions: ['system:role:view'] }
      }
    ]
  },
  {
    id: 3,
    path: '/materials',
    name: 'Materials',
    title: '药品管理',
    icon: 'Box',
    meta: { title: '药品管理', icon: 'Box', permissions: ['material:view'] },
    children: [
      {
        id: 31,
        path: '/materials',
        name: 'MaterialList',
        title: '药品列表',
        meta: { title: '药品列表', permissions: ['material:view'] }
      },
      {
        id: 32,
        path: '/materials/categories',
        name: 'MaterialCategories',
        title: '分类管理',
        meta: { title: '分类管理', permissions: ['material:category:view'] }
      },
      {
        id: 33,
        path: '/suppliers',
        name: 'Suppliers',
        title: '供应商管理',
        meta: { title: '供应商管理', permissions: ['supplier:view'] }
      }
    ]
  },
  {
    id: 4,
    path: '/inventory',
    name: 'Inventory',
    title: '库存管理',
    icon: 'Goods',
    meta: { title: '库存管理', icon: 'Goods', permissions: ['inventory:view'] },
    children: [
      {
        id: 41,
        path: '/inventory/stock',
        name: 'InventoryStock',
        title: '库存查询',
        meta: { title: '库存查询', permissions: ['inventory:view'] }
      },
      {
        id: 42,
        path: '/inventory/stock-in',
        name: 'StockIn',
        title: '入库管理',
        meta: { title: '入库管理', permissions: ['inventory:in:view'] }
      },
      {
        id: 43,
        path: '/inventory/stock-out',
        name: 'StockOut',
        title: '出库管理',
        meta: { title: '出库管理', permissions: ['inventory:out:view'] }
      },
      {
        id: 44,
        path: '/inventory/stock-check',
        name: 'StockCheck',
        title: '库存盘点',
        meta: { title: '库存盘点', permissions: ['inventory:check:view'] }
      }
    ]
  },
  {
    id: 5,
    path: '/applications',
    name: 'Applications',
    title: '申请审批',
    icon: 'Document',
    meta: { title: '申请审批', icon: 'Document', permissions: ['application:view'] },
    children: [
      {
        id: 51,
        path: '/applications',
        name: 'ApplicationList',
        title: '领用申请',
        meta: { title: '领用申请', permissions: ['application:view'] }
      },
      {
        id: 52,
        path: '/applications/my',
        name: 'MyApplications',
        title: '我的申请',
        meta: { title: '我的申请' }
      },
      {
        id: 53,
        path: '/applications/pending',
        name: 'PendingApplications',
        title: '待审批',
        meta: { title: '待审批', permissions: ['application:approve'] }
      }
    ]
  },
  {
    id: 6,
    path: '/hazardous',
    name: 'Hazardous',
    title: '危化品管理',
    icon: 'Warning',
    meta: { title: '危化品管理', icon: 'Warning', permissions: ['hazardous:view'] },
    children: [
      {
        id: 61,
        path: '/hazardous/materials',
        name: 'HazardousMaterials',
        title: '危化品列表',
        meta: { title: '危化品列表', permissions: ['hazardous:view'] }
      },
      {
        id: 62,
        path: '/hazardous/usage',
        name: 'HazardousUsage',
        title: '使用记录',
        meta: { title: '使用记录', permissions: ['hazardous:usage:view'] }
      },
      {
        id: 63,
        path: '/hazardous/ledger',
        name: 'HazardousLedger',
        title: '危化品台账',
        meta: { title: '危化品台账', permissions: ['hazardous:ledger:view'] }
      }
    ]
  },
  {
    id: 7,
    path: '/alerts',
    name: 'Alerts',
    title: '预警通知',
    icon: 'Bell',
    meta: { title: '预警通知', icon: 'Bell', permissions: ['alert:view'] },
    children: [
      {
        id: 71,
        path: '/alerts',
        name: 'AlertList',
        title: '预警列表',
        meta: { title: '预警列表', permissions: ['alert:view'] }
      },
      {
        id: 72,
        path: '/notifications',
        name: 'Notifications',
        title: '消息中心',
        meta: { title: '消息中心' }
      },
      {
        id: 73,
        path: '/todos',
        name: 'Todos',
        title: '待办事项',
        meta: { title: '待办事项' }
      }
    ]
  },
  {
    id: 8,
    path: '/reports',
    name: 'Reports',
    title: '报表统计',
    icon: 'DataAnalysis',
    meta: { title: '报表统计', icon: 'DataAnalysis', permissions: ['report:view'] },
    children: [
      {
        id: 81,
        path: '/reports/stock-summary',
        name: 'StockSummary',
        title: '库存汇总',
        meta: { title: '库存汇总', permissions: ['report:stock:view'] }
      },
      {
        id: 82,
        path: '/reports/consumption',
        name: 'ConsumptionReport',
        title: '消耗统计',
        meta: { title: '消耗统计', permissions: ['report:consumption:view'] }
      },
      {
        id: 83,
        path: '/reports/audit-logs',
        name: 'AuditLogs',
        title: '审计日志',
        meta: { title: '审计日志', permissions: ['report:audit:view'] }
      }
    ]
  }
]

export function useMenu() {
  const userStore = useUserStore()

  // 过滤菜单：根据权限过滤
  const filterMenus = (menus: MenuItem[]): MenuItem[] => {
    return menus.filter(menu => {
      // 如果菜单有权限要求，检查用户是否有权限
      if (menu.meta?.permissions && menu.meta.permissions.length > 0) {
        if (!userStore.hasAnyPermission(menu.meta.permissions)) {
          return false
        }
      }

      // 递归过滤子菜单
      if (menu.children && menu.children.length > 0) {
        menu.children = filterMenus(menu.children)
        // 如果所有子菜单都被过滤掉了，也过滤掉父菜单
        if (menu.children.length === 0) {
          return false
        }
      }

      return true
    })
  }

  // 获取可访问的菜单列表
  const accessibleMenus = computed(() => {
    // 优先使用后端返回的菜单
    const menus = userStore.menuList.length > 0 ? userStore.menuList : defaultMenus
    return filterMenus(JSON.parse(JSON.stringify(menus)))
  })

  return {
    accessibleMenus
  }
}
