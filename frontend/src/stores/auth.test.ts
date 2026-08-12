import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { apiPost } from '@/shared/api/http'
import { useAuthStore } from './auth'

vi.mock('@/shared/api/http', () => ({ apiPost: vi.fn() }))

describe('auth store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('persists and clears a successful login', async () => {
    vi.mocked(apiPost).mockResolvedValue({ token: 'token-1', username: 'analyst', displayName: '分析师', roles: ['ANALYST'] })
    const store = useAuthStore()
    await store.login({ username: 'analyst', password: 'secret' })
    expect(store.isAuthenticated).toBe(true)
    expect(localStorage.getItem('iids.auth.token')).toBe('token-1')
    store.logout()
    expect(store.isAuthenticated).toBe(false)
  })

  it('clears the session when the API reports unauthorized', () => {
    localStorage.setItem('iids.auth.token', 'expired')
    const store = useAuthStore()
    window.dispatchEvent(new Event('iids:unauthorized'))
    expect(store.isAuthenticated).toBe(false)
  })
})
