import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import type {
  StockInventory,
  StockQuery,
  StockIn,
  StockInForm,
  StockOut,
  StockOutForm,
  StockCheck,
  StockCheckForm,
  StockCheckDetail,
  Warehouse,
  WarehouseQuery,
  WarehouseForm,
  StorageLocation
} from '@/types/inventory'

interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
}

interface StockInImportPreview {
  warehouseId: number
  stockInType: number
  remark?: string
  items: Array<{
    materialId: number
    quantity: number
    unitPrice?: number
    batchNumber?: string
    productionDate?: string
    expiryDate?: string
  }>
}

const toListResult = <T>(result: PageResult<T>, mapper?: (item: any) => T) => {
  const list = (result?.records ?? result?.list ?? []).map(item => mapper ? mapper(item) : item)
  return {
    list,
    total: result?.total ?? list.length
  }
}

const normalizeStatusValue = (value: unknown): number => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : 0
}

const mapStockInventory = (item: any): StockInventory => ({
  ...item,
  locationId: item.locationId ?? item.storageLocationId,
  expiryDate: item.expiryDate ?? item.expireDate,
  totalValue: item.totalValue ?? item.totalAmount
})

const mapStockIn = (item: any): StockIn => ({
  ...item,
  stockInCode: item.stockInCode ?? item.inOrderNo,
  stockInType: item.stockInType ?? item.inType,
  status: normalizeStatusValue(item.status) === 1 ? 0 : normalizeStatusValue(item.status) === 2 ? 1 : 2,
  items: (item.items ?? []).map((detail: any) => ({
    ...detail,
    stockInId: detail.stockInId ?? detail.inOrderId,
    expiryDate: detail.expiryDate ?? detail.expireDate,
    totalPrice: detail.totalPrice ?? detail.totalAmount,
    locationId: detail.locationId ?? detail.storageLocationId
  }))
})

const mapStockOut = (item: any): StockOut => ({
  ...item,
  stockOutCode: item.stockOutCode ?? item.outOrderNo,
  stockOutType: item.stockOutType ?? item.outType,
  status: normalizeStatusValue(item.status) === 1 ? 0 : normalizeStatusValue(item.status) === 2 ? 1 : 2,
  createdBy: item.createdBy ?? item.operatorId,
  createdTime: item.createdTime ?? item.outDate,
  items: (item.items ?? []).map((detail: any) => ({
    ...detail,
    stockOutId: detail.stockOutId ?? detail.outOrderId,
    totalPrice: detail.totalPrice ?? detail.totalAmount,
    locationId: detail.locationId ?? detail.storageLocationId
  }))
})

const mapStockCheck = (item: any): StockCheck => ({
  ...item,
  checkCode: item.checkCode ?? item.checkNo,
  checkType: item.checkType ?? 1,
  status: normalizeStatusValue(item.status) === 1 ? 0 : normalizeStatusValue(item.status) === 2 ? 1 : 2,
  createdBy: item.createdBy ?? item.checkerId,
  completedTime: item.completedTime ?? (item.status === 2 ? item.updatedTime : undefined),
  items: (item.items ?? []).map((detail: any) => ({
    ...detail,
    differenceQuantity: detail.differenceQuantity ?? detail.diffQuantity,
    differenceReason: detail.differenceReason ?? detail.diffReason,
    locationId: detail.locationId ?? detail.storageLocationId
  }))
})

const today = () => new Date().toISOString().slice(0, 10)

const currentUserId = () => {
  const userStore = useUserStore()
  return userStore.userInfo?.id ?? 1
}

