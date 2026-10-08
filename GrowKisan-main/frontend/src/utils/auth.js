// Simple auth storage in localStorage. Stores the token + user returned by the
// backend after OTP verification.
const KEY = 'kisanfarm_auth'

export function saveAuth(auth) {
  localStorage.setItem(KEY, JSON.stringify(auth))
  // Let other components (e.g. header) react to login/logout.
  window.dispatchEvent(new Event('auth-changed'))
}

export function getAuth() {
  try {
    return JSON.parse(localStorage.getItem(KEY))
  } catch {
    return null
  }
}

export function getUser() {
  return getAuth()?.user || null
}

export function getToken() {
  return getAuth()?.token || null
}

export function clearAuth() {
  localStorage.removeItem(KEY)
  window.dispatchEvent(new Event('auth-changed'))
}
