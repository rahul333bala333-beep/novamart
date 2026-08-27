"use client";

import { Minus, Plus } from "lucide-react";
import { cn } from "@/lib/cn";

/**
 * Quantity control.
 *
 * A stepper rather than a free-text number input: it removes the whole class of
 * invalid states (empty, negative, "3a", 9999) instead of validating them after
 * the fact, and the buttons are far easier to hit on a phone than a spinner.
 */
export function QuantityStepper({
  value,
  onChange,
  min = 1,
  max = 20,
  disabled = false,
  label = "Quantity",
  className,
}: {
  value: number;
  onChange: (next: number) => void;
  min?: number;
  max?: number;
  disabled?: boolean;
  label?: string;
  className?: string;
}) {
  const buttonClass =
    "flex size-10 cursor-pointer items-center justify-center text-ink transition-colors hover:bg-sunken disabled:cursor-not-allowed disabled:text-line-strong disabled:hover:bg-transparent";

  return (
    <div
      className={cn(
        "inline-flex items-center rounded-[--radius-md] border border-line-strong bg-surface",
        disabled && "opacity-60",
        className
      )}
    >
      <button
        type="button"
        onClick={() => onChange(Math.max(min, value - 1))}
        disabled={disabled || value <= min}
        className={buttonClass}
        aria-label={`Decrease ${label.toLowerCase()}`}
      >
        <Minus className="size-4" />
      </button>

      {/* aria-live so a screen reader hears the new value after each press,
          rather than the user having to navigate back to read it. */}
      <span
        className="tabular w-10 text-center text-[length:--text-body] font-medium text-ink"
        aria-live="polite"
        aria-atomic="true"
      >
        {value}
      </span>

      <button
        type="button"
        onClick={() => onChange(Math.min(max, value + 1))}
        disabled={disabled || value >= max}
        className={buttonClass}
        aria-label={`Increase ${label.toLowerCase()}`}
      >
        <Plus className="size-4" />
      </button>
    </div>
  );
}
