import { useEffect, useMemo, useState } from 'react'
import Layout from '../components/Layout'
import ProductCard from '../components/ProductCard'
import { getProducts } from '../services/productService'

export default function Products() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState('newest')
  const [inStockOnly, setInStockOnly] = useState(false)
  const [offersOnly, setOffersOnly] = useState(false)

  useEffect(() => {
    getProducts()
      .then((data) => setProducts(Array.isArray(data) ? data : []))
      .catch(() => setProducts([]))
      .finally(() => setLoading(false))
  }, [])

  const sellPrice = (p) => {
    const price = Number(p.price || 0)
    if (p.offerActive && Number(p.offerPrice) > 0) return Number(p.offerPrice)
    const d = Number(p.discountPercent || 0)
    return d > 0 ? price - (price * d) / 100 : price
  }

  const visible = useMemo(() => {
    let list = [...products]
    const q = query.trim().toLowerCase()
    if (q) {
      list = list.filter((p) =>
        [p.name, p.brand, p.sku].filter(Boolean).some((v) => String(v).toLowerCase().includes(q)))
    }
    if (inStockOnly) list = list.filter((p) => p.stockQuantity > 0)
    if (offersOnly) list = list.filter((p) => p.offerActive)
    if (sort === 'price-low') list.sort((a, b) => sellPrice(a) - sellPrice(b))
    else if (sort === 'price-high') list.sort((a, b) => sellPrice(b) - sellPrice(a))
    else if (sort === 'name') list.sort((a, b) => String(a.name).localeCompare(String(b.name)))
    return list
  }, [products, query, sort, inStockOnly, offersOnly])

  return (
    <Layout>
      <h1 className="page-title">Shop Products</h1>

      <div className="shop-toolbar">
        <input
          className="shop-search"
          placeholder="Search products, brands..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <select className="shop-select" value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="newest">Sort: Newest</option>
          <option value="price-low">Price: Low to High</option>
          <option value="price-high">Price: High to Low</option>
          <option value="name">Name: A–Z</option>
        </select>
        <label className="shop-check">
          <input type="checkbox" checked={inStockOnly} onChange={(e) => setInStockOnly(e.target.checked)} />
          In stock
        </label>
        <label className="shop-check">
          <input type="checkbox" checked={offersOnly} onChange={(e) => setOffersOnly(e.target.checked)} />
          Offers only
        </label>
      </div>

      {loading ? (
        <p className="shop-empty">Loading products…</p>
      ) : visible.length === 0 ? (
        <p className="shop-empty">
          No products found. {products.length === 0 && 'Add products from the admin panel.'}
        </p>
      ) : (
        <>
          <p className="shop-count">{visible.length} product{visible.length !== 1 ? 's' : ''}</p>
          <div className="product-grid">
            {visible.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        </>
      )}
    </Layout>
  )
}
