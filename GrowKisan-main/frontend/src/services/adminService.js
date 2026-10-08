import api from './api'
import { getAdminToken } from '../utils/adminAuth'

// Admin login with username + password.
export async function adminLogin(username, password) {
  const res = await api.post('/admin/login', { username, password })
  return res.data // { token, admin }
}

function adminHeaders() {
  return { headers: { 'X-Admin-Token': getAdminToken() || '' } }
}

// Product management (admin-only; backend checks X-Admin-Token).
export async function listProducts() {
  const res = await api.get('/products')
  return res.data
}

export async function createProduct(product) {
  const res = await api.post('/products', product, adminHeaders())
  return res.data
}

export async function updateProduct(id, product) {
  const res = await api.put(`/products/${id}`, product, adminHeaders())
  return res.data
}

export async function deleteProduct(id) {
  const res = await api.delete(`/products/${id}`, adminHeaders())
  return res.data
}

export async function setStock(id, stockQuantity) {
  const res = await api.patch(`/products/${id}/stock`, { stockQuantity }, adminHeaders())
  return res.data
}

export async function setOffer(id, offer) {
  // offer = { offerActive, offerLabel, offerPrice }
  const res = await api.patch(`/products/${id}/offer`, offer, adminHeaders())
  return res.data
}
