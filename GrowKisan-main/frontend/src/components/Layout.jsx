import { useState } from 'react'
import Header from './Header'
import Footer from './Footer'

// Common page shell: renders the shared Header and Footer around every page.
// Any page wrapped by Layout automatically gets the common header/footer.
export default function Layout({ children }) {
  const [search, setSearch] = useState('')

  return (
    <>
      <Header search={search} onSearch={setSearch} />
      <main className="container">{children}</main>
      <Footer />
    </>
  )
}
