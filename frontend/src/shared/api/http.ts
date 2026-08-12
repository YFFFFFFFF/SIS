import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '@/shared/types/api'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('iids.auth.token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && body.code && body.code !== 'SUCCESS') {
      return Promise.reject(new Error(body.message || body.code))
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      window.dispatchEvent(new Event('iids:unauthorized'))
    }
    const message = error.response?.data?.message || error.message || 'Request failed'
    return Promise.reject(new Error(message))
  }
)

export async function apiGet<T>(url: string): Promise<T> {
  const response = await http.get<ApiResponse<T>>(url)
  return response.data.data
}

export async function apiPost<T, B = unknown>(url: string, body?: B): Promise<T> {
  const response = await http.post<ApiResponse<T>>(url, body)
  return response.data.data
}

export async function apiPut<T, B = unknown>(url: string, body?: B): Promise<T> {
  const response = await http.put<ApiResponse<T>>(url, body)
  return response.data.data
}

export async function apiDelete<T, B = unknown>(url: string, body?: B): Promise<T> {
  const response = await http.delete<ApiResponse<T>>(url, { data: body })
  return response.data.data
}

export async function apiDownload(url: string): Promise<Blob> {
  const response = await http.get<Blob>(url, { responseType: 'blob' })
  return response.data
}
