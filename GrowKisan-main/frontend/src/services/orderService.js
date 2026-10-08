import api from './api'

// Create an order + payment intent. items = [{productId, quantity}], address = {...}
export async function checkout(items, address) {
  const res = await api.post('/checkout', { items, address })
  return res.data // { orderNumber, amount, provider, providerPaymentId, clientSecret, order }
}

// Confirm payment after the gateway step.
export async function confirmPayment(orderNumber, providerPaymentId) {
  const res = await api.post('/checkout/confirm', { orderNumber, providerPaymentId })
  return res.data // order
}

export async function getOrder(orderNumber) {
  const res = await api.get(`/orders/${orderNumber}`)
  return res.data
}

export async function myOrders(mobile) {
  const res = await api.get('/orders', { params: { mobile } })
  return res.data
}
