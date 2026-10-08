import { NavLink, useNavigate } from 'react-router-dom'
import Logo from './Logo'
import { getAdmin, clearAdmin } from '../utils/adminAuth'

// Admin shell: dark-blue top bar (KISANFARM ADMIN) + left sidebar + content.
export default function AdminLayout({ children }) {
  const navigate = useNavigate()
  const admin = getAdmin()?.admin

  function handleLogout() {
    clearAdmin()
    navigate('/admin/login', { replace: true })
  }

  return (
    <div className="admin-shell">
      <header className="admin-topbar">
        <div className="admin-topbar-brand">
          <span className="login-logo-circle" style={{ width: 32, height: 32 }}><Logo size={20} /></span>
          KISANFARM ADMIN
        </div>
        <div className="admin-topbar-right">
          <span>{admin?.name || admin?.username || 'Admin'}</span>
          <button className="btn-admin-logout" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <div className="admin-body">
        <aside className="admin-sidebar">
          <NavLink to="/admin" end className={({ isActive }) => 'admin-nav' + (isActive ? ' active' : '')}>Dashboard</NavLink>
          <NavLink to="/admin/products" className={({ isActive }) => 'admin-nav' + (isActive ? ' active' : '')}>Products</NavLink>
          <a className="admin-nav" href="/" target="_blank" rel="noreferrer">View Store ↗</a>
        </aside>

        <main className="admin-content">{children}</main>
      </div>
    </div>
  )
}
