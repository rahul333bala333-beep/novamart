"use client";

import { useRouter } from "next/navigation";
import { ShoppingCart } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { DataTable, type Column } from "@/components/admin/data-table";
import { OrderStatusBadge } from "@/components/commerce/order-status-badge";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { useOrders } from "@/lib/hooks/use-orders";
import { formatCurrency, formatDate } from "@/lib/format";
import type { Order, OrderStatus } from "@/lib/types";

const STATUSES: (OrderStatus | "ALL")[] = [
  "ALL",
  "PENDING",
  "CONFIRMED",
  "PROCESSING",
  "SHIPPED",
  "DELIVERED",
  "CANCELLED",
];

export default function AdminOrdersPage() {
  const router = useRouter();
  const [page, setPage] = React.useState(0);
  const [status, setStatus] = React.useState<OrderStatus | "ALL">("ALL");

  const { data, isLoading, isError, refetch } = useOrders({
    page,
    size: 15,
    status: status === "ALL" ? undefined : status,
  });

  const columns: Column<Order>[] = [
    {
      key: "order",
      header: "Order",
      cell: (order) => <span className="tabular font-medium">{order.orderNumber}</span>,
    },
    { key: "placed", header: "Placed", cell: (order) => formatDate(order.placedAt) },
    {
      key: "items",
      header: "Items",
      cell: (order) => (
        <span className="tabular">{order.items.reduce((sum, item) => sum + item.quantity, 0)}</span>
      ),
    },
    {
      key: "payment",
      header: "Payment",
      showOnMobile: false,
      cell: (order) => (
        <span className="text-[length:--text-small] text-muted">
          {order.paymentMethod === "CASH_ON_DELIVERY" ? "Cash" : "Card"} &middot; {order.paymentStatus}
        </span>
      ),
    },
    { key: "status", header: "Status", cell: (order) => <OrderStatusBadge status={order.status} /> },
    {
      key: "total",
      header: "Total",
      align: "right",
      cell: (order) => (
        <span className="tabular font-medium">{formatCurrency(order.total, order.currency)}</span>
      ),
    },
  ];

  if (isError) {
    return <ErrorState description="We could not load orders." onRetry={() => refetch()} />;
  }

  return (
    <div>
      <AdminPageHeader
        title="Orders"
        description={
          data ? `${data.page.totalElements} orders across all customers` : "Every order in the platform"
        }
      />

      <div className="-mx-1 mb-4 flex gap-1 overflow-x-auto pb-2">
        {STATUSES.map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => {
              setStatus(value);
              setPage(0);
            }}
            aria-pressed={status === value}
            className={
              status === value
                ? "shrink-0 cursor-pointer rounded-[--radius-md] bg-ink px-3 py-1.5 text-[length:--text-small] font-medium text-white"
                : "shrink-0 cursor-pointer rounded-[--radius-md] border border-line-strong bg-surface px-3 py-1.5 text-[length:--text-small] text-ink-soft transition-colors hover:bg-sunken"
            }
          >
            {value === "ALL" ? "All" : value.charAt(0) + value.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      <DataTable
        caption="All orders"
        columns={columns}
        rows={data?.content ?? []}
        keyOf={(order) => order.id}
        loading={isLoading}
        onRowClick={(order) => router.push(`/admin/orders/${order.id}`)}
        emptyState={
          <EmptyState
            icon={ShoppingCart}
            title="No orders to show"
            description={status === "ALL" ? "No orders have been placed yet." : "No orders have that status."}
          />
        }
      />

      {data && <Pagination meta={data.page} onPageChange={setPage} className="mt-6" />}
    </div>
  );
}
