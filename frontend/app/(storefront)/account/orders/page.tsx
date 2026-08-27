"use client";

import Link from "next/link";
import { Package } from "lucide-react";
import * as React from "react";
import { ProductImage } from "@/components/commerce/product-image";
import { OrderStatusBadge } from "@/components/commerce/order-status-badge";
import { Button } from "@/components/ui/button";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { Skeleton } from "@/components/ui/skeleton";
import { useOrders } from "@/lib/hooks/use-orders";
import { formatCurrency, formatDate } from "@/lib/format";
import type { OrderStatus } from "@/lib/types";

const FILTERS: { value: OrderStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "PROCESSING", label: "Processing" },
  { value: "SHIPPED", label: "Shipped" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "CANCELLED", label: "Cancelled" },
];

export default function OrdersPage() {
  const [page, setPage] = React.useState(0);
  const [status, setStatus] = React.useState<OrderStatus | "ALL">("ALL");

  const { data, isLoading, isError, refetch } = useOrders({
    page,
    size: 8,
    status: status === "ALL" ? undefined : status,
  });

  return (
    <div>
      <h2 className="text-[length:--text-h3] font-semibold text-ink">My orders</h2>
      <p className="mt-1 text-[length:--text-body] text-muted">
        Track a delivery or review something you bought.
      </p>

      <div className="mt-5 -mx-1 flex gap-1 overflow-x-auto pb-2">
        {FILTERS.map((filter) => (
          <button
            key={filter.value}
            type="button"
            onClick={() => {
              setStatus(filter.value);
              setPage(0);
            }}
            aria-pressed={status === filter.value}
            className={
              status === filter.value
                ? "shrink-0 cursor-pointer rounded-[--radius-md] bg-ink px-3 py-1.5 text-[length:--text-small] font-medium text-white"
                : "shrink-0 cursor-pointer rounded-[--radius-md] border border-line-strong bg-surface px-3 py-1.5 text-[length:--text-small] text-ink-soft transition-colors hover:bg-sunken"
            }
          >
            {filter.label}
          </button>
        ))}
      </div>

      {isError ? (
        <div className="mt-6">
          <ErrorState description="We could not load your orders." onRetry={() => refetch()} />
        </div>
      ) : isLoading ? (
        <div className="mt-6 flex flex-col gap-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-36 w-full" />
          ))}
        </div>
      ) : (data?.content.length ?? 0) === 0 ? (
        <div className="mt-6">
          <EmptyState
            icon={Package}
            title={status === "ALL" ? "No orders yet" : "No orders with that status"}
            description={
              status === "ALL"
                ? "When you place an order it will appear here with live tracking."
                : "Try a different filter."
            }
            action={
              status === "ALL" ? (
                <Link
                  href="/products"
                  className="inline-flex h-11 items-center rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
                >
                  Start shopping
                </Link>
              ) : (
                <Button variant="secondary" onClick={() => setStatus("ALL")}>
                  Show all orders
                </Button>
              )
            }
          />
        </div>
      ) : (
        <>
          <ul className="mt-6 flex flex-col gap-4">
            {data!.content.map((order) => (
              <li key={order.id} className="rounded-[--radius-lg] border border-line bg-surface">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line px-4 py-3">
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
                    <span className="tabular text-[length:--text-body] font-semibold text-ink">
                      {order.orderNumber}
                    </span>
                    <span className="text-[length:--text-small] text-muted">
                      {formatDate(order.placedAt)}
                    </span>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <span className="tabular text-[length:--text-body] font-semibold text-ink">
                    {formatCurrency(order.total, order.currency)}
                  </span>
                </div>

                <div className="flex items-center gap-3 px-4 py-4">
                  {/* A strip of thumbnails identifies an order far faster than a
                      list of product names. */}
                  <div className="flex -space-x-2">
                    {order.items.slice(0, 4).map((item) => (
                      <ProductImage
                        key={item.productId}
                        src={item.imageUrl ?? ""}
                        alt={item.name}
                        sizes="48px"
                        className="size-12 rounded-[--radius-md] border-2 border-surface"
                      />
                    ))}
                    {order.items.length > 4 && (
                      <span className="tabular flex size-12 items-center justify-center rounded-[--radius-md] border-2 border-surface bg-sunken text-[length:--text-caption] font-medium text-muted">
                        +{order.items.length - 4}
                      </span>
                    )}
                  </div>

                  <p className="min-w-0 flex-1 truncate text-[length:--text-small] text-muted">
                    {order.items.map((item) => item.name).join(", ")}
                  </p>

                  <Link
                    href={`/account/orders/${order.id}`}
                    className="shrink-0 rounded-[--radius-md] border border-line-strong bg-surface px-3 py-2 text-[length:--text-small] font-medium text-ink transition-colors hover:bg-sunken"
                  >
                    View
                  </Link>
                </div>
              </li>
            ))}
          </ul>

          {data && <Pagination meta={data.page} onPageChange={setPage} className="mt-8" />}
        </>
      )}
    </div>
  );
}
