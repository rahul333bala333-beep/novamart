import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * A single dashboard metric.
 *
 * There is no sparkline and no "+12% since last week" on these tiles, because
 * the API returns no historical comparison and inventing one would be inventing
 * data. A tile shows a real number, what it counts, and nothing else.
 */
export function StatTile({
  label,
  value,
  hint,
  icon: Icon,
  tone = "neutral",
  loading = false,
}: {
  label: string;
  value: React.ReactNode;
  hint?: string;
  icon?: React.ComponentType<{ className?: string }>;
  tone?: "neutral" | "warning" | "danger" | "success";
  loading?: boolean;
}) {
  return (
    <div className="rounded-[--radius-lg] border border-line bg-surface p-4">
      <div className="flex items-start justify-between gap-3">
        <p className="text-[length:--text-caption] font-medium uppercase tracking-[0.07em] text-muted">
          {label}
        </p>
        {Icon && (
          <span
            className={cn(
              "flex size-8 shrink-0 items-center justify-center rounded-[--radius-md]",
              tone === "neutral" && "bg-sunken text-ink-soft",
              tone === "warning" && "bg-warning-soft text-warning",
              tone === "danger" && "bg-danger-soft text-danger",
              tone === "success" && "bg-success-soft text-success"
            )}
          >
            <Icon className="size-4" aria-hidden="true" />
          </span>
        )}
      </div>

      {loading ? (
        <div className="skeleton mt-3 h-8 w-24 rounded-[--radius-sm]" />
      ) : (
        <p className="tabular mt-2 text-[length:--text-h2] font-semibold leading-tight text-ink">
          {value}
        </p>
      )}

      {hint && <p className="mt-1 text-[length:--text-caption] text-muted">{hint}</p>}
    </div>
  );
}
