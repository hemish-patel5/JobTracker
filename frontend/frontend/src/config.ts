// In a Vercel Services deployment, /api is routed to the backend service on
// the same origin. VITE_API_URL remains available when the frontend and
// backend are deployed separately (for example, Vercel + Cloud Run).
export const API_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
