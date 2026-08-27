"use client";

import { Check, TriangleAlert, X } from "lucide-react";
import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * Transient confirmations.
 *
 * Hand-written rather than pulled from a library, for two reasons: the design
 * system already defines the surface, radius and motion this needs, and a toast
 * is roughly a hundred lines. A dependency here would add weight and a second
 * visual language.
 *
 * Accessibility: the container is an `aria-live` region, so a message is
 * announced without stealing focus. A toast that grabbed focus would interrupt
 * whatever the user was typing.
 */

type ToastTone = "success" | "error" | "info";

interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

interface ToastContextValue {
  push: (message: string, tone?: ToastTone) => void;
  success: (message: string) => void;
  error: (message: string) => void;
}

const ToastContext = React.createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
  const context = React.useContext(ToastContext);
  if (!context) {
    return {
      push: () => {},
      success: () => {},
      error: () => {},
    };
  }
  return context;
}

const AUTO_DISMISS_MS = 4000;

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = React.useState<Toast[]>([]);
  const nextId = React.useRef(0);

  const dismiss = React.useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const push = React.useCallback(
    (message: string, tone: ToastTone = "info") => {
      const id = ++nextId.current;
      setToasts((current) => [...current, { id, tone, message }]);
      window.setTimeout(() => dismiss(id), AUTO_DISMISS_MS);
    },
    [dismiss]
  );

  const value = React.useMemo<ToastContextValue>(
    () => ({
      push,
      success: (message: string) => push(message, "success"),
      error: (message: string) => push(message, "error"),
    }),
    [push]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        // polite, not assertive: a confirmation should wait its turn rather than
        // cutting across whatever the screen reader is currently saying.
        aria-live="polite"
        aria-atomic="false"
        className="pointer-events-none fixed inset-x-0 bottom-0 z-[100] flex flex-col items-center gap-2 p-4 sm:bottom-auto sm:right-0 sm:top-0 sm:items-end"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={cn(
              "animate-in-up pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-[--radius-lg] border bg-surface px-4 py-3 shadow-[--shadow-menu]",
              toast.tone === "success" && "border-success/30",
              toast.tone === "error" && "border-danger/30",
              toast.tone === "info" && "border-line-strong"
            )}
          >
            {toast.tone === "success" && <Check className="mt-0.5 size-4 shrink-0 text-success" aria-hidden="true" />}
            {toast.tone === "error" && (
              <TriangleAlert className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden="true" />
            )}
            <p className="flex-1 text-[length:--text-body] text-ink">{toast.message}</p>
            <button
              type="button"
              onClick={() => dismiss(toast.id)}
              className="cursor-pointer rounded-[--radius-sm] p-0.5 text-muted transition-colors hover:text-ink"
              aria-label="Dismiss notification"
            >
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
