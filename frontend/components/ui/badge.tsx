import { cva, type VariantProps } from "class-variance-authority";
import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * A small status marker.
 *
 * Every variant pairs a colour with a word. Colour alone is not a signal: around
 * one man in twelve cannot reliably separate the red and green ones, and the
 * text carries the meaning for them.
 */
const badge = cva(
  "inline-flex items-center gap-1 rounded-[--radius-sm] px-2 py-0.5 text-[length:--text-caption] font-medium leading-5",
  {
    variants: {
      tone: {
        neutral: "bg-sunken text-ink-soft",
        success: "bg-success-soft text-success",
        warning: "bg-warning-soft text-warning",
        danger: "bg-danger-soft text-danger",
        info: "bg-info-soft text-info",
        accent: "bg-accent-soft text-accent",
        solid: "bg-ink text-white",
      },
    },
    defaultVariants: { tone: "neutral" },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badge> {}

export function Badge({ className, tone, ...props }: BadgeProps) {
  return <span className={cn(badge({ tone }), className)} {...props} />;
}
