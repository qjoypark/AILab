export interface StockInventory {
  id: number
  materialId: number
  materialCode?: string
  materialName?: string
  warehouseId: number
  warehouseName?: string
  locationId?: number
  locationName?: string
  quantity: number
  availableQuantity: number
  lockedQuantity: number
  unitPrice?: number
  totalValue?: number
  batchNumber?: string
  productionDate?: string
  expiryDate?: string
  status: number
  updatedTime?: string
}

export interface StockQuery {
  keyword?: string
  materialType?: number
  warehouseId?: number
  lowStock?: boolean
  page?: number
  size?: number
}

export interface StockIn {
  id: number
  stockInCode: string
  warehouseId: number
  warehouseName?: string
  operatorId?: number
  operatorName?: string
  supplierId?: number
  supplierName?: string
  stockInType: number // 1-采购入库 2-退货入库 3-盘盈入库
  totalAmount?: number
  status: number // 0-待确认 1-已确认
  remark?: string
  createdBy?: number
  createdByName?: string
  createdTime?: string
  updatedTime?: string
  confirmedBy?: number
  confirmedTime?: string
  items?: StockInDetail[]
}

export interface StockInDetail {
  id?: number
  stockInId?: number
  materialId: number
  materialCode?: string
  materialName?: string
  unit?: string
  quantity: number
  unitPrice?: number
  totalPrice?: number
  batchNumber?: string
  productionDate?: string
  expiryDate?: string
  locationId?: number
  locationName?: string
}

export interface StockInForm {
  warehouseId: number
  supplierId?: number
  stockInType: number
  remark?: string
  items: StockInDetail[]
}

export interface StockOut {
  id: number
  stockOutCode: string
  warehouseId: number
  warehouseName?: string
  stockOutType: number // 1-领用出库 2-报废出库 3-盘亏出库
  applicationId?: number
  totalAmount?: number
  status: number // 0-待确认 1-已确认
  remark?: string
  createdBy?: number
  createdByName?: string
  createdTime?: string
  confirmedBy?: number
  confirmedTime?: string
  items?: StockOutDetail[]
}

export interface StockOutDetail {
  id?: number
  stockOutId?: number
  materialId: number
  materialCode?: string
  materialName?: string
  quantity: number
  unitPrice?: number
  totalPrice?: number
  batchNumber?: string
  locationId?: number
  locationName?: string
}

export interface StockOutForm {
  warehouseId: number
  stockOutType: number
  applicationId?: number
  remark?: string
  items: StockOutDetail[]
}

export interface StockCheck {
  id: number
  checkCode: string
  warehouseId: number
  warehouseName?: string
  checkType: number // 1-全盘 2-抽盘
  status: number // 0-盘点中 1-已完成
  remark?: string
  createdBy?: number
  createdByName?: string
  createdTime?: string
  completedTime?: string
  items?: StockCheckDetail[]
}

export interface StockCheckDetail {
  id?: number
  checkId?: number
  materialId: number
  materialCode?: string
  materialName?: string
  batchNumber?: string
  bookQuantity: number
  actualQuantity: number
  differenceQuantity: number
  differenceReason?: string
}

export interface StockCheckForm {
  warehouseId: number
  checkType: number
  remark?: string
}

export interface Warehouse {
  id: number
  warehouseCode?: string
  warehouseName: string
  warehouseType: number // 1-普通仓库 2-危化品仓库
  location?: string
  managerId?: number
  address?: string
  manager?: string
  phone?: string
  status: number
  createdTime?: string
  updatedTime?: string
}

export interface WarehouseQuery {
  warehouseType?: number
  page?: number
  size?: number
}

export interface WarehouseForm {
  id?: number
  warehouseCode: string
  warehouseName: string
  warehouseType: number
  location?: string
  managerId?: number
  status?: number
}

export interface StorageLocation {
  id: number
  warehouseId: number
  locationCode: string
  locationName: string
  status: number
}
