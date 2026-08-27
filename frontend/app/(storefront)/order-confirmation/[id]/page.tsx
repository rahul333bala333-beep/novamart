"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { CheckCircle2, Package, Truck } from "lucide-react";
import { ProductImage } from "@/components/commerce/product-image";
import { Badge } from "@/components/ui/badge";
import { ErrorState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useOrder } from "@/lib/hooks/use-orders";
import { formatCurrency, formatDate } from "@/lib/format";

/**
 * Post-purchase confirmation.
 *
 * Answers the four questions a shopper has the moment they have paid: did it
 * work, what is my reference, what did I buy, and when will it arrive. Every
 * value here is read back from the server rather than carried over from the
 * checkout form, so what is shown is what was actually recorded.
 */
export default function OrderConfirmationPage() {
  const params = useParams<{ id: string }>();
  const { data, isLoading, isError, refetch } = useOrder(params.id);

  if (isError) {
    return (
      <div className="container-page py-16">
        <ErrorState
          title="We could not load your order"
          description="Your order may still have been placed. Check My orders before trying again."
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  if (isLoading || !data) {
    return (
      <div className="container-page py-16">
        <Skeleton className="mx-auto h-14 w-14 rounded-full" />
        <Skeleton className="mx-auto mt-4 h-8 w-64" />
        <Skeleton className="mx-auto mt-8 h-64 w-full max-w-2xl" />
      </div>
    );
  }

  const order = data.order;
  const paid = order.paymentStatus === "SUCCESS";

  return (
    <div className="container-page py-10 lg:py-14">
      <div className="mx-auto max-w-2xl">
        <div className="flex flex-col items-center text-center">
          <span className="flex size-14 items-center justify-center rounded-full bg-success-soft">
            <CheckCircle2 className="size-7 text-success" aria-hidden="true" />
          </span>
          <h1 className="mt-4 font-[family-name:--font-display] text-[length:--text-h1] font-semibold tracking-[-0.02em] text-ink">
            Thank you, your order is confirmed
          </h1>
          <p className="mt-2 text-[length:--text-body] text-muted">
            A confirmation has been recorded against your account, and you can track this order at
            any time.
          </p>
        </div>

        <dl className="mt-8 grid gap-px overflow-hidden rounded-[--radius-lg] border border-line bg-line sm:grid-cols-3">
          <div className="bg-surface px-4 py-4">
            <dt className="text-[length:--text-caption] uppercase tracking-[0.08em] text-muted">
              Order number
            </dt>
            <dd className="tabular mt-1 text-[length:--text-body] font-semibold text-ink">
              {order.orderNumber}
            </dd>
          </div>
          <div className="bg-surface px-4 py-4">
            <dt className="text-[length:--text-caption] uppercase tracking-[0.08em] text-muted">
              Payment
            </dt>
            <dd className="mt-1">
              <Badge tone={paid ? "success" : "warning"}>
                {paid ? "Paid (simulated)" : order.paymentStatus === "INITIATED" ? "Due on delivery" : order.paymentStatus}
              </Badge>
            </dd>
          </div>
          <div className="bg-surface px-4 py-4">
            <dt className="text-[length:--text-caption] uppercase tracking-[0.08em] text-muted">
              Estimated delivery
            </dt>
            <dd className="mt-1 text-[length:--text-body] font-semibold text-ink">
              {formatDate(order.estimatedDeliveryDate)}
            </dd>
          </div>
        </dl>

        <section className="mt-8 rounded-[--radius-lg] border border-line bg-surface">
          <h2 className="border-b border-line px-5 py-3.5 text-[length:--text-body] font-semibold text-ink">
            What you ordered
          </h2>
          <ul className="divide-y divide-line">
            {order.items.map((item) => (
              <li key={item.productId} className="flex items-center gap-4 px-5 py-4">
                <ProductImage
                  src={item.imageUrl ?? ""}
                  alt={item.name}
                  sizes="56px"
                  className="size-14 shrink-0 rounded-[--radius-md]"
                />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[length:--text-body] text-ink">{item.name}</p>
                  <p className="tabular text-[length:--text-small] text-muted">
                    Quantity {item.quantity}
                  </p>
                </div>
                <p className="tabular text-[length:--text-body] font-medium text-ink">
                  {formatCurrency(item.lineTotal, order.currency)}
                </p>
              </li>
            ))}
          </ul>

          <dl className="flex flex-col gap-2 border-t border-line bg-sunken px-5 py-4 text-[length:--text-body]">
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
          </dl>
        </section>

        {order.shippingAddress && (
          <section className="mt-6 rounded-[--radius-lg] border border-line bg-surface px-5 py-4">
            <h2 className="flex items-center gap-2 text-[length:--text-body] font-semibold text-ink">
              <Truck className="size-4 text-muted" aria-hidden="true" />
              Delivering to
            </h2>
            <address className="mt-2 not-italic text-[length:--text-body] leading-relaxed text-ink-soft">
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
          </section>
        )}

        {/* Stated rather than glossed over. notification-service records the
            message and writes it to its log, but no mail provider is connected,
            so telling the shopper to "check their inbox" would be false. */}
        <p className="mt-6 rounded-[--radius-md] bg-sunken px-4 py-3 text-center text-[length:--text-caption] leading-relaxed text-muted">
          This is a demonstration system. Your confirmation was recorded by the notification service
          but no email was actually sent, and no payment was actually taken.
        </p>

        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Link
            href={`/account/orders/${order.id}`}
            className="inline-flex h-11 items-center justify-center gap-2 rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
          >
            <Package className="size-4" aria-hidden="true" />
            Track this order
          </Link>
          <Link
            href="/products"
            className="inline-flex h-11 items-center justify-center rounded-[--radius-md] border border-line-strong bg-surface px-5 text-[length:--text-body] font-medium text-ink transition-colors hover:bg-sunken"
          >
            Continue shopping
          </Link>
        </div>
      </div>
    </div>
  );
}
