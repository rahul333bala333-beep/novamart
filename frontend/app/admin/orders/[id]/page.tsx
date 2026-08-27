"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, Printer } from "lucide-react";
import * as React from "react";
import { ProductImage } from "@/components/commerce/product-image";
import { OrderStatusBadge } from "@/components/commerce/order-status-badge";
import { OrderTimeline } from "@/components/commerce/order-timeline";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/dialog";
import { ErrorState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { ApiError } from "@/lib/api/client";
import { orderApi } from "@/lib/api/resources";
import { useCancelOrder, useOrder, orderKeys } from "@/lib/hooks/use-orders";
import { useQueryClient } from "@tanstack/react-query";
import { formatCurrency, formatDateTime } from "@/lib/format";
import type { OrderStatus } from "@/lib/types";

/**
 * The single legal forward transition from each state.
 *
 * Mirrors the state machine in `OrderStatus.java`. Offering only the one valid
 * next step means an administrator cannot construct an illegal transition and
 * then be told off for it; the server still enforces the rule regardless.
 */
const NEXT_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
  CONFIRMED: "PROCESSING",
  PROCESSING: "SHIPPED",
  SHIPPED: "OUT_FOR_DELIVERY",
  OUT_FOR_DELIVERY: "DELIVERED",
};

export default function AdminOrderDetailPage() {
  const params = useParams<{ id: string }>();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { data, isLoading, isError, refetch } = useOrder(params.id);
  const cancelOrder = useCancelOrder();

  const [advancing, setAdvancing] = React.useState(false);
  const [confirmCancel, setConfirmCancel] = React.useState(false);

  if (isError) {
    return <ErrorState description="We could not load this order." onRetry={() => refetch()} />;
  }

  if (isLoading || !data) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-72 w-full" />
      </div>
    );
  }

  const order = data.order;
  const next = NEXT_STATUS[order.status];
  const cancellable = ["PENDING", "CONFIRMED", "PROCESSING"].includes(order.status);

  async function advance() {
    if (!next) return;
    setAdvancing(true);
    try {
      await orderApi.updateStatus(order.id, next);
      await queryClient.invalidateQueries({ queryKey: orderKeys.detail(order.id) });
      await queryClient.invalidateQueries({ queryKey: ["orders"] });
      toast.success(`Order marked ${next.toLowerCase()}`);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "We could not update this order.");
    } finally {
      setAdvancing(false);
    }
  }

  return (
    <div>
      <Link
        href="/admin/orders"
        className="inline-flex items-center gap-1.5 text-[length:--text-small] text-muted transition-colors hover:text-ink"
      >
        <ArrowLeft className="size-3.5" aria-hidden="true" />
        All orders
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-4 border-b border-line pb-5">
        <div>
          <h1 className="tabular font-[family-name:--font-display] text-[length:--text-h2] font-semibold text-ink">
            {order.orderNumber}
          </h1>
          <p className="mt-1 text-[length:--text-body] text-muted">
            {formatDateTime(order.placedAt)}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <OrderStatusBadge status={order.status} />

          <Link
            href={`/account/orders/${order.id}/invoice`}
            target="_blank"
            className="inline-flex h-9 items-center justify-center gap-1.5 rounded-[--radius-md] border border-line-strong bg-surface px-3 text-sm font-medium text-ink transition-colors hover:bg-sunken"
          >
            <Printer className="size-3.5 mr-1.5" />
            Invoice
          </Link>

          {next && (
            <Button size="sm" onClick={advance} loading={advancing}>
              Mark {next.toLowerCase().replace(/_/g, " ")}
            </Button>
          )}
          {cancellable && (
            <Button size="sm" variant="secondary" onClick={() => setConfirmCancel(true)}>
              Cancel
            </Button>
          )}
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_20rem]">
        <div className="flex flex-col gap-6">
          <section className="rounded-[--radius-lg] border border-line bg-surface">
            <h2 className="border-b border-line px-4 py-3 text-[length:--text-body] font-semibold text-ink">
              Items
            </h2>
            <ul className="divide-y divide-line">
              {order.items.map((item) => (
                <li key={item.productId} className="flex items-center gap-3 px-4 py-3">
                  <ProductImage
                    src={item.imageUrl ?? ""}
                    alt={item.name}
                    sizes="48px"
                    className="size-12 shrink-0 rounded-[--radius-md] border border-line"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[length:--text-body] text-ink">{item.name}</p>
                    <p className="tabular text-[length:--text-caption] text-muted">
                      {item.sku} &middot; {item.quantity} &times;{" "}
                      {formatCurrency(item.unitPrice, order.currency)}
                    </p>
                  </div>
                  <p className="tabular text-[length:--text-body] font-medium text-ink">
                    {formatCurrency(item.lineTotal, order.currency)}
                  </p>
                </li>
              ))}
            </ul>
          </section>

          <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
            <h2 className="text-[length:--text-body] font-semibold text-ink">Timeline</h2>
            <div className="mt-4">
              <OrderTimeline timeline={data.timeline} status={order.status} />
            </div>
          </section>
        </div>

        <aside className="flex flex-col gap-4">
          <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
            <h2 className="text-[length:--text-body] font-semibold text-ink">Totals</h2>
            <dl className="mt-3 flex flex-col gap-2 text-[length:--text-small]">
              <div className="flex justify-between">
                <dt className="text-muted">Subtotal</dt>
                <dd className="tabular">{formatCurrency(order.subtotal, order.currency)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted">Delivery</dt>
                <dd className="tabular">{formatCurrency(order.deliveryFee, order.currency)}</dd>
              </div>
              <div className="flex justify-between border-t border-line pt-2 font-semibold text-ink">
                <dt>Total</dt>
                <dd className="tabular">{formatCurrency(order.total, order.currency)}</dd>
              </div>
            </dl>
          </section>

          <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
            <h2 className="text-[length:--text-body] font-semibold text-ink">Payment</h2>
            <dl className="mt-3 flex flex-col gap-2 text-[length:--text-small]">
              <div className="flex items-center justify-between">
                <dt className="text-muted">Method</dt>
                <dd>{order.paymentMethod === "CASH_ON_DELIVERY" ? "Cash on delivery" : "Card (simulated)"}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted">Status</dt>
                <dd>
                  <Badge
                    tone={
                      order.paymentStatus === "SUCCESS"
                        ? "success"
                        : order.paymentStatus === "REFUNDED"
                          ? "info"
                          : order.paymentStatus === "FAILED"
                            ? "danger"
                            : "warning"
                    }
                  >
                    {order.paymentStatus}
                  </Badge>
                </dd>
              </div>
            </dl>
          </section>

          {order.shippingAddress && (
            <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
              <h2 className="text-[length:--text-body] font-semibold text-ink">Ship to</h2>
              <address className="mt-2 not-italic text-[length:--text-small] leading-relaxed text-muted">
                {order.shippingAddress.recipientName}
                <br />
                {order.shippingAddress.line1}
                {order.shippingAddress.line2 ? `, ${order.shippingAddress.line2}` : ""}
                <br />
                {order.shippingAddress.city}, {order.shippingAddress.state}{" "}
                {order.shippingAddress.postalCode}
                <br />
                {order.shippingAddress.phone}
              </address>
            </section>
          )}
        </aside>
      </div>

      <ConfirmDialog
        open={confirmCancel}
        onClose={() => setConfirmCancel(false)}
        onConfirm={() =>
          cancelOrder.mutate(
            { id: order.id, reason: "Cancelled by administrator" },
            {
              onSuccess: () => {
                setConfirmCancel(false);
                toast.success("Order cancelled, stock returned and payment refunded");
              },
              onError: (error) => {
                setConfirmCancel(false);
                toast.error(error instanceof ApiError ? error.message : "We could not cancel this order.");
              },
            }
          )
        }
        title={`Cancel ${order.orderNumber}?`}
        description="Stock will be returned to inventory and any captured payment refunded."
        confirmLabel="Cancel order"
        loading={cancelOrder.isPending}
        destructive
      />
    </div>
  );
}
