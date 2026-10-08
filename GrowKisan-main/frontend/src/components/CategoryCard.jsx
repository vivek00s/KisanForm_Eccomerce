import { Link } from 'react-router-dom'

// A featured category tile (icon, name, Explore link) as shown in the PDF.
export default function CategoryCard({ icon, name }) {
  return (
    <Link className="category-card" to="/products">
      <span className="cat-icon" aria-hidden="true">{icon}</span>
      <span className="cat-name">{name}</span>
      <span className="cat-explore">Explore &rarr;</span>
    </Link>
  )
}
