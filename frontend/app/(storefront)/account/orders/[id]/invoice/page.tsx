"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, CheckCircle2, Printer } from "lucide-react";
import * as React from "react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useOrder } from "@/lib/hooks/use-orders";
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format";

export default function OrderInvoicePage() {
  const params = useParams<{ id: string }>();
  const { data, isLoading, isError } = useOrder(params.id);

  if (isError) {
    return (
      <div className="container-page py-16 text-center">
        <p className="text-danger font-medium">Unable to load invoice for this order.</p>
        <Link
          href="/account/orders"
          className="mt-4 inline-flex h-11 items-center justify-center rounded-[--radius-md] border border-line-strong bg-surface px-5 text-sm font-medium text-ink transition-colors hover:bg-sunken"
        >
          Back to Orders
        </Link>
      </div>
    );
  }

  if (isLoading || !data) {
    return (
      <div className="container-page py-10 max-w-4xl space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-96 w-full rounded-[--radius-lg]" />
      </div>
    );
  }

  const order = data.order;

  return (
    <div className="min-h-screen bg-sunken/40 py-8 lg:py-12 print:bg-white print:p-0">
      <div className="container-page max-w-4xl">
        {/* Controls - hidden on print */}
        <div className="mb-6 flex items-center justify-between print:hidden">
          <Link
            href={`/account/orders/${order.id}`}
            className="inline-flex items-center gap-1.5 text-sm text-muted transition-colors hover:text-ink"
          >
            <ArrowLeft className="size-4" />
            Back to Order Details
          </Link>
          <Button onClick={() => window.print()}>
            <Printer className="size-4 mr-2" />
            Print / Save as PDF
          </Button>
        </div>

        {/* Invoice Paper Document */}
        <article className="overflow-hidden rounded-[--radius-lg] border border-line bg-surface p-8 shadow-sm print:border-none print:shadow-none print:p-0">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-line pb-6 gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-[family-name:--font-display] text-2xl font-bold tracking-tight text-ink">
                  Nova Mart
                </span>
                <span className="rounded bg-ink/10 px-2 py-0.5 text-xs font-semibold text-ink uppercase tracking-wider">
                  Official Receipt
                </span>
              </div>
              <p className="mt-1 text-xs text-muted">
                Premium Online Shopping Platform &middot; GSTIN: 33AAAAA0000A1Z5
              </p>
              <p className="text-xs text-muted">
                support@novamart.dev &middot; +91 80000 12345
              </p>
            </div>

            <div className="text-left sm:text-right">
              <h1 className="text-xl font-bold uppercase tracking-wider text-ink">
                TAX INVOICE
              </h1>
              <p className="font-mono text-sm font-semibold text-ink mt-0.5">
                {order.orderNumber}
              </p>
              <p className="text-xs text-muted mt-1">
                Date: {formatDate(order.placedAt)}
              </p>
              <p className="text-xs text-muted">
                Time: {formatDateTime(order.placedAt)}
              </p>
            </div>
          </div>

          {/* Customer & Order Metadata */}
          <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-6 border-b border-line pb-6 text-sm">
            <div>
              <h2 className="text-xs font-semibold uppercase tracking-wider text-muted mb-2">
                Billed / Shipped To
              </h2>
              {order.shippingAddress ? (
                <div className="text-ink leading-relaxed">
                  <p className="font-medium">{order.shippingAddress.recipientName}</p>
                  <p>{order.shippingAddress.line1}</p>
                  {order.shippingAddress.line2 && <p>{order.shippingAddress.line2}</p>}
                  <p>
                    {order.shippingAddress.city}, {order.shippingAddress.state}{" "}
                    {order.shippingAddress.postalCode}
                  </p>
                  <p>{order.shippingAddress.country}</p>
                  <p className="text-muted mt-1">Phone: {order.shippingAddress.phone}</p>
                </div>
              ) : (
                <p className="text-muted">Standard Customer Delivery</p>
              )}
            </div>

            <div className="sm:text-right space-y-1.5">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-muted mb-2">
                Order & Payment Info
              </h2>
              <div className="flex justify-between sm:justify-end gap-4">
                <span className="text-muted">Order Status:</span>
                <span className="font-medium text-ink uppercase">{order.status.replace(/_/g, " ")}</span>
              </div>
              <div className="flex justify-between sm:justify-end gap-4">
                <span className="text-muted">Payment Method:</span>
                <span className="font-medium text-ink">
                  {order.paymentMethod === "CASH_ON_DELIVERY" ? "Cash on Delivery" : "Online / Card"}
                </span>
              </div>
              <div className="flex justify-between sm:justify-end gap-4">
                <span className="text-muted">Payment Status:</span>
                <span className="font-medium text-emerald-600 dark:text-emerald-400">
                  {order.paymentStatus}
                </span>
              </div>
              {order.paymentId && (
                <div className="flex justify-between sm:justify-end gap-4">
                  <span className="text-muted">Transaction ID:</span>
                  <span className="font-mono text-xs text-ink">{order.paymentId.substring(0, 16)}...</span>
                </div>
              )}
            </div>
          </div>

          {/* Line Items Table */}
          <div className="mt-6">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line text-xs font-semibold uppercase tracking-wider text-muted">
                  <th className="py-3 pr-4">#</th>
                  <th className="py-3 px-4">Item Description</th>
                  <th className="py-3 px-4 text-center">Qty</th>
                  <th className="py-3 px-4 text-right">Unit Price</th>
                  <th className="py-3 pl-4 text-right">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {order.items.map((item, idx) => (
                  <tr key={item.productId}>
                    <td className="py-4 pr-4 font-mono text-xs text-muted">{idx + 1}</td>
                    <td className="py-4 px-4">
                      <p className="font-medium text-ink">{item.name}</p>
                      {item.sku && (
                        <p className="font-mono text-xs text-muted">SKU: {item.sku}</p>
                      )}
                    </td>
                    <td className="py-4 px-4 text-center font-medium text-ink">{item.quantity}</td>
                    <td className="py-4 px-4 text-right tabular text-muted">
                      {formatCurrency(item.unitPrice, order.currency)}
                    </td>
                    <td className="py-4 pl-4 text-right tabular font-semibold text-ink">
                      {formatCurrency(item.lineTotal, order.currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Financial Breakdown */}
          <div className="mt-6 border-t border-line pt-4 flex flex-col sm:flex-row sm:justify-between items-start gap-4">
            <div className="max-w-xs text-xs text-muted leading-relaxed">
              <p className="font-medium text-ink mb-1">Terms & Guarantee:</p>
              <p>All items covered under Nova Mart 30-day return policy and standard manufacturer warranty.</p>
              <div className="mt-2 flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-medium">
                <CheckCircle2 className="size-3.5" />
                Verified & Authorized Invoice
              </div>
            </div>

            <div className="w-full sm:w-72 space-y-2 text-sm">
              <div className="flex justify-between text-muted">
                <span>Subtotal</span>
                <span className="tabular font-medium text-ink">
                  {formatCurrency(order.subtotal, order.currency)}
                </span>
              </div>
              <div className="flex justify-between text-muted">
                <span>Delivery Fee</span>
                <span className="tabular font-medium text-ink">
                  {order.deliveryFee === 0 ? "FREE" : formatCurrency(order.deliveryFee, order.currency)}
                </span>
              </div>
              {order.discount > 0 && (
                <div className="flex justify-between text-emerald-600 dark:text-emerald-400 font-medium">
                  <span>Coupon Discount</span>
                  <span className="tabular">
                    -{formatCurrency(order.discount, order.currency)}
                  </span>
                </div>
              )}
              <div className="flex justify-between border-t-2 border-ink pt-2 text-base font-bold text-ink">
                <span>Grand Total</span>
                <span className="tabular">
                  {formatCurrency(order.total, order.currency)}
                </span>
              </div>
              <p className="text-[11px] text-muted text-right">
                (Inclusive of all statutory taxes & GST)
              </p>
            </div>
          </div>

          {/* Footer note */}
          <div className="mt-8 border-t border-dashed border-line pt-4 text-center text-xs text-muted">
            Thank you for shopping with Nova Mart! For inquiries, reach us at support@novamart.dev.
          </div>
        </article>
      </div>
    </div>
  );
}
