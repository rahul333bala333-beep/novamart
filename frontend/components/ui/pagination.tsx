"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/cn";
import type { PageMeta } from "@/lib/types";

/**
 * Page navigation.
 *
 * Renders a windowed range rather than every page, because a catalogue of two
 * hundred pages would otherwise produce two hundred tab stops. First and last
 * are always reachable so the ends of the collection are one click away.
 */
export function Pagination({
  meta,
  onPageChange,
  className,
}: {
  meta: PageMeta;
  onPageChange: (page: number) => void;
  className?: string;
}) {
  if (meta.totalPages <= 1) return null;

  const current = meta.page;
  const last = meta.totalPages - 1;

  const pages: (number | "gap")[] = [];
  const window = 1;
  for (let index = 0; index <= last; index++) {
    const inWindow = Math.abs(index - current) <= window;
    if (index === 0 || index === last || inWindow) {
      pages.push(index);
    } else if (pages[pages.length - 1] !== "gap") {
      pages.push("gap");
    }
  }

  const buttonBase =
    "inline-flex h-10 min-w-10 cursor-pointer items-center justify-center rounded-[--radius-md] px-3 text-[length:--text-small] font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-40";

  return (
    <nav className={cn("flex items-center justify-center gap-1", className)} aria-label="Pagination">
      <button
        type="button"
        onClick={() => onPageChange(current - 1)}
        disabled={meta.first}
        className={cn(buttonBase, "text-ink hover:bg-sunken")}
        aria-label="Previous page"
      >
        <ChevronLeft className="size-4" />
      </button>

      {pages.map((page, index) =>
        page === "gap" ? (
          <span key={`gap-${index}`} className="px-1 text-muted" aria-hidden="true">
            &hellip;
          </span>
        ) : (
          <button
            key={page}
            type="button"
            onClick={() => onPageChange(page)}
            className={cn(
              buttonBase,
              "tabular",
              page === current ? "bg-ink text-white" : "text-ink hover:bg-sunken"
            )}
            aria-current={page === current ? "page" : undefined}
            aria-label={`Page ${page + 1}`}
          >
            {page + 1}
          </button>
        )
      )}

      <button
        type="button"
        onClick={() => onPageChange(current + 1)}
        disabled={meta.last}
        className={cn(buttonBase, "text-ink hover:bg-sunken")}
        aria-label="Next page"
      >
        <ChevronRight className="size-4" />
      </button>
    </nav>
  );
}
