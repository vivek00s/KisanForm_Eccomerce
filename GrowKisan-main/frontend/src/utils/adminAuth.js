// Admin auth storage (separate from buyer auth).
const KEY = 'kisanfarm_admin'

export function saveAdmin(auth) {
  localStorage.setItem(KEY, JSON.stringify(auth))
  window.dispatchEvent(new Event('admin-auth-changed'))
}

export function getAdmin() {
  try {
    return JSON.parse(localStorage.getItem(KEY))
  } catch {
    return null
  }
}

export function getAdminToken() {
  return getAdmin()?.token || null
}

export function isAdminLoggedIn() {
  return !!getAdminToken()
}

export function clearAdmin() {
  localStorage.removeItem(KEY)
  window.dispatchEvent(new Event('admin-auth-changed'))
}
