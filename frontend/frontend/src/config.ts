const DEFAULT_API_URL = 'https://jobtracker-5j2t.onrender.com'

// VITE_API_URL allows the backend host to be changed without editing code.
// The deployed Render URL is the fallback so a missing Vercel variable does
// not incorrectly send API requests to the static frontend origin.
export const API_URL = (
  import.meta.env.VITE_API_URL?.trim() || DEFAULT_API_URL
).replace(/\/$/, '')
