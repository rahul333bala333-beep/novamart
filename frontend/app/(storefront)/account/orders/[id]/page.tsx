"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Printer, Receipt, RotateCw } from "lucide-react";
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
import { useCancelOrder, useOrder } from "@/lib/hooks/use-orders";
import { useCartMutations } from "@/lib/hooks/use-cart";
import { formatCurrency, formatDate } from "@/lib/format";

export default function OrderDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const toast = useToast();
  const { data, isLoading, isError, refetch } = useOrder(params.id);
  const cancelOrder = useCancelOrder();
  const { addItem } = useCartMutations();
  const [confirmCancel, setConfirmCancel] = React.useState(false);
  const [reordering, setReordering] = React.useState(false);

  if (isError) {
    return <ErrorState description="We could not load this order." onRetry={() => refetch()} />;
  }

  if (isLoading || !data) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  const order = data.order;
  const cancellable = ["PENDING", "CONFIRMED", "PROCESSING"].includes(order.status);

  async function handleBuyAgain() {
    setReordering(true);
    try {
      for (const item of order.items) {
        await addItem.mutateAsync({ productId: item.productId, quantity: item.quantity });
      }
      toast.success("Items added to your bag");
      router.push("/cart");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to reorder some items";
      toast.error(message);
    } finally {
      setReordering(false);
    }
  }

  return (
    <div>
      <Link
        href="/account/orders"
        className="inline-flex items-center gap-1.5 text-[length:--text-small] text-muted transition-colors hover:text-ink"
      >
        <ArrowLeft className="size-3.5" aria-hidden="true" />
        All orders
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="tabular font-[family-name:--font-display] text-[length:--text-h2] font-semibold text-ink">
            {order.orderNumber}
          </h2>
          <p className="mt-1 text-[length:--text-body] text-muted">
            Placed {formatDate(order.placedAt)}
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

          <Button
            size="sm"
            loading={reordering}
            onClick={handleBuyAgain}
          >
            <RotateCw className="size-3.5 mr-1.5" />
            Buy again
          </Button>

          {cancellable && (
            <Button variant="secondary" size="sm" onClick={() => setConfirmCancel(true)}>
              Cancel order
            </Button>
          )}
        </div>
      </div>

      {order.status === "CANCELLED" && order.cancelledReason && (
        <div
          role="status"
          className="mt-5 rounded-[--radius-lg] border border-danger/25 bg-danger-soft px-4 py-3 text-[length:--text-body] text-ink"
        >
          This order was cancelled. {order.cancelledReason}
        </div>
      )}

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_18rem] lg:gap-12">
        <div className="flex flex-col gap-8">
          <section>
            <h3 className="border-b border-line pb-3 text-[length:--text-lead] font-semibold text-ink">
              Progress
            </h3>
            <div className="mt-5">
              <OrderTimeline timeline={data.timeline} status={order.status} />
            </div>
          </section>

          <section>
            <h3 className="border-b border-line pb-3 text-[length:--text-lead] font-semibold text-ink">
              Items
            </h3>
            <ul className="divide-y divide-line">
              {order.items.map((item) => (
                <li key={item.productId} className="flex items-center gap-4 py-4">
                  <ProductImage
                    src={item.imageUrl ?? ""}
                    alt={item.name}
                    sizes="64px"
                    className="size-16 shrink-0 rounded-[--radius-md] border border-line"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[length:--text-body] text-ink">
                      {item.slug ? (
                        <Link href={`/products/${item.slug}`} className="hover:underline">
                          {item.name}
                        </Link>
                      ) : (
                        item.name
                      )}
                    </p>
                    <p className="tabular text-[length:--text-small] text-muted">
                      {item.quantity} &times; {formatCurrency(item.unitPrice, order.currency)}
                    </p>
                  </div>
                  <p className="tabular text-[length:--text-body] font-medium text-ink">
                    {formatCurrency(item.lineTotal, order.currency)}
                  </p>
                </li>
              ))}
            </ul>
          </section>
        </div>

        <aside className="flex flex-col gap-5">
          <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
            <h3 className="flex items-center gap-2 text-[length:--text-body] font-semibold text-ink">
              <Receipt className="size-4 text-muted" aria-hidden="true" />
              Payment
            </h3>
            <dl className="mt-3 flex flex-col gap-2 text-[length:--text-small]">
              <div className="flex justify-between">
                <dt className="text-muted">Subtotal</dt>
                <dd className="tabular text-ink">{formatCurrency(order.subtotal, order.currency)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted">Delivery</dt>
                <dd className="tabular text-ink">
                  {order.deliveryFee === 0 ? (
                    <span className="text-success">Free</span>
                  ) : (
                    formatCurrency(order.deliveryFee, order.currency)
                  )}
                </dd>
              </div>
              <div className="flex justify-between border-t border-line pt-2">
                <dt className="font-semibold text-ink">Total</dt>
                <dd className="tabular font-semibold text-ink">
                  {formatCurrency(order.total, order.currency)}
                </dd>
              </div>
              <div className="mt-1 flex items-center justify-between">
                <dt className="text-muted">Method</dt>
                <dd className="text-ink">
                  {order.paymentMethod === "CASH_ON_DELIVERY" ? "Cash on delivery" : "Card (simulated)"}
                </dd>
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
              <h3 className="text-[length:--text-body] font-semibold text-ink">Delivery address</h3>
              <address className="mt-2 not-italic text-[length:--text-small] leading-relaxed text-muted">
                {order.shippingAddress.recipientName}
                <br />
                {order.shippingAddress.line1}
                {order.shippingAddress.line2 ? `, ${order.shippingAddress.line2}` : ""}
                <br />
                {order.shippingAddress.city}, {order.shippingAddress.state}{" "}
                {order.shippingAddress.postalCode}
                <br />
                {order.shippingAddress.country}
              </address>
              {order.estimatedDeliveryDate && order.status !== "CANCELLED" && (
                <p className="mt-3 border-t border-line pt-3 text-[length:--text-small] text-ink">
                  Estimated {formatDate(order.estimatedDeliveryDate)}
                </p>
              )}
            </section>
          )}

          {order.notes && (
            <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
              <h3 className="text-[length:--text-body] font-semibold text-ink">Delivery notes</h3>
              <p className="mt-2 text-[length:--text-small] leading-relaxed text-muted">{order.notes}</p>
            </section>
          )}
        </aside>
      </div>

      <ConfirmDialog
        open={confirmCancel}
        onClose={() => setConfirmCancel(false)}
        onConfirm={() =>
          cancelOrder.mutate(
            { id: order.id, reason: "Cancelled by customer" },
            {
              onSuccess: () => {
                setConfirmCancel(false);
                toast.success("Order cancelled and stock returned");
              },
              onError: (error) => {
                setConfirmCancel(false);
                toast.error(
                  error instanceof ApiError ? error.message : "We could not cancel this order."
                );
              },
            }
          )
        }
        title={`Cancel order ${order.orderNumber}?`}
        description="The items will be returned to stock and any payment taken will be refunded. This cannot be undone."
        confirmLabel="Cancel order"
        loading={cancelOrder.isPending}
        destructive
      />
    </div>
  );
}
