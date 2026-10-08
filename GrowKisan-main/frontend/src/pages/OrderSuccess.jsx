import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'

function money(v) {
  return '₹' + Number(v || 0).toLocaleString('en-IN')
}

export default function OrderSuccess() {
  const navigate = useNavigate()
  const order = useLocation().state?.order

  // Direct visits (no order in router state) go back home.
  useEffect(() => {
    if (!order) navigate('/', { replace: true })
  }, [order, navigate])

  if (!order) return null

  return (
    <Layout>
      <h1 className="page-title">Order Confirmed</h1>

      <div className="success-card">
        <div className="success-check">✓</div>
        <h2 className="success-title">Order placed successfully!</h2>
        <p className="success-sub">Thank you for shopping with KisanFarm</p>

        <div className="success-order">
          <div className="success-order-no">Order #{order.orderNumber}</div>
          <div className="success-paid">Payment confirmed • {money(order.totalAmount)}</div>
          <div className="success-note">A confirmation will be sent to your mobile.</div>
        </div>

        {order.items?.length > 0 && (
          <div className="review-items" style={{ marginTop: 20, textAlign: 'left' }}>
            {order.items.map((it) => (
              <div className="review-item" key={it.productId}>
                <span>{it.quantity} × {it.productName}</span>
                <span>{money(it.lineTotal)}</span>
              </div>
            ))}
          </div>
        )}

        <div className="success-actions">
          <Link to="/products" className="btn btn-ghost">View Products</Link>
          <Link to="/" className="btn btn-success">Continue Shopping</Link>
        </div>
      </div>
    </Layout>
  )
}
