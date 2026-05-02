export const MATERIAL_PERMISSIONS = ['material:list', 'material:create', 'material:update', 'material:delete'] as const

export const INVENTORY_STOCK_PERMISSIONS = ['inventory:stock:list'] as const

export const INVENTORY_STOCK_IN_PERMISSIONS = [
  'inventory:stock-in:list',
  'inventory:stock-in:create',
  'inventory:stock-in:confirm',
  'inventory:stock-in:delete'
] as const

export const INVENTORY_STOCK_OUT_PERMISSIONS = [
  'inventory:stock-out:list',
  'inventory:stock-out:create',
  'inventory:stock-out:confirm',
  'inventory:stock-out:delete'
] as const

export const INVENTORY_STOCK_CHECK_PERMISSIONS = [
  'inventory:stock-check:list',
  'inventory:stock-check:create',
  'inventory:stock-check:record',
  'inventory:stock-check:complete'
] as const

export const INVENTORY_MODULE_PERMISSIONS = [
  ...INVENTORY_STOCK_PERMISSIONS,
  ...INVENTORY_STOCK_IN_PERMISSIONS,
  ...INVENTORY_STOCK_OUT_PERMISSIONS,
  ...INVENTORY_STOCK_CHECK_PERMISSIONS
] as const

export const APPROVAL_PERMISSIONS = ['application:list', 'application:approve'] as const

export const APPROVAL_TODO_PERMISSIONS = ['application:approve', 'lab-usage:approve'] as const

export const HAZARDOUS_PERMISSIONS = ['hazardous:usage:list', 'hazardous:ledger:view'] as const

export const ALERT_PERMISSIONS = ['alert:list'] as const

export const LAB_ROOM_PERMISSIONS = [
  'lab-room:list',
  'lab-room:create',
  'lab-room:update',
  'lab-room:delete',
  'lab-room:manager:update'
] as const

export const LAB_USAGE_PERMISSIONS = [
  'lab-usage:list',
  'lab-usage:create',
  'lab-usage:cancel',
  'lab-usage:approve',
  'lab-usage:schedule:view'
] as const

export const LAB_MODULE_PERMISSIONS = [
  ...LAB_ROOM_PERMISSIONS,
  ...LAB_USAGE_PERMISSIONS
] as const
