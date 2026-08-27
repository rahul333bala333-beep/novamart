import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * What a screen shows when it has nothing to show.
 *
 * An empty region with no explanation is indistinguishable from a broken one.
 * Every empty state here names what is missing and offers the action that would
 * fill it, so the screen is never a dead end.
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: {
  icon?: React.ComponentType<{ className?: string }>;
  title: string;
  description?: string;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-3 rounded-[--radius-lg] border border-dashed border-line-strong bg-surface px-6 py-14 text-center",
        className
      )}
    >
      {Icon && (
        <span className="flex size-11 items-center justify-center rounded-full bg-sunken text-muted">
          <Icon className="size-5" />
        </span>
      )}
      <h3 className="text-[length:--text-lead] font-semibold text-ink">{title}</h3>
      {description && <p className="max-w-sm text-[length:--text-body] text-muted">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}

/**
 * A failed request, with the one control that matters: try again.
 *
 * The message shown is the human-readable one from the API envelope. Backend
 * exception text and stack traces never reach this component, because they never
 * leave the server.
 */
export function ErrorState({
  title = "Something went wrong",
  description,
  onRetry,
  className,
}: {
  title?: string;
  description?: string;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <div
      role="alert"
      className={cn(
        "flex flex-col items-center justify-center gap-3 rounded-[--radius-lg] border border-danger/25 bg-danger-soft px-6 py-12 text-center",
        className
      )}
    >
      <h3 className="text-[length:--text-lead] font-semibold text-ink">{title}</h3>
      {description && <p className="max-w-md text-[length:--text-body] text-ink-soft">{description}</p>}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-1 cursor-pointer rounded-[--radius-md] border border-line-strong bg-surface px-4 py-2 text-[length:--text-small] font-medium text-ink transition-colors hover:bg-sunken"
        >
          Try again
        </button>
      )}
    </div>
  );
}
