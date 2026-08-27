"use client";

import { cva, type VariantProps } from "class-variance-authority";
import { Loader2 } from "lucide-react";
import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * The one button in the system.
 *
 * Variants are semantic, not decorative: `primary` marks the single most
 * important action on a screen, `danger` marks something destructive. A screen
 * with two primary buttons has an unresolved hierarchy problem that no amount of
 * styling fixes.
 */
const button = cva(
  [
    "inline-flex items-center justify-center gap-2 whitespace-nowrap font-medium",
    "cursor-pointer select-none",
    "transition-colors duration-[--duration-fast] ease-[--ease-out-quart]",
    // Disabled must look disabled AND stop responding. Opacity alone leaves a
    // control that still shows a pointer cursor and still invites a click.
    "disabled:pointer-events-none disabled:opacity-45",
  ],
  {
    variants: {
      variant: {
        primary: "bg-ink text-white hover:bg-ink/90 active:bg-ink",
        secondary:
          "bg-surface text-ink border border-line-strong hover:bg-sunken active:bg-sunken",
        ghost: "bg-transparent text-ink-soft hover:bg-sunken hover:text-ink",
        danger: "bg-danger text-white hover:bg-danger/90",
        link: "bg-transparent text-ink underline underline-offset-4 hover:text-ink-soft p-0 h-auto",
      },
      size: {
        // 44px minimum on the touch sizes. Anything smaller fails the platform
        // guidance and is genuinely hard to hit on a phone.
        sm: "h-9 px-3 text-[length:--text-small] rounded-[--radius-md]",
        md: "h-11 px-4 text-[length:--text-body] rounded-[--radius-md]",
        lg: "h-12 px-6 text-[length:--text-base] rounded-[--radius-md]",
        icon: "h-11 w-11 rounded-[--radius-md]",
      },
      block: { true: "w-full", false: "" },
    },
    defaultVariants: { variant: "primary", size: "md", block: false },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof button> {
  /** Shows a spinner and blocks further clicks while an action is in flight. */
  loading?: boolean;
  /** Announced to screen readers while `loading` is true. */
  loadingLabel?: string;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { className, variant, size, block, loading = false, loadingLabel = "Working", children, disabled, ...props },
  ref
) {
  return (
    <button
      ref={ref}
      className={cn(button({ variant, size, block }), className)}
      // Disabling during an async action is what actually prevents the double
      // submission; the spinner only explains why nothing is happening.
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
      {loading ? <span className="sr-only">{loadingLabel}</span> : null}
      {children}
    </button>
  );
});
