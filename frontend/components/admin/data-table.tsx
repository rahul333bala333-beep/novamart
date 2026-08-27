"use client";

import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * Table for the back office.
 *
 * Renders as a real `<table>` from the medium breakpoint up and as a list of
 * cards below it. This is the point of the component: a table squeezed onto a
 * phone either scrolls horizontally, which hides the columns that matter, or
 * shrinks its text below the legible minimum. Restructuring keeps every field
 * readable and every row tappable.
 *
 * Columns declare a `priority` so the card view can show the important fields
 * first rather than dumping all nine in source order.
 */
export interface Column<T> {
  key: string;
  header: string;
  /** Rendered in both layouts. */
  cell: (row: T) => React.ReactNode;
  /** Hidden in the card layout when false. Defaults to true. */
  showOnMobile?: boolean;
  align?: "left" | "right";
  className?: string;
}

export function DataTable<T>({
  columns,
  rows,
  keyOf,
  onRowClick,
  emptyState,
  loading = false,
  loadingRows = 6,
  caption,
}: {
  columns: Column<T>[];
  rows: T[];
  keyOf: (row: T) => string;
  onRowClick?: (row: T) => void;
  emptyState?: React.ReactNode;
  loading?: boolean;
  loadingRows?: number;
  caption: string;
}) {
  if (loading) {
    return (
      <div className="overflow-hidden rounded-[--radius-lg] border border-line bg-surface">
        {Array.from({ length: loadingRows }).map((_, index) => (
          <div key={index} className="flex items-center gap-4 border-b border-line px-4 py-4 last:border-0">
            {columns.slice(0, 4).map((column, columnIndex) => (
              <div
                key={column.key}
                className={cn("skeleton h-4 rounded-[--radius-sm]", columnIndex === 0 ? "w-2/5" : "w-1/6")}
              />
            ))}
          </div>
        ))}
      </div>
    );
  }

  if (rows.length === 0 && emptyState) {
    return <>{emptyState}</>;
  }

  return (
    <>
      {/* -------------------------------------------------- desktop: table */}
      <div className="hidden overflow-hidden rounded-[--radius-lg] border border-line bg-surface md:block">
        <table className="w-full border-collapse text-left">
          <caption className="sr-only">{caption}</caption>
          <thead>
            <tr className="border-b border-line bg-sunken">
              {columns.map((column) => (
                <th
                  key={column.key}
                  scope="col"
                  className={cn(
                    "px-4 py-3 text-[length:--text-caption] font-semibold uppercase tracking-[0.06em] text-muted",
                    column.align === "right" && "text-right",
                    column.className
                  )}
                >
                  {column.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr
                key={keyOf(row)}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={cn(
                  "border-b border-line last:border-0",
                  onRowClick && "cursor-pointer transition-colors hover:bg-sunken"
                )}
              >
                {columns.map((column) => (
                  <td
                    key={column.key}
                    className={cn(
                      "px-4 py-3.5 align-middle text-[length:--text-body] text-ink",
                      column.align === "right" && "text-right",
                      column.className
                    )}
                  >
                    {column.cell(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ---------------------------------------------------- mobile: cards */}
      <ul className="flex flex-col gap-3 md:hidden">
        {rows.map((row) => {
          const visible = columns.filter((column) => column.showOnMobile !== false);
          const [primary, ...rest] = visible;
          return (
            <li
              key={keyOf(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={cn(
                "rounded-[--radius-lg] border border-line bg-surface p-4",
                onRowClick && "cursor-pointer transition-colors active:bg-sunken"
              )}
            >
              <div className="text-[length:--text-body] font-medium text-ink">{primary.cell(row)}</div>
              <dl className="mt-3 flex flex-col gap-1.5">
                {rest.map((column) => (
                  <div key={column.key} className="flex items-center justify-between gap-3">
                    <dt className="text-[length:--text-caption] uppercase tracking-[0.06em] text-muted">
                      {column.header}
                    </dt>
                    <dd className="text-[length:--text-small] text-ink">{column.cell(row)}</dd>
                  </div>
                ))}
              </dl>
            </li>
          );
        })}
      </ul>
    </>
  );
}
