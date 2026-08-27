/**
 * The single HTTP entry point for the whole frontend.
 *
 * Nothing else in the application calls `fetch`. Centralising it means the base
 * URL, the bearer token, error normalisation and token refresh each exist once
 * rather than being reimplemented, slightly differently, in every component that
 * needs data.
 *
 * The layering is: component -> hook -> resource module -> this client -> gateway.
 */

import { getStoredTokens, storeTokens, clearTokens } from "@/lib/auth/token-store";
import type { ApiEnvelope, FieldError } from "@/lib/types";

/**
 * All traffic goes to the gateway, never to a service directly. The frontend
 * does not know that seven services exist, which is exactly the decoupling the
 * gateway is there to provide.
 */
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

/**
 * A failed request, carrying the machine-readable code the backend contract
 * promises. Components branch on `code`, never on the message text, so wording
 * can change without breaking behaviour.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }

  /** True when the shopper can fix this by editing the form. */
  get isValidation() {
    return this.code === "VALIDATION_FAILED";
  }

  get isAuth() {
    return this.status === 401;
  }

  /** True when retrying might work: the fault was not in the request. */
  get isRetryable() {
    return this.status === 0 || this.status === 503 || this.status >= 500;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  /** Public endpoints skip the token so a signed-out visitor can browse. */
  auth?: boolean;
  headers?: Record<string, string>;
  signal?: AbortSignal;
  /** Guards a non-idempotent write against double submission. */
  idempotencyKey?: string;
}

function buildUrl(path: string, query?: Record<string, unknown>) {
  const url = new URL(
    API_BASE_URL.replace(/\/$/, "") + (path.startsWith("/") ? path : `/${path}`)
  );
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null || value === "") continue;
      url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

/**
 * Refresh is shared across concurrent 401s.
 *
 * Without this, a page that fires four requests at once on a stale token starts
 * four refreshes, three of which present an already-consumed refresh token. The
 * backend treats a replayed refresh token as theft and revokes every session,
 * signing the user out for doing nothing wrong.
 */
let refreshInFlight: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  const tokens = getStoredTokens();
  if (!tokens?.refreshToken) return false;

  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await fetch(buildUrl("/auth/refresh"), {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: tokens.refreshToken }),
        });
        if (!response.ok) {
          clearTokens();
          return false;
        }
        const payload = (await response.json()) as ApiEnvelope<{
          accessToken: string;
          refreshToken: string;
          user: unknown;
        }>;
        storeTokens({
          accessToken: payload.data.accessToken,
          refreshToken: payload.data.refreshToken,
        });
        return true;
      } catch {
        clearTokens();
        return false;
      } finally {
        // Cleared on the next tick so every caller awaiting this cycle sees the
        // same result before a new cycle can begin.
        setTimeout(() => {
          refreshInFlight = null;
        }, 0);
      }
    })();
  }
  return refreshInFlight;
}

async function toApiError(response: Response, path: string): Promise<ApiError> {
  let code = "UNKNOWN_ERROR";
  let message = "Something went wrong. Please try again.";
  let fieldErrors: FieldError[] = [];

  try {
    const body = await response.json();
    if (body && typeof body === "object") {
      code = body.errorCode ?? code;
      message = body.message ?? message;
      fieldErrors = Array.isArray(body.fieldErrors) ? body.fieldErrors : [];
    }
  } catch {
    // A non-JSON body means something upstream of the services answered: a
    // proxy, or the gateway itself failing before it could format a response.
    // The generic message above is the honest thing to show.
    if (response.status === 404) {
      code = "NOT_FOUND";
      message = "We could not find what you were looking for.";
    }
  }

  if (process.env.NODE_ENV !== "production") {
    console.warn(`[api] ${response.status} ${path} -> ${code}`);
  }
  return new ApiError(response.status, code, message, fieldErrors);
}

async function execute<T>(
  path: string,
  options: RequestOptions,
  query: Record<string, unknown> | undefined,
  isRetry: boolean
): Promise<T> {
  const { method = "GET", body, auth = true, headers = {}, signal, idempotencyKey } = options;

  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
  const requestHeaders: Record<string, string> = { Accept: "application/json", ...headers };
  if (body !== undefined && !isFormData) requestHeaders["Content-Type"] = "application/json";
  if (idempotencyKey) requestHeaders["Idempotency-Key"] = idempotencyKey;

  if (auth) {
    const token = getStoredTokens()?.accessToken;
    if (token) requestHeaders.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers: requestHeaders,
      body: body === undefined ? undefined : isFormData ? (body as BodyInit) : JSON.stringify(body),
      signal,
      cache: "no-store",
    });
  } catch (cause) {
    // fetch only rejects for network-level failure, so this is genuinely
    // "unreachable", not "returned an error". Status 0 distinguishes the two.
    if ((cause as Error)?.name === "AbortError") throw cause;
    throw new ApiError(
      0,
      "NETWORK_ERROR",
      "We could not reach Nova Mart. Check your connection and try again."
    );
  }

  if (response.status === 204) return undefined as T;

  if (!response.ok) {
    const error = await toApiError(response, path);

    // One transparent retry after refreshing. TOKEN_EXPIRED means the session is
    // recoverable; any other 401 means the credential is wrong, and retrying
    // would just fail again.
    if (error.code === "TOKEN_EXPIRED" && auth && !isRetry) {
      const refreshed = await refreshAccessToken();
      if (refreshed) return execute<T>(path, options, query, true);
      clearTokens();
    }
    throw error;
  }

  const envelope = (await response.json()) as ApiEnvelope<T>;
  return envelope.data;
}

export const api = {
  get: <T>(path: string, query?: Record<string, unknown>, options: RequestOptions = {}) =>
    execute<T>(path, { ...options, method: "GET" }, query, false),

  post: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
    execute<T>(path, { ...options, method: "POST", body }, undefined, false),

  put: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
    execute<T>(path, { ...options, method: "PUT", body }, undefined, false),

  delete: <T>(path: string, options: RequestOptions = {}) =>
    execute<T>(path, { ...options, method: "DELETE" }, undefined, false),

  upload: <T>(path: string, formData: FormData, options: RequestOptions = {}) =>
    execute<T>(path, { ...options, method: "POST", body: formData }, undefined, false),
};
