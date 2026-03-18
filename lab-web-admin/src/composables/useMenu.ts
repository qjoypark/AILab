import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { MenuItem } from '@/types/user'
import {
  ALERT_PERMISSIONS,
  APPROVAL_PERMISSIONS,
  HAZARDOUS_PERMISSIONS,
  INVENTORY_MODULE_PERMISSIONS,
  INVENTORY_STOCK_CHECK_PERMISSIONS,
  INVENTORY_STOCK_IN_PERMISSIONS,
  INVENTORY_STOCK_OUT_PERMISSIONS,
  INVENTORY_STOCK_PERMISSIONS,
  MATERIAL_PERMISSIONS
} from '@/constants/permissions'

const materialMenuPathPrefixes = ['/materials', '/instruments']
const inventoryMenuPathPrefixes = ['/inventory']
const approvalMenuPathPrefixes = ['/applications', '/approval']
const hazardousMenuPathPrefixes = ['/hazardous']
const alertMenuPathPrefixes = ['/alerts']

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
    meta: { title: '申请审批', permissions: [...APPROVAL_PERMISSIONS] },
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
        meta: { title: '待审批事项', permissions: [...APPROVAL_PERMISSIONS] }
      }
    ]
  },
  {
    id: 6,
    path: '/hazardous',
    name: 'Hazardous',
    title: '危化品管理',
    icon: 'Warning',
    meta: { title: '危化品管理', permissions: [...HAZARDOUS_PERMISSIONS] },
    children: [
      {
        id: 61,
        path: '/hazardous/usage-records',
        name: 'UsageRecordList',
        title: '使用记录',
        meta: { title: '使用记录', permissions: [...HAZARDOUS_PERMISSIONS] }
      },
      {
        id: 62,
        path: '/hazardous/ledger',
        name: 'HazardousLedger',
        title: '危化品台账',
        meta: { title: '危化品台账', permissions: [...HAZARDOUS_PERMISSIONS] }
      }
    ]
  },
  {
    id: 7,
    path: '/notify',
    name: 'Notify',
    title: '预警通知',
    icon: 'Bell',
    children: [
      {
        id: 71,
        path: '/alerts',
        name: 'AlertList',
        title: '预警管理',
        meta: { title: '预警管理', permissions: [...ALERT_PERMISSIONS] }
      },
      {
        id: 72,
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

  const hasMaterialModuleAccess = () => userStore.hasAnyPermission([...MATERIAL_PERMISSIONS])
  const hasInventoryModuleAccess = () => userStore.hasAnyPermission([...INVENTORY_MODULE_PERMISSIONS])
  const hasApprovalModuleAccess = () => userStore.hasAnyPermission([...APPROVAL_PERMISSIONS])
  const hasHazardousModuleAccess = () => userStore.hasAnyPermission([...HAZARDOUS_PERMISSIONS])

  const startsWithAnyPrefix = (path: string, prefixes: string[]) => prefixes.some(prefix => path.startsWith(prefix))

  const isMaterialMenu = (menu: MenuItem): boolean => {
    const path = (menu.path ?? '').toLowerCase()
    if (startsWithAnyPrefix(path, materialMenuPathPrefixes)) {
      return true
    }
    const name = (menu.name ?? '').toLowerCase()
    if (['materials', 'materiallist', 'instrumentlist'].includes(name)) {
      return true
    }
    return !!menu.children?.some(child => isMaterialMenu(child))
  }

  const isInventoryMenu = (menu: MenuItem): boolean => {
    const path = (menu.path ?? '').toLowerCase()
    if (startsWithAnyPrefix(path, inventoryMenuPathPrefixes)) {
      return true
    }
    const name = (menu.name ?? '').toLowerCase()
    if (['inventory', 'stocklist', 'stockinmanagement', 'stockoutmanagement', 'stockcheckmanagement'].includes(name)) {
      return true
    }
    return !!menu.children?.some(child => isInventoryMenu(child))
  }

  const isApprovalMenu = (menu: MenuItem): boolean => {
    const path = (menu.path ?? '').toLowerCase()
    if (startsWithAnyPrefix(path, approvalMenuPathPrefixes)) {
      return true
    }
    const name = (menu.name ?? '').toLowerCase()
    if (['approval', 'applicationlist', 'approvaltodo'].includes(name)) {
      return true
    }
    return !!menu.children?.some(child => isApprovalMenu(child))
  }

  const isHazardousMenu = (menu: MenuItem): boolean => {
    const path = (menu.path ?? '').toLowerCase()
    if (startsWithAnyPrefix(path, hazardousMenuPathPrefixes)) {
      return true
    }
    const name = (menu.name ?? '').toLowerCase()
    if (['hazardous', 'usagerecordlist', 'hazardousledger'].includes(name)) {
      return true
    }
    return !!menu.children?.some(child => isHazardousMenu(child))
  }

  const isAlertMenu = (menu: MenuItem): boolean => {
    const path = (menu.path ?? '').toLowerCase()
    if (startsWithAnyPrefix(path, alertMenuPathPrefixes)) {
      return true
    }
    const name = (menu.name ?? '').toLowerCase()
    if (['notify', 'alertlist'].includes(name)) {
      return true
    }
    return !!menu.children?.some(child => isAlertMenu(child))
  }

  const filterMenus = (menus: MenuItem[]): MenuItem[] => {
    return menus
      .map(menu => ({
        ...menu,
        children: menu.children ? filterMenus(menu.children) : undefined
      }))
      .filter(menu => {
        if (isMaterialMenu(menu) && !hasMaterialModuleAccess()) {
          return false
        }
        if (isInventoryMenu(menu) && !hasInventoryModuleAccess()) {
          return false
        }
        if (isApprovalMenu(menu) && !hasApprovalModuleAccess()) {
          return false
        }
        if (isHazardousMenu(menu) && !hasHazardousModuleAccess()) {
          return false
        }

        const roles = menu.meta?.roles ?? []
        const permissions = menu.meta?.permissions ?? []
        const hasMenuRole = roles.length === 0 || userStore.hasRole(roles)
        const hasMenuPermission = permissions.length === 0 || userStore.hasAnyPermission(permissions)
        const hasMenuAccess = hasMenuRole && hasMenuPermission

        if (menu.children && menu.children.length > 0) {
          return hasMenuAccess && menu.children.length > 0
        }

        if (isAlertMenu(menu)) {
          return hasMenuAccess
        }

        return hasMenuAccess
      })
  }

  const accessibleMenus = computed(() => filterMenus(defaultMenus))

  return {
    accessibleMenus
  }
}
