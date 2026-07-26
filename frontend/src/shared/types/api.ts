export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  username: string
  displayName: string
  roles: string[]
}