import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Logo from '../../components/Logo'
import { adminLogin } from '../../services/adminService'
import { saveAdmin } from '../../utils/adminAuth'

// Admin/seller login with username + password (separate from buyer OTP login).
export default function AdminLogin() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const result = await adminLogin(username.trim(), password)
      saveAdmin(result)
      navigate('/admin', { replace: true })
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed. Check your credentials.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="admin-login-page">
      <div className="admin-login-card">
        <div className="login-card-brand" style={{ justifyContent: 'center' }}>
          <span className="login-logo-circle"><Logo size={26} /></span>
          <span className="brand-name">KISANFARM</span>
        </div>
        <h2 className="admin-login-title">Admin Panel</h2>
        <p className="admin-login-sub">Sign in to manage products, stock &amp; offers</p>

        <form onSubmit={handleSubmit}>
          <label className="login-label" htmlFor="username">Username</label>
          <input
            id="username"
            className="admin-input"
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="admin"
            autoComplete="username"
          />

          <label className="login-label" htmlFor="password" style={{ marginTop: 14 }}>Password</label>
          <input
            id="password"
            className="admin-input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter password"
            autoComplete="current-password"
          />

          {error && <p className="form-error">{error}</p>}

          <button type="submit" className="btn btn-success btn-block" disabled={loading}>
            {loading ? 'SIGNING IN...' : 'SIGN IN'}
          </button>
        </form>

        <p className="admin-login-hint">Demo: username <b>admin</b> / password <b>admin123</b></p>
      </div>
    </div>
  )
}
