import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  // Build the React app straight into Spring Boot's static folder so the single
  // Spring Boot app serves the UI on one port (8080). No separate frontend server.
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },

  // Used only during `npm run dev` (optional). API calls to /api are proxied to
  // the Spring Boot backend on port 8080.
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
