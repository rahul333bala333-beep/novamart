"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import * as React from "react";
import { authApi } from "@/lib/api/resources";
import { ApiError } from "@/lib/api/client";
import { clearTokens, getStoredTokens, storeTokens } from "./token-store";
import type { UserProfile } from "@/lib/types";

interface AuthContextValue {
  user: UserProfile | null;
  /** True until the stored session has been checked, so guards do not flash. */
  initialising: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  register: (input: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phone?: string;
  }) => Promise<void>;
  signOut: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = React.createContext<AuthContextValue | null>(null);

export function useAuth() {
  const context = React.useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside <AuthProvider>");
  return context;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = React.useState<UserProfile | null>(null);
  const [initialising, setInitialising] = React.useState(true);
  const router = useRouter();
  const queryClient = useQueryClient();

  /**
   * On first load, a token in the cookie is only a claim. The profile is fetched
   * to confirm the session is still valid server-side, so a revoked or expired
   * session does not leave the interface showing a signed-in header that then
   * fails on the first real request.
   */
  React.useEffect(() => {
    let cancelled = false;

    async function restore() {
      if (!getStoredTokens()) {
        setInitialising(false);
        return;
      }
      try {
        const profile = await authApi.me();
        if (!cancelled) setUser(profile);
      } catch (error) {
        if (
          error instanceof ApiError &&
          (error.isAuth || error.status === 404 || error.status === 401 || error.code === "USER_NOT_FOUND")
        ) {
          clearTokens();
          setUser(null);
        }
      } finally {
        if (!cancelled) setInitialising(false);
      }
    }

    void restore();
    return () => {
      cancelled = true;
    };
  }, []);

  const signIn = React.useCallback(
    async (email: string, password: string) => {
      const tokens = await authApi.login(email, password);
      storeTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
      setUser(tokens.user);
      // The previous visitor's cached cart and orders must not survive a change
      // of user. Clearing the cache is the only reliable way to guarantee it.
      await queryClient.invalidateQueries();
    },
    [queryClient]
  );

  const register = React.useCallback(
    async (input: {
      firstName: string;
      lastName: string;
      email: string;
      password: string;
      phone?: string;
    }) => {
      const tokens = await authApi.register(input);
      storeTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
      setUser(tokens.user);
      await queryClient.invalidateQueries();
    },
    [queryClient]
  );

  const signOut = React.useCallback(async () => {
    const tokens = getStoredTokens();
    try {
      // Tell the server to revoke the refresh token. A signed JWT cannot be
      // withdrawn, so without this the session stays usable until it expires.
      if (tokens?.refreshToken) await authApi.logout(tokens.refreshToken);
    } catch {
      // Signing out must succeed locally even if the request fails; the user
      // asked to leave and the client-side session is going regardless.
    } finally {
      clearTokens();
      setUser(null);
      queryClient.clear();
      router.push("/");
      router.refresh();
    }
  }, [queryClient, router]);

  const refresh = React.useCallback(async () => {
    try {
      setUser(await authApi.me());
    } catch {
      // Left to the interceptor in the API client, which handles expiry.
    }
  }, []);

  const value = React.useMemo<AuthContextValue>(
    () => ({
      user,
      initialising,
      isAuthenticated: Boolean(user),
      isAdmin: Boolean(user?.roles.includes("ADMIN")),
      signIn,
      register,
      signOut,
      refresh,
    }),
    [user, initialising, signIn, register, signOut, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
