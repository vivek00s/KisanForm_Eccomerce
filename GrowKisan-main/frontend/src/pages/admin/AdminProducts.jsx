import { useEffect, useState } from 'react'
import AdminLayout from '../../components/AdminLayout'
import {
  listProducts, createProduct, updateProduct, deleteProduct, setStock, setOffer,
} from '../../services/adminService'

const EMPTY = {
  sku: '', name: '', description: '', brand: '', imageUrl: '',
  price: '', discountPercent: 0, stockQuantity: 0, badge: '',
}

function money(v) {
  return '₹' + Number(v || 0).toLocaleString('en-IN')
}

export default function AdminProducts() {
  const [products, setProducts] = useState([])
  const [form, setForm] = useState(EMPTY)
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function refresh() {
    listProducts().then(setProducts).catch(() => setProducts([]))
  }
  useEffect(refresh, [])

  function change(e) {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  function startEdit(p) {
    setEditingId(p.id)
    setForm({
      sku: p.sku || '', name: p.name || '', description: p.description || '',
      brand: p.brand || '', imageUrl: p.imageUrl || '', price: p.price ?? '',
      discountPercent: p.discountPercent ?? 0, stockQuantity: p.stockQuantity ?? 0,
      badge: p.badge || '',
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function cancelEdit() {
    setEditingId(null)
    setForm(EMPTY)
    setError('')
  }

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (!form.name || !form.sku || form.price === '') {
      setError('SKU, name and price are required.')
      return
    }
    setBusy(true)
    try {
      const payload = {
        ...form,
        price: Number(form.price),
        discountPercent: Number(form.discountPercent) || 0,
        stockQuantity: Number(form.stockQuantity) || 0,
      }
      if (editingId) await updateProduct(editingId, { ...payload, id: editingId })
      else await createProduct(payload)
      cancelEdit()
      refresh()
    } catch (err) {
      setError(err?.response?.data?.message || 'Save failed.')
    } finally {
      setBusy(false)
    }
  }

  async function remove(id) {
    if (!window.confirm('Delete this product?')) return
    try { await deleteProduct(id); refresh() }
    catch (err) { setError(err?.response?.data?.message || 'Delete failed.') }
  }

  async function changeStock(p) {
    const val = window.prompt(`Set stock quantity for "${p.name}"`, p.stockQuantity ?? 0)
    if (val === null) return
    try { await setStock(p.id, Math.max(0, parseInt(val, 10) || 0)); refresh() }
    catch (err) { setError(err?.response?.data?.message || 'Stock update failed.') }
  }

  async function toggleOffer(p) {
    if (p.offerActive) {
      // Turn offer off.
      try { await setOffer(p.id, { offerActive: false }); refresh() }
      catch (err) { setError(err?.response?.data?.message || 'Offer update failed.') }
      return
    }
    const label = window.prompt('Festival offer label (e.g. DIWALI OFFER):', 'FESTIVAL OFFER')
    if (label === null) return
    const price = window.prompt(`Offer price (current MRP ${p.price}):`, p.price)
    if (price === null) return
    try {
      await setOffer(p.id, { offerActive: true, offerLabel: label, offerPrice: Number(price) })
      refresh()
    } catch (err) {
      setError(err?.response?.data?.message || 'Offer update failed.')
    }
  }

  return (
    <AdminLayout>
      <h1 className="admin-h1">Products</h1>

      {/* Add / Edit form */}
      <form className="admin-form" onSubmit={submit}>
        <h3>{editingId ? 'Edit product' : 'Add new product'}</h3>
        <div className="admin-form-grid">
          <label>SKU<input name="sku" value={form.sku} onChange={change} placeholder="SEED-TOM-01" /></label>
          <label>Name<input name="name" value={form.name} onChange={change} placeholder="Hybrid Tomato Seeds" /></label>
          <label>Brand<input name="brand" value={form.brand} onChange={change} placeholder="AgriBegri" /></label>
          <label>Image URL<input name="imageUrl" value={form.imageUrl} onChange={change} placeholder="https://..." /></label>
          <label>Price (MRP)<input name="price" type="number" value={form.price} onChange={change} placeholder="299" /></label>
          <label>Discount %<input name="discountPercent" type="number" value={form.discountPercent} onChange={change} placeholder="10" /></label>
          <label>Stock Qty<input name="stockQuantity" type="number" value={form.stockQuantity} onChange={change} placeholder="100" /></label>
          <label>Badge<input name="badge" value={form.badge} onChange={change} placeholder="BEST / NEW / SAVE" /></label>
          <label className="admin-form-full">Description<textarea name="description" value={form.description} onChange={change} rows={2} /></label>
        </div>
        {error && <p className="form-error">{error}</p>}
        <div className="admin-form-actions">
          <button type="submit" className="btn btn-success" disabled={busy}>{editingId ? 'Update' : 'Add Product'}</button>
          {editingId && <button type="button" className="btn btn-ghost" onClick={cancelEdit}>Cancel</button>}
        </div>
      </form>

      {/* Products table */}
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Product</th><th>Price</th><th>Stock</th><th>Offer</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {products.length === 0 && (
              <tr><td colSpan={5} className="admin-empty">No products yet. Add your first product above.</td></tr>
            )}
            {products.map((p) => (
              <tr key={p.id}>
                <td>
                  <div className="admin-prod-cell">
                    {p.imageUrl ? <img src={p.imageUrl} alt={p.name} /> : <span className="admin-prod-noimg">—</span>}
                    <div>
                      <div className="admin-prod-name">{p.name}</div>
                      <div className="admin-prod-sku">{p.sku}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <div>{money(p.price)}</div>
                  {p.offerActive && p.offerPrice
                    ? <div className="admin-offer-price">Offer: {money(p.offerPrice)}</div>
                    : p.discountPercent > 0 ? <div className="admin-disc">{p.discountPercent}% off</div> : null}
                </td>
                <td>
                  {p.stockQuantity > 0
                    ? <span className={'stock-pill' + (p.stockQuantity <= 10 ? ' low' : '')}>{p.stockQuantity}</span>
                    : <span className="stock-pill out">Out</span>}
                </td>
                <td>
                  {p.offerActive
                    ? <span className="offer-tag">{p.offerLabel}</span>
                    : <span className="offer-none">—</span>}
                </td>
                <td className="admin-row-actions">
                  <button onClick={() => startEdit(p)}>Edit</button>
                  <button onClick={() => changeStock(p)}>Stock</button>
                  <button onClick={() => toggleOffer(p)}>{p.offerActive ? 'Clear Offer' : 'Set Offer'}</button>
                  <button className="danger" onClick={() => remove(p.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  )
}
