import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import { getCart, setQuantity, removeFromCart } from '../utils/cart'

const DELIVERY_CHARGE = 49
const FREE_DELIVERY_THRESHOLD = 499

function money(v) {
  return '₹' + Number(v || 0).toLocaleString('en-IN')
}

export default function Cart() {
  const navigate = useNavigate()
  const [items, setItems] = useState(getCart())

  useEffect(() => {
    const update = () => setItems(getCart())
    window.addEventListener('cart-updated', update)
    return () => window.removeEventListener('cart-updated', update)
  }, [])

  const subtotal = items.reduce((s, i) => s + i.price * i.quantity, 0)
  const delivery = items.length === 0 || subtotal >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_CHARGE
  const total = subtotal + delivery

  function dec(i) { setQuantity(i.productId, i.quantity - 1) }
  function inc(i) { setQuantity(i.productId, i.quantity + 1) }

  return (
    <Layout>
      <h1 className="page-title">Your Cart</h1>

      {items.length === 0 ? (
        <div className="cart-empty">
          <p>Your cart is empty.</p>
          <Link to="/" className="btn btn-success">Continue Shopping</Link>
        </div>
      ) : (
        <div className="cart-grid">
          <div className="cart-items">
            {items.map((i) => (
              <div className="cart-item" key={i.productId}>
                <div className="cart-item-img">
                  {i.imageUrl ? <img src={i.imageUrl} alt={i.name} /> : <span>PRODUCT</span>}
                </div>
                <div className="cart-item-info">
                  <div className="cart-item-name">{i.name}</div>
                  <div className="cart-item-stock">In Stock • Seller verified</div>
                  <button className="cart-remove" onClick={() => removeFromCart(i.productId)}>Remove</button>
                </div>
                <div className="cart-item-price">{money(i.price)}</div>
                <div className="qty-control">
                  <button onClick={() => dec(i)} aria-label="Decrease">−</button>
                  <span>{i.quantity}</span>
                  <button onClick={() => inc(i)} aria-label="Increase">+</button>
                </div>
              </div>
            ))}
          </div>

          <aside className="cart-summary">
            <h3>Price Summary</h3>
            <div className="summary-row"><span>Subtotal</span><span>{money(subtotal)}</span></div>
            <div className="summary-row"><span>Delivery</span><span>{delivery === 0 ? 'FREE' : money(delivery)}</span></div>
            <div className="summary-divider" />
            <div className="summary-row total"><span>Total</span><span>{money(total)}</span></div>
            <button className="btn btn-success btn-block" onClick={() => navigate('/checkout')}>
              PROCEED TO CHECKOUT
            </button>
            <p className="summary-note">Final amount is confirmed by the server at checkout.</p>
          </aside>
        </div>
      )}
    </Layout>
  )
}
