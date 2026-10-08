import axios from 'axios'
import { getToken } from '../utils/auth'

// Base axios instance. Relative URL works whether served by Spring Boot
// (same origin) or the Vite dev server (which proxies /api).
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Attach the auth token (if logged in) to every request.
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default api
