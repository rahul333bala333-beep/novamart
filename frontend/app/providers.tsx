"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { AuthProvider } from "@/lib/auth/auth-context";
import { ApiError } from "@/lib/api/client";
import { ToastProvider } from "@/components/ui/toast";

/**
 * Client-side providers.
 *
 * TanStack Query is here to own request state. Hand-rolling loading, error,
 * caching and invalidation with `useState` and `useEffect` in every component is
 * how the same three bugs get written twenty times: a stale response overwriting
 * a fresh one, a state update after unmount, and a refetch that never happens
 * after a mutation.
 */
export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = React.useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Catalogue data changes rarely. A minute of freshness removes most
            // refetches while browsing without ever showing a stale price at
            // checkout, where the figure is re-read from the server anyway.
            staleTime: 60_000,
            gcTime: 5 * 60_000,
            refetchOnWindowFocus: false,
            retry: (failureCount, error) => {
              // Retrying a 404 or a 403 cannot succeed and only delays showing
              // the user what happened. Only genuinely transient failures are
              // worth a second attempt.
              if (error instanceof ApiError && !error.isRetryable) return false;
              return failureCount < 2;
            },
          },
          mutations: {
            // A mutation is a side effect. Retrying one automatically risks
            // placing a second order, so retries are always explicit.
            retry: false,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>{children}</AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}
