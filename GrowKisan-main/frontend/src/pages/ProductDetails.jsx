import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Layout from '../components/Layout'
import { getProduct } from '../services/productService'
import { addToCart } from '../utils/cart'

function money(v) {
  return '₹' + Number(v || 0).toLocaleString('en-IN')
}

export default function ProductDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState(null)
  const [qty, setQty] = useState(1)
  const [notFound, setNotFound] = useState(false)
  const [added, setAdded] = useState(false)

  useEffect(() => {
    getProduct(id).then(setProduct).catch(() => setNotFound(true))
  }, [id])

  if (notFound) {
    return (
      <Layout>
        <h1 className="page-title">Product Details</h1>
        <p className="shop-empty">This product is no longer available.</p>
      </Layout>
    )
  }
  if (!product) {
    return (
      <Layout>
        <h1 className="page-title">Product Details</h1>
        <p className="shop-empty">Loading…</p>
      </Layout>
    )
  }

  const hasOffer = product.offerActive && Number(product.offerPrice) > 0
  const disc = Number(product.discountPercent || 0)
  const sell = hasOffer
    ? Number(product.offerPrice)
    : disc > 0
      ? Math.round((product.price - (product.price * disc) / 100) * 100) / 100
      : Number(product.price || 0)
  const outOfStock = !(product.stockQuantity > 0)
  const maxQty = product.stockQuantity > 0 ? product.stockQuantity : 1

  function handleAdd() {
    addToCart(product, qty)
    setAdded(true)
    setTimeout(() => setAdded(false), 1800)
  }

  function handleBuyNow() {
    addToCart(product, qty)
    navigate('/cart')
  }

  return (
    <Layout>
      <h1 className="page-title">Product Details</h1>

      <div className="pd-grid">
        <div className="pd-image">
          {product.imageUrl
            ? <img src={product.imageUrl} alt={product.name}
                   onError={(e) => { e.currentTarget.style.display = 'none' }} />
            : <span>PRODUCT IMAGE</span>}
          {hasOffer && <span className="pd-offer-flag">{product.offerLabel}</span>}
        </div>

        <div className="pd-info">
          <h2 className="pd-name">{product.name}</h2>
          <div className="pd-meta">
            {product.brand && <span>{product.brand}</span>}
            <span className={outOfStock ? 'pd-oos' : 'pd-instock'}>
              {outOfStock ? 'Out of Stock' : `In Stock (${product.stockQuantity})`}
            </span>
          </div>

          <div className="pd-price-row">
            <span className="pd-price">{money(sell)}</span>
            {(hasOffer || disc > 0) && <span className="mrp">{money(product.price)}</span>}
            {hasOffer && <span className="badge badge-offer">{product.offerLabel}</span>}
            {!hasOffer && disc > 0 && <span className="badge badge-off">{disc}% OFF</span>}
          </div>

          {product.description && (
            <>
              <h4 className="pd-section">Description</h4>
              <p className="pd-desc">{product.description}</p>
            </>
          )}

          <h4 className="pd-section">Quantity</h4>
          <div className="pd-actions">
            <div className="qty-control">
              <button onClick={() => setQty((q) => Math.max(1, q - 1))} disabled={outOfStock}>−</button>
              <span>{qty}</span>
              <button onClick={() => setQty((q) => Math.min(maxQty, q + 1))} disabled={outOfStock}>+</button>
            </div>
            <button className="btn btn-primary" onClick={handleAdd} disabled={outOfStock}>
              {added ? 'ADDED ✓' : 'ADD TO CART'}
            </button>
            <button className="btn btn-success" onClick={handleBuyNow} disabled={outOfStock}>
              BUY NOW
            </button>
          </div>
        </div>
      </div>
    </Layout>
  )
}
