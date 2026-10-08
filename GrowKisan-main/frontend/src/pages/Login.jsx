import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import Logo from '../components/Logo'
import { sendOtp } from '../services/authService'

// Login page from the PDF: left promo panel + right "Welcome back" mobile card.
export default function Login() {
  const navigate = useNavigate()
  const [mobile, setMobile] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function onChangeMobile(e) {
    // Keep digits only, max 10.
    const digits = e.target.value.replace(/\D/g, '').slice(0, 10)
    setMobile(digits)
    if (error) setError('')
  }

  async function handleContinue(e) {
    e.preventDefault()
    if (mobile.length !== 10) {
      setError('Please enter a valid 10-digit mobile number.')
      return
    }
    setLoading(true)
    setError('')
    try {
      await sendOtp(mobile)
      // Pass the mobile to the verify page via router state.
      navigate('/verify', { state: { mobile } })
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not send OTP. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Layout>
      <h1 className="page-title">Login</h1>

      <div className="login-grid">
        {/* Left promo panel */}
        <section className="login-promo">
          <h2 className="promo-title">Everything you need to grow.</h2>
          <p className="promo-sub">Shop trusted agricultural products, from seeds to crop protection.</p>

          <div className="promo-chips">
            <span className="chip chip-green">Fast Delivery</span>
            <span className="chip chip-blue">Secure Pay</span>
            <span className="chip chip-orange">Easy Returns</span>
          </div>

          <h3 className="promo-featured">Featured today</h3>
          <div className="promo-cards">
            <div className="promo-card">SEEDS</div>
            <div className="promo-card">FERTILIZER</div>
            <div className="promo-card">TOOLS</div>
          </div>
        </section>

        {/* Right login card */}
        <section className="login-card">
          <div className="login-card-brand">
            <span className="login-logo-circle"><Logo size={26} /></span>
            <span className="brand-name">KISANFARM</span>
          </div>

          <h2 className="login-welcome">Welcome back</h2>
          <p className="login-welcome-sub">Login with your mobile number</p>

          <form onSubmit={handleContinue}>
            <label className="login-label" htmlFor="mobile">Mobile Number</label>
            <div className="mobile-input">
              <span className="mobile-prefix">+91</span>
              <input
                id="mobile"
                type="tel"
                inputMode="numeric"
                placeholder="Enter 10-digit mobile number"
                value={mobile}
                onChange={onChangeMobile}
                autoComplete="tel"
              />
            </div>

            {error && <p className="form-error">{error}</p>}

            <button type="submit" className="btn btn-success btn-block" disabled={loading}>
              {loading ? 'SENDING OTP...' : 'CONTINUE'}
            </button>
          </form>

          <p className="login-note">We will send a one-time verification code.<br />No password required.</p>
          <a href="#terms" className="login-terms">Terms &amp; Privacy Policy</a>
        </section>
      </div>
    </Layout>
  )
}
