export interface Material {
  id: number
  materialCode: string
  materialName: string
  materialType: number // 1-耗材 2-试剂 3-危化品
  categoryId: number
  categoryName?: string
  specification?: string
  unit: string
  manufacturer?: string
  supplierId?: number
  supplierName?: string
  unitPrice?: number
  safetyStock?: number
  imageUrl?: string
  casNumber?: string
  dangerCategory?: string
  isControlled?: number // 0-非管控 1-易制毒 2-易制爆
  storageConditions?: string
  safetyInstructions?: string
  status: number
  createdTime?: string
  updatedTime?: string
}

export interface MaterialQuery {
  keyword?: string
  materialType?: number
  categoryId?: number
  isControlled?: number
  status?: number
  page?: number
  size?: number
}

export interface MaterialForm {
  id?: number
  materialCode: string
  materialName: string
  materialType: number
  categoryId: number
  specification?: string
  unit: string
  manufacturer?: string
  supplierId?: number
  unitPrice?: number
  imageUrl?: string
  casNumber?: string
  dangerCategory?: string
  isControlled?: number
  storageConditions?: string
  safetyInstructions?: string
  status?: number
}

export interface MaterialCategory {
  id: number
  categoryName: string
  parentId?: number
  children?: MaterialCategory[]
}

export interface Supplier {
  id: number
  supplierName: string
  contactPerson?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  status: number
  createdTime?: string
}
