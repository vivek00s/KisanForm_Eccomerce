import { Navigate } from 'react-router-dom'
import { isAdminLoggedIn } from '../utils/adminAuth'

// Protects admin pages. Redirects to /admin/login if not authenticated.
export default function AdminRoute({ children }) {
  return isAdminLoggedIn() ? children : <Navigate to="/admin/login" replace />
}
