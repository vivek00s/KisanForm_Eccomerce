// Green hero banner from the PDF: "Beat the Summer Heat with Shade Nets",
// three feature chips, and a BUY NOW button.
export default function Hero() {
  return (
    <section className="hero">
      <div className="hero-content">
        <p className="hero-kicker">Beat the Summer Heat</p>
        <h1 className="hero-title">Shade Nets</h1>
        <p className="hero-sub">
          Top-quality shade nets for durable build, strong UV protection and healthy soil &amp; crops.
        </p>
        <div className="hero-features">
          <span>&#128279; Durable Build</span>
          <span>&#9728; UV Protection</span>
          <span>&#127793; Soil &amp; Crop Care</span>
        </div>
        <button className="btn btn-primary hero-btn">BUY NOW</button>
      </div>
    </section>
  )
}
