/**
 * Where the session lives on the client.
 *
 * TRADE-OFF, stated plainly. Tokens are kept in a JavaScript-readable cookie so
 * that Next.js middleware can gate `/admin` and `/account` on the server before
 * a protected page is ever streamed. The cost is that a successful XSS could
 * read them; an httpOnly cookie could not be read by script, but also could not
 * be attached by this client to a cross-origin gateway request, and middleware
 * cannot see `localStorage` at all.
 *
 * The production answer is a Backend-For-Frontend: Next.js route handlers hold
 * an httpOnly session cookie and proxy to the gateway, so no token ever reaches
 * the browser. That is a meaningful amount of additional machinery, and it is
 * documented in `docs/architecture.md` as the intended next step rather than
 * quietly skipped.
 *
 * Mitigations actually in place: React escapes interpolated content by default,
 * `dangerouslySetInnerHTML` is used nowhere in this codebase, access tokens live
 * one hour, and refresh tokens are single-use so a stolen one is detected on
 * replay and revokes the whole family.
 */

export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
}

const ACCESS_COOKIE = "nm_at";
const REFRESH_COOKIE = "nm_rt";

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(^|;\\s*)${name}=([^;]*)`));
  return match ? decodeURIComponent(match[2]) : null;
}

function writeCookie(name: string, value: string, maxAgeSeconds: number) {
  if (typeof document === "undefined") return;
  const secure = window.location.protocol === "https:" ? "; Secure" : "";
  // SameSite=Lax blocks the cookie on cross-site POSTs, which removes the CSRF
  // vector that storing a credential in a cookie would otherwise reintroduce.
  document.cookie =
    `${name}=${encodeURIComponent(value)}; Path=/; Max-Age=${maxAgeSeconds}; SameSite=Lax${secure}`;
}

function deleteCookie(name: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; Path=/; Max-Age=0; SameSite=Lax`;
}

export function getStoredTokens(): StoredTokens | null {
  const accessToken = readCookie(ACCESS_COOKIE);
  const refreshToken = readCookie(REFRESH_COOKIE);
  if (!accessToken || !refreshToken) return null;
  return { accessToken, refreshToken };
}

export function storeTokens(tokens: StoredTokens) {
  writeCookie(ACCESS_COOKIE, tokens.accessToken, 60 * 60);          // 1 hour
  writeCookie(REFRESH_COOKIE, tokens.refreshToken, 60 * 60 * 24 * 14); // 14 days
}

export function clearTokens() {
  deleteCookie(ACCESS_COOKIE);
  deleteCookie(REFRESH_COOKIE);
}

export const TOKEN_COOKIE_NAMES = {
  access: ACCESS_COOKIE,
  refresh: REFRESH_COOKIE,
} as const;
