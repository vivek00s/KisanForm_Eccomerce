import api from './api'

// Product API calls. Falls back gracefully; pages handle empty data.
export async function getProducts() {
  const res = await api.get('/products')
  return res.data
}

export async function getProduct(id) {
  const res = await api.get(`/products/${id}`)
  return res.data
}
