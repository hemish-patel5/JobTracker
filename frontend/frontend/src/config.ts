const DEFAULT_API_URL = import.meta.env.DEV
  ? 'http://localhost:8080'
  : ''

// Production uses same-origin /api requests, which Vercel proxies to Render.
// This keeps preview and production domains working without CORS changes.
export const API_URL = import.meta.env.DEV
  ? (import.meta.env.VITE_API_URL?.trim() || DEFAULT_API_URL).replace(/\/$/, '')
  : DEFAULT_API_URL
