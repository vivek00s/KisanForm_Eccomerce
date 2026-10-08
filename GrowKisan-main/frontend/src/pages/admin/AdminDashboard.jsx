import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AdminLayout from '../../components/AdminLayout'
import { listProducts } from '../../services/adminService'

export default function AdminDashboard() {
  const navigate = useNavigate()
  const [products, setProducts] = useState([])

  useEffect(() => {
    listProducts().then(setProducts).catch(() => setProducts([]))
  }, [])

  const total = products.length
  const outOfStock = products.filter((p) => !(p.stockQuantity > 0)).length
  const onOffer = products.filter((p) => p.offerActive).length
  const lowStock = products.filter((p) => p.stockQuantity > 0 && p.stockQuantity <= 10).length

  return (
    <AdminLayout>
      <h1 className="admin-h1">Dashboard Overview</h1>

      <div className="admin-cards">
        <div className="admin-card"><span className="admin-card-label">Total Products</span><span className="admin-card-value green">{total}</span></div>
        <div className="admin-card"><span className="admin-card-label">On Offer</span><span className="admin-card-value blue">{onOffer}</span></div>
        <div className="admin-card"><span className="admin-card-label">Low Stock (≤10)</span><span className="admin-card-value orange">{lowStock}</span></div>
        <div className="admin-card"><span className="admin-card-label">Out of Stock</span><span className="admin-card-value red">{outOfStock}</span></div>
      </div>

      <div className="admin-actions">
        <button className="btn btn-success" onClick={() => navigate('/admin/products')}>Manage Products →</button>
      </div>
    </AdminLayout>
  )
}
