import { cn } from "@/lib/cn";

/**
 * Loading placeholder.
 *
 * Skeletons rather than a centred spinner, and shaped like the content they
 * stand in for. Reserving the final layout means nothing jumps when the data
 * lands, which is both a better experience and the difference between a passing
 * and failing Cumulative Layout Shift score.
 */
export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden="true" className={cn("skeleton rounded-[--radius-md]", className)} />;
}

export function ProductCardSkeleton() {
  return (
    <div className="flex flex-col gap-3">
      <Skeleton className="aspect-square w-full" />
      <Skeleton className="h-3 w-16" />
      <Skeleton className="h-4 w-4/5" />
      <Skeleton className="h-4 w-1/3" />
    </div>
  );
}

export function RowSkeleton({ columns = 4 }: { columns?: number }) {
  return (
    <div className="flex items-center gap-4 border-b border-line px-4 py-4">
      {Array.from({ length: columns }).map((_, index) => (
        <Skeleton key={index} className={cn("h-4", index === 0 ? "w-2/5" : "w-1/6")} />
      ))}
    </div>
  );
}
