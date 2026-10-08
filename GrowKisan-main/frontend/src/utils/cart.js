// Cart stored in localStorage. Each item keeps a product snapshot for display;
// the backend recomputes authoritative prices at checkout.
const KEY = 'kisanfarm_cart'

export function getCart() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || []
  } catch {
    return []
  }
}

function save(items) {
  localStorage.setItem(KEY, JSON.stringify(items))
  window.dispatchEvent(new Event('cart-updated'))
}

export function cartCount() {
  return getCart().reduce((sum, i) => sum + i.quantity, 0)
}

export function addToCart(product, qty = 1) {
  const items = getCart()
  const existing = items.find((i) => i.productId === product.id)
  const max = product.stockQuantity != null ? product.stockQuantity : 9999
  if (existing) {
    existing.quantity = Math.min(max, existing.quantity + qty)
  } else {
    items.push({
      productId: product.id,
      name: product.name,
      imageUrl: product.imageUrl,
      // display price only; backend recalculates at checkout
      price: effectivePrice(product),
      mrp: Number(product.price || 0),
      stockQuantity: product.stockQuantity,
      quantity: Math.min(max, qty),
    })
  }
  save(items)
}

export function setQuantity(productId, qty) {
  const items = getCart()
  const item = items.find((i) => i.productId === productId)
  if (!item) return
  const max = item.stockQuantity != null ? item.stockQuantity : 9999
  item.quantity = Math.max(1, Math.min(max, qty))
  save(items)
}

export function removeFromCart(productId) {
  save(getCart().filter((i) => i.productId !== productId))
}

export function clearCart() {
  save([])
}

export function effectivePrice(product) {
  const price = Number(product.price || 0)
  if (product.offerActive && Number(product.offerPrice) > 0) return Number(product.offerPrice)
  const disc = Number(product.discountPercent || 0)
  if (disc > 0) return Math.round((price - (price * disc) / 100) * 100) / 100
  return price
}
