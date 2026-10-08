import { useEffect, useState } from 'react'
import Layout from '../components/Layout'
import Hero from '../components/Hero'
import CategoryCard from '../components/CategoryCard'
import ProductCard from '../components/ProductCard'
import { getProducts } from '../services/productService'

const CATEGORIES = [
  { icon: '🌱', name: 'Seeds' },
  { icon: '🧪', name: 'Fertilizers' },
  { icon: '🛡️', name: 'Crop Protection' },
  { icon: '💧', name: 'Irrigation' },
  { icon: '⚙️', name: 'Farm Tools' },
]

// Sample products from the PDF, used when the backend has no products yet.
const SAMPLE_PRODUCTS = [
  { id: null, name: 'Hybrid Vegetable Seeds', price: 299, discountPercent: 10, badge: '10% OFF',
    imageUrl: 'https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=600&q=80' },
  { id: null, name: 'Organic Fertilizer', price: 599, discountPercent: 0, badge: 'BEST',
    imageUrl: 'https://images.unsplash.com/photo-1628352081506-83c43123ed6d?auto=format&fit=crop&w=600&q=80' },
  { id: null, name: 'Crop Shield', price: 799, discountPercent: 0, badge: 'SAVE',
    imageUrl: 'https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=600&q=80' },
  { id: null, name: 'Shade Net 90%', price: 1499, discountPercent: 0, badge: 'NEW',
    imageUrl: 'https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&w=600&q=80' },
]

export default function Home() {
  const [products, setProducts] = useState(SAMPLE_PRODUCTS)

  useEffect(() => {
    getProducts()
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) setProducts(data)
      })
      .catch(() => {
        // Backend not reachable or empty table: keep the sample products.
      })
  }, [])

  return (
    <Layout>
      <Hero />

      <section className="section">
        <h2 className="section-title">Featured categories</h2>
        <div className="category-grid">
          {CATEGORIES.map((c) => (
            <CategoryCard key={c.name} icon={c.icon} name={c.name} />
          ))}
        </div>
      </section>

      <section className="section">
        <h2 className="section-title">Popular products</h2>
        <div className="product-grid">
          {products.slice(0, 8).map((p, i) => (
            <ProductCard key={p.id ?? i} product={p} />
          ))}
        </div>
      </section>
    </Layout>
  )
}
