import { Link } from 'react-router-dom'
import { addToCart } from '../utils/cart'

function formatPrice(value) {
  return '₹' + Number(value).toLocaleString('en-IN')
}

// The price the buyer pays: offer price (if active) > discount price > MRP.
function effectivePrice(product) {
  const price = Number(product.price || 0)
  if (product.offerActive && Number(product.offerPrice) > 0) return Number(product.offerPrice)
  const disc = Number(product.discountPercent || 0)
  if (disc > 0) return Math.round((price - (price * disc) / 100) * 100) / 100
  return price
}

export default function ProductCard({ product }) {
  const sell = effectivePrice(product)
  const hasOffer = product.offerActive && Number(product.offerPrice) > 0
  const hasDiscount = !hasOffer && Number(product.discountPercent || 0) > 0
  const showMrp = hasOffer || hasDiscount
  const outOfStock = product.stockQuantity != null && product.stockQuantity <= 0
  const badgeText = hasOffer ? product.offerLabel : product.badge

  function handleAdd(e) {
    e.preventDefault()
    e.stopPropagation()
    if (outOfStock || !product.id) return
    addToCart(product, 1)
  }

  return (
    <div className="product-card">
      <Link to={product.id ? `/products/${product.id}` : '/products'} className="product-card-link">
        <div className="product-image">
          {product.imageUrl
            ? <img src={product.imageUrl} alt={product.name}
                   onError={(e) => { e.currentTarget.style.display = 'none' }} />
            : <span>PRODUCT</span>}
          {outOfStock && <span className="oos-overlay">Out of Stock</span>}
        </div>
        <h3 className="product-name">{product.name}</h3>
        <div className="product-price-row">
          <span className="price">{formatPrice(sell)}</span>
          {showMrp && <span className="mrp">{formatPrice(product.price)}</span>}
          {badgeText && (
            <span className={'badge ' + (hasOffer ? 'badge-offer' : badgeClass(badgeText))}>{badgeText}</span>
          )}
        </div>
      </Link>
      <button
        className={'add-cart-btn' + (outOfStock ? ' disabled' : '')}
        onClick={handleAdd}
        disabled={outOfStock || !product.id}
      >
        {outOfStock ? 'Out of Stock' : 'Add to Cart'}
      </button>
    </div>
  )
}

function badgeClass(badge) {
  const b = String(badge).toUpperCase()
  if (b.includes('BEST')) return 'badge-best'
  if (b.includes('NEW')) return 'badge-new'
  if (b.includes('SAVE')) return 'badge-save'
  return 'badge-off'
}
