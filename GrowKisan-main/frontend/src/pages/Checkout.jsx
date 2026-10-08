import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import { getCart, clearCart } from '../utils/cart'
import { getUser } from '../utils/auth'
import { checkout, confirmPayment } from '../services/orderService'

const DELIVERY_CHARGE = 49
const FREE_DELIVERY_THRESHOLD = 499

function money(v) {
  return '₹' + Number(v || 0).toLocaleString('en-IN')
}

const EMPTY_ADDRESS = {
  name: '', mobile: '', line1: '', line2: '', city: '', state: '', pincode: '',
}

export default function Checkout() {
  const navigate = useNavigate()
  const [items, setItems] = useState(getCart())
  const [address, setAddress] = useState(EMPTY_ADDRESS)
  const [step, setStep] = useState(1) // 1=address, 2=review+pay
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  // Prefill mobile from the logged-in buyer, if any.
  useEffect(() => {
    const user = getUser()
    if (user?.mobile) setAddress((a) => ({ ...a, mobile: user.mobile }))
  }, [])

  useEffect(() => {
    if (getCart().length === 0) navigate('/cart', { replace: true })
  }, [navigate])

  const subtotal = items.reduce((s, i) => s + i.price * i.quantity, 0)
  const delivery = subtotal >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_CHARGE
  const total = subtotal + delivery

  function change(e) {
    const { name, value } = e.target
    setAddress((a) => ({ ...a, [name]: value }))
  }

  function continueToReview(e) {
    e.preventDefault()
    setError('')
    if (!address.name || !address.mobile || !address.line1 || !address.city || !address.state || !address.pincode) {
      setError('Please fill all required address fields.')
      return
    }
    if (address.pincode.replace(/\D/g, '').length !== 6) {
      setError('Please enter a valid 6-digit pincode.')
      return
    }
    setStep(2)
  }

  async function pay() {
    setBusy(true)
    setError('')
    try {
      const cartLines = items.map((i) => ({ productId: i.productId, quantity: i.quantity }))
      // 1) Backend creates the order + payment intent (recomputes the amount).
      const res = await checkout(cartLines, address)

      // 2) Complete payment.
      if (res.provider === 'stripe' && window.Stripe) {
        // Real Stripe flow would confirm the card payment here using res.clientSecret.
        // (Requires Stripe publishable key + Elements; wired when Stripe is enabled.)
        setError('Stripe is enabled on the server but the card form is not configured in this build.')
        setBusy(false)
        return
      }

      // Mock provider: confirm immediately.
      const order = await confirmPayment(res.orderNumber, res.providerPaymentId)
      clearCart()
      navigate('/order-success', { state: { order }, replace: true })
    } catch (err) {
      setError(err?.response?.data?.message || 'Checkout failed. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Layout>
      <h1 className="page-title">Checkout</h1>

      <div className="checkout-steps">
        <span className={step >= 1 ? 'active' : ''}>1 Address</span>
        <span className={step >= 2 ? 'active' : ''}>2 Review &amp; Pay</span>
      </div>

      <div className="checkout-grid">
        <section className="checkout-main">
          {step === 1 ? (
            <form className="address-form" onSubmit={continueToReview}>
              <h3>Delivery Details</h3>
              <div className="address-grid">
                <label>Full Name*<input name="name" value={address.name} onChange={change} /></label>
                <label>Mobile Number*<input name="mobile" value={address.mobile} onChange={change} maxLength={10} /></label>
                <label className="full">Address Line 1*<input name="line1" value={address.line1} onChange={change} placeholder="House no, street" /></label>
                <label className="full">Address Line 2<input name="line2" value={address.line2} onChange={change} placeholder="Landmark, area (optional)" /></label>
                <label>City*<input name="city" value={address.city} onChange={change} /></label>
                <label>State*<input name="state" value={address.state} onChange={change} /></label>
                <label>Pincode*<input name="pincode" value={address.pincode} onChange={change} maxLength={6} /></label>
              </div>
              {error && <p className="form-error">{error}</p>}
              <button type="submit" className="btn btn-success">Continue to Review</button>
            </form>
          ) : (
            <div className="review-box">
              <h3>Review Your Order</h3>
              <div className="review-address">
                <strong>{address.name}</strong> • {address.mobile}<br />
                {address.line1}{address.line2 ? ', ' + address.line2 : ''}<br />
                {address.city}, {address.state} {address.pincode}
                <button className="link-btn" onClick={() => setStep(1)} style={{ marginLeft: 10 }}>Edit</button>
              </div>
              <div className="review-items">
                {items.map((i) => (
                  <div className="review-item" key={i.productId}>
                    <span>{i.quantity} × {i.name}</span>
                    <span>{money(i.price * i.quantity)}</span>
                  </div>
                ))}
              </div>
              {error && <p className="form-error">{error}</p>}
              <button className="btn btn-success btn-block" onClick={pay} disabled={busy}>
                {busy ? 'PROCESSING...' : `PAY ${money(total)}`}
              </button>
              <p className="summary-note">🔒 Secure payment. The server confirms the final amount.</p>
            </div>
          )}
        </section>

        <aside className="cart-summary">
          <h3>Order Summary</h3>
          <div className="summary-row"><span>Subtotal</span><span>{money(subtotal)}</span></div>
          <div className="summary-row"><span>Delivery</span><span>{delivery === 0 ? 'FREE' : money(delivery)}</span></div>
          <div className="summary-divider" />
          <div className="summary-row total"><span>Payable</span><span>{money(total)}</span></div>
        </aside>
      </div>
    </Layout>
  )
}
