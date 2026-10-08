// KisanFarm leaf logo mark (inline SVG so it scales crisply everywhere).
export default function Logo({ size = 34 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d="M24 42C14 42 7 35 6 24c10 0 17 2 20 8 1-10 7-18 16-22-1 18-8 32-18 32z" fill="#2e8b57" />
      <path d="M24 42c0-8 2-16 8-22" stroke="#1b5e3a" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}
