import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { MenuItem } from '@/types/user'
import {
  ALERT_PERMISSIONS,
  APPROVAL_PERMISSIONS,
  APPROVAL_TODO_PERMISSIONS,
  HAZARDOUS_PERMISSIONS,
  INVENTORY_MODULE_PERMISSIONS,
  INVENTORY_STOCK_CHECK_PERMISSIONS,
  INVENTORY_STOCK_IN_PERMISSIONS,
  INVENTORY_STOCK_OUT_PERMISSIONS,
  INVENTORY_STOCK_PERMISSIONS,
  LAB_MODULE_PERMISSIONS,
  LAB_ROOM_PERMISSIONS,
  LAB_USAGE_PERMISSIONS,
  MATERIAL_PERMISSIONS
} from '@/constants/permissions'

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
    meta: { title: '系统管理', roles: ['ADMIN', 'CENTER_ADMIN'] },
    children: [
      {
        id: 21,
        path: '/system/users',
        name: 'UserManagement',
        title: '用户管理',
        meta: { title: '用户管理', roles: ['ADMIN', 'CENTER_ADMIN'] }
      },
      {
        id: 22,
        path: '/system/roles',
        name: 'RoleManagement',
        title: '角色管理',
        meta: { title: '角色管理', roles: ['ADMIN'] }
      },
      {
        id: 23,
        path: '/system/warehouses',
        name: 'WarehouseManagement',
        title: '仓库管理',
        meta: { title: '仓库管理', roles: ['ADMIN'] }
      }
    ]
  },
  {
    id: 3,
    path: '/materials',
    name: 'Materials',
    title: '物资管理',
    icon: 'Box',
    meta: { title: '物资管理', permissions: [...MATERIAL_PERMISSIONS] },
    children: [
      {
        id: 31,
        path: '/materials',
        name: 'MaterialList',
        title: '药品列表',
        meta: { title: '药品列表', permissions: [...MATERIAL_PERMISSIONS] }
      },
      {
        id: 32,
        path: '/instruments',
        name: 'InstrumentList',
        title: '仪器列表',
        meta: { title: '仪器列表', permissions: [...MATERIAL_PERMISSIONS] }
      }
    ]
  },
  {
    id: 4,
    path: '/inventory',
    name: 'Inventory',
    title: '库存管理',
    icon: 'Goods',
    meta: { title: '库存管理', permissions: [...INVENTORY_MODULE_PERMISSIONS] },
    children: [
      {
        id: 41,
        path: '/inventory/stock',
        name: 'StockList',
        title: '库存查询',
        meta: { title: '库存查询', permissions: [...INVENTORY_STOCK_PERMISSIONS] }
      },
      {
        id: 42,
        path: '/inventory/stock-in',
        name: 'StockInManagement',
        title: '入库管理',
        meta: { title: '入库管理', permissions: [...INVENTORY_STOCK_IN_PERMISSIONS] }
      },
      {
        id: 43,
        path: '/inventory/stock-out',
        name: 'StockOutManagement',
        title: '出库管理',
        meta: { title: '出库管理', permissions: [...INVENTORY_STOCK_OUT_PERMISSIONS] }
      },
      {
        id: 44,
        path: '/inventory/stock-check',
        name: 'StockCheckManagement',
        title: '库存盘点',
        meta: { title: '库存盘点', permissions: [...INVENTORY_STOCK_CHECK_PERMISSIONS] }
      }
    ]
  },
  {
    id: 5,
    path: '/approval',
    name: 'Approval',
    title: '申请审批',
    icon: 'Document',
    meta: { title: '申请审批', permissions: [...APPROVAL_PERMISSIONS, ...APPROVAL_TODO_PERMISSIONS] },
    children: [
      {
        id: 51,
        path: '/applications',
        name: 'ApplicationList',
        title: '领用申请',
        meta: { title: '领用申请', permissions: [...APPROVAL_PERMISSIONS] }
      },
      {
        id: 52,
        path: '/approval/todo',
        name: 'ApprovalTodo',
        title: '待审批事项',
        meta: { title: '待审批事项', permissions: [...APPROVAL_TODO_PERMISSIONS] }
      }
    ]
  },
  {
    id: 9,
    path: '/labs',
    name: 'Labs',
    title: '实验室管理',
    icon: 'OfficeBuilding',
    meta: { title: '实验室管理', permissions: [...LAB_MODULE_PERMISSIONS] },
    children: [
      {
        id: 91,
        path: '/labs/rooms',
        name: 'LabRoomList',
        title: '实验室列表',
        meta: { title: '实验室列表', permissions: [...LAB_ROOM_PERMISSIONS] }
      },
      {
        id: 92,
        path: '/labs/usage-applications',
        name: 'LabUsageApplicationList',
        title: '实验室使用申请',
        meta: { title: '实验室使用申请', permissions: [...LAB_USAGE_PERMISSIONS] }
      },
      {
        id: 93,
        path: '/labs/usage-schedule',
        name: 'LabUsageSchedule',
        title: '实验室使用日程',
        meta: { title: '实验室使用日程', permissions: ['lab-usage:schedule:view'] }
      }
    ]
  },
  {
    id: 6,
    path: '/usage-records',
    name: 'MedicationUsageRecordList',
    title: '药品使用',
    icon: 'Tickets',
    meta: { title: '药品使用' }
  },
  {
    id: 7,
    path: '/hazardous',
    name: 'Hazardous',
    title: '危化品管理',
    icon: 'Warning',
    meta: { title: '危化品管理', permissions: [...HAZARDOUS_PERMISSIONS] },
    children: [
      {
        id: 71,
        path: '/hazardous/ledger',
        name: 'HazardousLedger',
        title: '危化品台账',
        meta: { title: '危化品台账', permissions: [...HAZARDOUS_PERMISSIONS] }
      }
    ]
  },
  {
    id: 8,
    path: '/notify',
    name: 'Notify',
    title: '预警通知',
    icon: 'Bell',
    children: [
      {
        id: 81,
        path: '/alerts',
        name: 'AlertList',
        title: '预警管理',
        meta: { title: '预警管理', permissions: [...ALERT_PERMISSIONS] }
      },
      {
        id: 82,
        path: '/notifications',
        name: 'MessageCenter',
        title: '消息中心',
        meta: { title: '消息中心' }
      }
    ]
  }
]

export function useMenu() {
  const userStore = useUserStore()

  const hasMenuAccess = (menu: MenuItem): boolean => {
    const roles = menu.meta?.roles ?? []
    const permissions = menu.meta?.permissions ?? []
    const hasRole = roles.length === 0 || userStore.hasRole(roles)
    const hasPermission = permissions.length === 0 || userStore.hasAnyPermission(permissions)
    return hasRole && hasPermission
  }

  const filterMenus = (menus: MenuItem[]): MenuItem[] => {
    return menus
      .map(menu => ({
        ...menu,
        children: menu.children ? filterMenus(menu.children) : undefined
      }))
      .filter(menu => {
        const accessible = hasMenuAccess(menu)
        if (menu.children && menu.children.length > 0) {
          return accessible && menu.children.length > 0
        }
        return accessible
      })
  }

  const accessibleMenus = computed(() => filterMenus(defaultMenus))

  return {
    accessibleMenus
  }
}
