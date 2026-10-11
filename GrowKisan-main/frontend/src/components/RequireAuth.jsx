import { Navigate, useLocation } from 'react-router-dom'
import { getToken } from '../utils/auth'

// Protects buyer pages that need a logged-in user (e.g. checkout). If there is
// no auth token, redirects to /login and remembers where the user was headed so
// they can be sent back after a successful OTP login. After logout the token is
// cleared, so the user must log in again before reaching the protected page.
export default function RequireAuth({ children }) {
  const location = useLocation()
  if (!getToken()) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}
