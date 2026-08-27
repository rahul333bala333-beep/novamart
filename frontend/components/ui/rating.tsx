import { Star } from "lucide-react";
import { cn } from "@/lib/cn";

/**
 * Star rating.
 *
 * The numeric value is rendered as text alongside the stars rather than being
 * left implicit in the graphic, so it is available to a screen reader and to
 * anyone who cannot separate the filled stars from the empty ones.
 */
export function Rating({
  value,
  count,
  size = "sm",
  className,
}: {
  value: number;
  count?: number;
  size?: "sm" | "md";
  className?: string;
}) {
  const rounded = Math.round(value * 2) / 2;
  const starSize = size === "sm" ? "size-3.5" : "size-4";

  return (
    <div className={cn("flex items-center gap-1.5", className)}>
      <div className="flex items-center gap-0.5" aria-hidden="true">
        {[1, 2, 3, 4, 5].map((position) => (
          <Star
            key={position}
            className={cn(
              starSize,
              position <= rounded
                ? "fill-accent text-accent"
                : position - 0.5 === rounded
                  ? "fill-accent/50 text-accent"
                  : "fill-none text-line-strong"
            )}
            strokeWidth={1.5}
          />
        ))}
      </div>
      <span className={cn("tabular text-muted", size === "sm" ? "text-[length:--text-caption]" : "text-[length:--text-small]")}>
        {value.toFixed(1)}
        {count !== undefined && ` (${count.toLocaleString("en-IN")})`}
      </span>
      <span className="sr-only">
        Rated {value.toFixed(1)} out of 5{count !== undefined ? ` from ${count} reviews` : ""}
      </span>
    </div>
  );
}

export function StarInput({
  value,
  onChange,
  className,
}: {
  value: number;
  onChange: (val: number) => void;
  className?: string;
}) {
  return (
    <div className={cn("flex items-center gap-1", className)}>
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          onClick={() => onChange(star)}
          className="p-1 rounded transition-colors hover:scale-110 focus:outline-none focus:ring-2 focus:ring-accent"
          aria-label={`Rate ${star} star${star > 1 ? "s" : ""}`}
        >
          <Star
            className={cn(
              "size-6 transition-colors",
              star <= value
                ? "fill-accent text-accent"
                : "fill-none text-line-strong hover:text-accent/60"
            )}
            strokeWidth={1.5}
          />
        </button>
      ))}
      <span className="ml-2 text-sm font-medium text-muted">
        {value === 5 ? "Excellent" : value === 4 ? "Good" : value === 3 ? "Average" : value === 2 ? "Below Average" : value === 1 ? "Poor" : "Select rating"}
      </span>
    </div>
  );
}
