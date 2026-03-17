import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { MenuItem } from '@/types/user'

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
    children: [
      {
        id: 21,
        path: '/system/users',
        name: 'UserManagement',
        title: '用户管理',
        meta: { title: '用户管理', permissions: ['system:user:list'] }
      },
      {
        id: 22,
        path: '/system/roles',
        name: 'RoleManagement',
        title: '角色管理',
        meta: { title: '角色管理', permissions: ['system:role:list'] }
      }
    ]
  },
  {
    id: 3,
    path: '/materials',
    name: 'Materials',
    title: '物资管理',
    icon: 'Box',
    children: [
      {
        id: 31,
        path: '/materials',
        name: 'MaterialList',
        title: '物资列表',
        meta: { title: '物资列表', permissions: ['material:list'] }
      }
    ]
  },
  {
    id: 4,
    path: '/inventory',
    name: 'Inventory',
    title: '库存管理',
    icon: 'Goods',
    children: [
      {
        id: 41,
        path: '/inventory/stock',
        name: 'StockList',
        title: '库存查询',
        meta: { title: '库存查询', permissions: ['inventory:stock:list'] }
      },
      {
        id: 42,
        path: '/inventory/stock-in',
        name: 'StockInManagement',
        title: '入库管理',
        meta: { title: '入库管理', permissions: ['inventory:stock-in:list'] }
      },
      {
        id: 43,
        path: '/inventory/stock-out',
        name: 'StockOutManagement',
        title: '出库管理',
        meta: { title: '出库管理', permissions: ['inventory:stock-out:list'] }
      },
      {
        id: 44,
        path: '/inventory/stock-check',
        name: 'StockCheckManagement',
        title: '库存盘点',
        meta: { title: '库存盘点', permissions: ['inventory:stock-check:list'] }
      }
    ]
  },
  {
    id: 5,
    path: '/approval',
    name: 'Approval',
    title: '申请审批',
    icon: 'Document',
    children: [
      {
        id: 51,
        path: '/applications',
        name: 'ApplicationList',
        title: '领用申请',
        meta: { title: '领用申请', permissions: ['application:list'] }
      },
      {
        id: 52,
        path: '/approval/todo',
        name: 'ApprovalTodo',
        title: '待审批事项',
        meta: { title: '待审批事项', permissions: ['application:approve'] }
      }
    ]
  },
  {
    id: 6,
    path: '/hazardous',
    name: 'Hazardous',
    title: '危化品管理',
    icon: 'Warning',
    children: [
      {
        id: 61,
        path: '/hazardous/usage-records',
        name: 'UsageRecordList',
        title: '使用记录',
        meta: { title: '使用记录', permissions: ['hazardous:usage:list'] }
      },
      {
        id: 62,
        path: '/hazardous/ledger',
        name: 'HazardousLedger',
        title: '危化品台账',
        meta: { title: '危化品台账', permissions: ['hazardous:ledger:view'] }
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
        meta: { title: '预警管理', permissions: ['alert:list'] }
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

  const filterMenus = (menus: MenuItem[]): MenuItem[] => {
    return menus
      .map(menu => {
        const cloned: MenuItem = {
          ...menu,
          children: menu.children ? filterMenus(menu.children) : undefined
        }
        return cloned
      })
      .filter(menu => {
        const permissions = menu.meta?.permissions ?? []
        const hasMenuPermission = permissions.length === 0 || userStore.hasAnyPermission(permissions)

        if (menu.children && menu.children.length > 0) {
          return hasMenuPermission && menu.children.length > 0
        }

        return hasMenuPermission
      })
  }

  const accessibleMenus = computed(() => {
    const menus = userStore.menuList.length > 0 ? userStore.menuList : defaultMenus
    return filterMenus(menus)
  })

  return {
    accessibleMenus
  }
}
