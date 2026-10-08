import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import Logo from '../components/Logo'
import { sendOtp, verifyOtp } from '../services/authService'
import { saveAuth } from '../utils/auth'

const OTP_LENGTH = 6
const RESEND_SECONDS = 30

// Mask a mobile like 9876543210 -> 98XXXXXX10 for display.
function maskMobile(m) {
  if (!m || m.length !== 10) return m || ''
  return m.slice(0, 2) + 'XXXXXX' + m.slice(8)
}

export default function VerifyOtp() {
  const navigate = useNavigate()
  const location = useLocation()
  const mobile = location.state?.mobile

  const [digits, setDigits] = useState(Array(OTP_LENGTH).fill(''))
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [seconds, setSeconds] = useState(RESEND_SECONDS)
  const inputsRef = useRef([])

  // If someone lands here without a mobile (e.g. refresh), send them to login.
  useEffect(() => {
    if (!mobile) navigate('/login', { replace: true })
  }, [mobile, navigate])

  // Resend countdown.
  useEffect(() => {
    if (seconds <= 0) return
    const t = setTimeout(() => setSeconds((s) => s - 1), 1000)
    return () => clearTimeout(t)
  }, [seconds])

  function handleChange(index, value) {
    const d = value.replace(/\D/g, '')
    if (!d) {
      // Clearing the box
      const next = [...digits]
      next[index] = ''
      setDigits(next)
      return
    }
    const next = [...digits]
    next[index] = d[d.length - 1]
    setDigits(next)
    if (error) setError('')
    // Auto-advance
    if (index < OTP_LENGTH - 1) inputsRef.current[index + 1]?.focus()
  }

  function handleKeyDown(index, e) {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputsRef.current[index - 1]?.focus()
    }
  }

  function handlePaste(e) {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH)
    if (!pasted) return
    e.preventDefault()
    const next = Array(OTP_LENGTH).fill('')
    for (let i = 0; i < pasted.length; i++) next[i] = pasted[i]
    setDigits(next)
    inputsRef.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus()
  }

  async function handleVerify(e) {
    e.preventDefault()
    const code = digits.join('')
    if (code.length !== OTP_LENGTH) {
      setError('Please enter the 6-digit OTP.')
      return
    }
    setLoading(true)
    setError('')
    try {
      const result = await verifyOtp(mobile, code)
      saveAuth(result) // { token, user }
      navigate('/', { replace: true })
    } catch (err) {
      setError(err?.response?.data?.message || 'Invalid or expired OTP. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  async function handleResend() {
    if (seconds > 0) return
    try {
      await sendOtp(mobile)
      setSeconds(RESEND_SECONDS)
      setDigits(Array(OTP_LENGTH).fill(''))
      inputsRef.current[0]?.focus()
    } catch {
      setError('Could not resend OTP. Please try again.')
    }
  }

  const mm = String(Math.floor(seconds / 60)).padStart(2, '0')
  const ss = String(seconds % 60).padStart(2, '0')

  return (
    <Layout>
      <h1 className="page-title">Verify Mobile Number</h1>

      <section className="verify-card">
        <div className="login-card-brand verify-brand">
          <span className="login-logo-circle"><Logo size={26} /></span>
          <span className="brand-name">KISANFARM</span>
        </div>

        <h2 className="verify-title">Verify your mobile number</h2>
        <p className="verify-sub">Enter the 6-digit OTP sent to<br />+91 {maskMobile(mobile)}</p>

        <form onSubmit={handleVerify}>
          <div className="otp-boxes" onPaste={handlePaste}>
            {digits.map((d, i) => (
              <input
                key={i}
                ref={(el) => (inputsRef.current[i] = el)}
                className="otp-box"
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={d}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                autoFocus={i === 0}
              />
            ))}
          </div>

          {error && <p className="form-error center">{error}</p>}

          <button type="submit" className="btn btn-success btn-block" disabled={loading}>
            {loading ? 'VERIFYING...' : 'VERIFY & CONTINUE'}
          </button>
        </form>

        <p className="verify-footer">
          {seconds > 0
            ? <>Resend OTP in {mm}:{ss}</>
            : <button className="link-btn" onClick={handleResend}>Resend OTP</button>}
          {' '}&bull;{' '}
          <button className="link-btn" onClick={() => navigate('/login')}>Change number</button>
        </p>
      </section>
    </Layout>
  )
}
