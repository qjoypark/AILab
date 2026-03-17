import request from '@/utils/request'
import type { Material, MaterialQuery, MaterialForm, MaterialCategory, Supplier } from '@/types/material'

interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
}

const toListResult = <T>(result: PageResult<T>) => ({
  list: result?.records ?? result?.list ?? [],
  total: result?.total ?? 0
})

export const materialApi = {
  getMaterialList(params: MaterialQuery) {
    return request.get<any, PageResult<Material>>('/materials', { params }).then(toListResult<Material>)
  },

  getMaterialById(id: number) {
    return request.get<any, Material>(`/materials/${id}`)
  },

  createMaterial(data: MaterialForm) {
    return request.post<any, Material>('/materials', data)
  },

  updateMaterial(id: number, data: MaterialForm) {
    return request.put<any, Material>(`/materials/${id}`, data)
  },

  deleteMaterial(id: number) {
    return request.delete(`/materials/${id}`)
  },

  getCategoryTree() {
    return request.get<any, MaterialCategory[]>('/material-categories/tree')
  },

  createCategory(data: { categoryName: string; parentId?: number }) {
    return request.post<any, MaterialCategory>('/material-categories', data)
  },

  updateCategory(id: number, data: { categoryName: string }) {
    return request.put<any, MaterialCategory>(`/material-categories/${id}`, data)
  },

  deleteCategory(id: number) {
    return request.delete(`/material-categories/${id}`)
  },

  getSupplierList(params?: { keyword?: string; page?: number; size?: number }) {
    return request.get<any, PageResult<Supplier>>('/suppliers', { params }).then(toListResult<Supplier>)
  },

  createSupplier(data: Partial<Supplier>) {
    return request.post<any, Supplier>('/suppliers', data)
  },

  updateSupplier(id: number, data: Partial<Supplier>) {
    return request.put<any, Supplier>(`/suppliers/${id}`, data)
  },

  deleteSupplier(id: number) {
    return request.delete(`/suppliers/${id}`)
  },

  uploadImage(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, { url: string }>('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