export const inventoryApi = {
  getStockList(params: StockQuery) {
    return request
      .get<any, PageResult<StockInventory>>('/inventory/stock', { params })
      .then(result => toListResult(result, mapStockInventory))
  },

  getStockDetail(materialId: number) {
    return request.get<any, StockInventory[]>(`/inventory/stock/${materialId}/detail`)
      .then(list => list.map(mapStockInventory))
  },

  getStockInList(params: {
    keyword?: string
    warehouseId?: number
    status?: number
    createdTimeStart?: string
    createdTimeEnd?: string
    page?: number
    size?: number
  }) {
    const mappedParams = {
      ...params,
      status: params.status === undefined || params.status === -1
        ? undefined
        : (params.status === 0 ? 1 : params.status === 1 ? 2 : 3)
    }

    return request
      .get<any, PageResult<StockIn>>('/inventory/stock-in', { params: mappedParams })
      .then(result => toListResult(result, mapStockIn))
  },

  getStockInById(id: number) {
    return request.get<any, StockIn>(`/inventory/stock-in/${id}`).then(mapStockIn)
  },

  createStockIn(data: StockInForm) {
    const payload = {
      inType: data.stockInType,
      warehouseId: data.warehouseId,
      supplierId: data.supplierId,
      inDate: today(),
      operatorId: currentUserId(),
      remark: data.remark,
      items: data.items.map(item => ({
        materialId: item.materialId,
        batchNumber: item.batchNumber,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        productionDate: item.productionDate || undefined,
        expireDate: item.expiryDate || undefined,
        storageLocationId: item.locationId
      }))
    }
    return request.post<any, StockIn>('/inventory/stock-in', payload).then(mapStockIn)
  },

  importStockInExcel(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, any>('/inventory/stock-in/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then((data): StockInImportPreview => ({
      warehouseId: data.warehouseId,
      stockInType: data.inType,
      remark: data.remark,
      items: (data.items ?? []).map((item: any) => ({
        materialId: item.materialId,
        quantity: Number(item.quantity ?? 0),
        unitPrice: item.unitPrice == null ? undefined : Number(item.unitPrice),
        batchNumber: item.batchNumber,
        productionDate: item.productionDate,
        expiryDate: item.expireDate
      }))
    }))
  },

  downloadStockInImportTemplate() {
    return request.get<any, Blob>('/inventory/stock-in/import/template', {
      responseType: 'blob'
    })
  },

  confirmStockIn(id: number) {
    return request.post(`/inventory/stock-in/${id}/confirm`)
  },

  deleteStockIn(id: number) {
    return request.post(`/inventory/stock-in/${id}/cancel`)
  },

  getStockOutList(params: {
    keyword?: string
    warehouseId?: number
    status?: number
    createdTimeStart?: string
    createdTimeEnd?: string
    page?: number
    size?: number
  }) {
    const mappedParams = {
      ...params,
      status: params.status === undefined || params.status === -1
        ? undefined
        : (params.status === 0 ? 1 : params.status === 1 ? 2 : 3)
    }

    return request
      .get<any, PageResult<StockOut>>('/inventory/stock-out', { params: mappedParams })
      .then(result => toListResult(result, mapStockOut))
  },

  getStockOutById(id: number) {
    return request.get<any, StockOut>(`/inventory/stock-out/${id}`).then(mapStockOut)
  },

  exportStockOutPdf(id: number) {
    return request.get<any, Blob>(`/inventory/stock-out/${id}/pdf`, {
      responseType: 'blob'
    })
  },

  createStockOut(data: StockOutForm) {
    const payload = {
      outType: data.stockOutType,
      warehouseId: data.warehouseId,
      applicationId: data.applicationId,
      outDate: today(),
      operatorId: currentUserId(),
      receiverId: currentUserId(),
      remark: data.remark,
      items: data.items.map(item => ({
        materialId: item.materialId,
        batchNumber: item.batchNumber,
        quantity: item.quantity,
        storageLocationId: item.locationId
      }))
    }
    return request.post<any, StockOut>('/inventory/stock-out', payload).then(mapStockOut)
  },

  confirmStockOut(id: number) {
    return request.post(`/inventory/stock-out/${id}/confirm`)
  },

  deleteStockOut(id: number) {
    return request.post(`/inventory/stock-out/${id}/cancel`)
  },

  getStockCheckList(params: { keyword?: string; warehouseId?: number; status?: number; page?: number; size?: number }) {
    const mappedParams = {
      ...params,
      status: params.status === undefined || params.status === -1
        ? undefined
        : (params.status === 0 ? 1 : params.status === 1 ? 2 : 3)
    }

    return request
      .get<any, PageResult<StockCheck>>('/inventory/stock-check', { params: mappedParams })
      .then(result => toListResult(result, mapStockCheck))
  },

  getStockCheckById(id: number) {
    return request.get<any, StockCheck>(`/inventory/stock-check/${id}`).then(mapStockCheck)
  },

  createStockCheck(data: StockCheckForm) {
    const payload = {
      warehouseId: data.warehouseId,
      checkDate: today(),
      checkerId: currentUserId(),
      remark: data.remark
    }
    return request.post<any, StockCheck>('/inventory/stock-check', payload).then(mapStockCheck)
  },

  submitCheckItems(id: number, items: StockCheckDetail[]) {
    const payload = {
      items: items.map(item => ({
        materialId: item.materialId,
        batchNumber: item.batchNumber,
        bookQuantity: item.bookQuantity,
        actualQuantity: item.actualQuantity,
        diffReason: item.differenceReason,
        storageLocationId: (item as any).locationId
      }))
    }
    return request.post(`/inventory/stock-check/${id}/items`, payload)
  },

  completeStockCheck(id: number) {
    return request.post(`/inventory/stock-check/${id}/complete`)
  },

  getWarehouseList() {
    return request
      .get<any, PageResult<Warehouse>>('/inventory/warehouses', {
        params: {
          page: 1,
          size: 1000
        }
      })
      .then(result => result?.records ?? result?.list ?? [])
  },

  getWarehousePage(params: WarehouseQuery) {
    return request
      .get<any, PageResult<Warehouse>>('/inventory/warehouses', { params })
      .then(result => toListResult(result))
  },

  getWarehouseById(id: number) {
    return request.get<any, Warehouse>(`/inventory/warehouses/${id}`)
  },

  createWarehouse(data: WarehouseForm) {
    return request.post<any, Warehouse>('/inventory/warehouses', data)
  },

  updateWarehouse(id: number, data: WarehouseForm) {
    return request.put<any, Warehouse>(`/inventory/warehouses/${id}`, data)
  },

  deleteWarehouse(id: number) {
    return request.delete(`/inventory/warehouses/${id}`)
  },

  getLocationList(warehouseId: number) {
    return request.get<any, StorageLocation[]>(`/inventory/storage-locations/by-warehouse/${warehouseId}`)
  }
}
