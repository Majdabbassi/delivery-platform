/**
 * Where the API lives. Defaults to the local backend; a deployment overrides it in public/config.js
 * (window.__SWIFT_CONFIG__.apiUrl) without rebuilding the app.
 */
declare global {
  interface Window {
    __SWIFT_CONFIG__?: { apiUrl?: string };
  }
}

export const API_BASE_URL: string =
  (typeof window !== 'undefined' && window.__SWIFT_CONFIG__?.apiUrl) || 'http://localhost:8080/api';
