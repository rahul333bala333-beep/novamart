import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/cn";

/**
 * A price, optionally with the amount it was reduced from.
 *
 * The struck-through original is marked up with `<s>` and given a spoken label,
 * because a screen reader announcing two numbers with no relationship between
 * them is confusing rather than persuasive.
 */
export function Price({
  amount,
  compareAt,
  currency = "INR",
  size = "md",
  className,
}: {
  amount: number;
  compareAt?: number | null;
  currency?: string;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const onOffer = typeof compareAt === "number" && compareAt > amount;

  return (
    <div className={cn("flex flex-wrap items-baseline gap-2", className)}>
      <span
        className={cn(
          "tabular font-semibold text-ink",
          size === "sm" && "text-[length:--text-body]",
          size === "md" && "text-[length:--text-lead]",
          size === "lg" && "text-[length:--text-h2]"
        )}
      >
        {formatCurrency(amount, currency)}
      </span>
      {onOffer && (
        <>
          <s className={cn("tabular text-muted", size === "lg" ? "text-[length:--text-base]" : "text-[length:--text-small]")}>
            {formatCurrency(compareAt!, currency)}
          </s>
          <span className="sr-only">reduced from {formatCurrency(compareAt!, currency)}</span>
        </>
      )}
    </div>
  );
}
