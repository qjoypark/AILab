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
  StorageLocation
} from '@/types/inventory'

interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
}

const toListResult = <T>(result: PageResult<T>, mapper?: (item: any) => T) => {
  const list = (result?.records ?? result?.list ?? []).map(item => mapper ? mapper(item) : item)
  return {
    list,
    total: result?.total ?? list.length
  }
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
  stockInType: item.stockInType ?? item.inType
})

const mapStockOut = (item: any): StockOut => ({
  ...item,
  stockOutCode: item.stockOutCode ?? item.outOrderNo,
  stockOutType: item.stockOutType ?? item.outType
})

const mapStockCheck = (item: any): StockCheck => ({
  ...item,
  checkCode: item.checkCode ?? item.checkNo,
  checkType: item.checkType ?? 1
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

  getStockInList(params: { keyword?: string; status?: number; page?: number; size?: number }) {
    return request
      .get<any, PageResult<StockIn>>('/inventory/stock-in', { params })
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

  confirmStockIn(id: number) {
    return request.post(`/inventory/stock-in/${id}/confirm`)
  },

  deleteStockIn(id: number) {
    return request.post(`/inventory/stock-in/${id}/cancel`)
  },

  getStockOutList(params: { keyword?: string; status?: number; page?: number; size?: number }) {
    return request
      .get<any, PageResult<StockOut>>('/inventory/stock-out', { params })
      .then(result => toListResult(result, mapStockOut))
  },

  getStockOutById(id: number) {
    return request.get<any, StockOut>(`/inventory/stock-out/${id}`).then(mapStockOut)
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

  getStockCheckList(params: { keyword?: string; status?: number; page?: number; size?: number }) {
    return request
      .get<any, PageResult<StockCheck>>('/inventory/stock-check', { params })
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

  getLocationList(warehouseId: number) {
    return request.get<any, StorageLocation[]>(`/inventory/storage-locations/by-warehouse/${warehouseId}`)
  }
}
