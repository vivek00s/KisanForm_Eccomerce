import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Logo from './Logo'
import { getUser, clearAuth } from '../utils/auth'
import { cartCount as readCartCount } from '../utils/cart'

// Matches the PDF: a dark-blue top strip, then a white header with left nav,
// a center search box, and the KISANFARM brand lockup on the right.
export default function Header({ search = '', onSearch }) {
  const navigate = useNavigate()
  const [user, setUser] = useState(getUser())
  const [cartCount, setCartCount] = useState(readCartCount())

  // Keep the header in sync with login/logout and cart changes anywhere.
  useEffect(() => {
    const update = () => { setUser(getUser()); setCartCount(readCartCount()) }
    window.addEventListener('auth-changed', update)
    window.addEventListener('cart-updated', update)
    window.addEventListener('storage', update)
    return () => {
      window.removeEventListener('auth-changed', update)
      window.removeEventListener('cart-updated', update)
      window.removeEventListener('storage', update)
    }
  }, [])

  function handleLogout() {
    clearAuth()
    navigate('/')
  }

  return (
    <>
      <div className="topbar">
        <div className="container topbar-inner">
          <span className="topbar-brand">KisanFarm &bull; Smart Agriculture Marketplace</span>
          <nav className="topbar-links">
            <a href="#help">Help</a>
            <span className="sep">|</span>
            <a href="#faq">FAQ</a>
            <Link to="/cart" className="topbar-cart">Cart <strong>({cartCount})</strong></Link>
            {user
              ? (<>
                  <span className="topbar-user">Hi, {user.name || user.mobile}</span>
                  <button className="link-btn topbar-logout" onClick={handleLogout}>Logout</button>
                </>)
              : <Link to="/login">Login</Link>}
          </nav>
        </div>
      </div>

      <header className="header">
        <div className="container header-inner">
          <nav className="main-nav">
            <Link to="/products" className="nav-item nav-categories">&#9776; Categories</Link>
            <a href="#offers" className="nav-item">Offers</a>
            <a href="#new" className="nav-item">New Arrivals</a>
          </nav>

          <form className="search" onSubmit={(e) => e.preventDefault()}>
            <input
              type="text"
              placeholder="Search products, crops, brands..."
              aria-label="Search"
              value={search}
              onChange={(e) => onSearch && onSearch(e.target.value)}
            />
            <button type="submit" aria-label="Search">&#128269;</button>
          </form>

          <Link to="/" className="brand">
            <span className="brand-logo"><Logo /></span>
            <span className="brand-text">
              <span className="brand-name">KISANFARM</span>
              <span className="brand-tagline">Grow &bull; Protect &bull; Harvest</span>
            </span>
          </Link>
        </div>
      </header>
    </>
  )
}
