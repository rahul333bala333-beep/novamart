"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ShoppingBag, Trash2, TriangleAlert } from "lucide-react";
import * as React from "react";
import { ProductImage } from "@/components/commerce/product-image";
import { QuantityStepper } from "@/components/commerce/quantity-stepper";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth/auth-context";
import { useCart, useCartMutations } from "@/lib/hooks/use-cart";
import { formatCurrency } from "@/lib/format";
import { OrderSummary } from "@/components/commerce/order-summary";

export default function CartPage() {
  const router = useRouter();
  const { isAuthenticated, initialising } = useAuth();
  const { data: cart, isLoading, isError, refetch } = useCart();
  const { setQuantity, removeItem, clear } = useCartMutations();
  const [confirmClear, setConfirmClear] = React.useState(false);

  if (!initialising && !isAuthenticated) {
    return (
      <div className="container-page py-16">
        <EmptyState
          icon={ShoppingBag}
          title="Sign in to see your bag"
          description="Your bag is saved to your account, so it is waiting for you on any device."
          action={
            <Link
              href="/login?next=/cart"
              className="inline-flex h-11 items-center rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
            >
              Sign in
            </Link>
          }
        />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="container-page py-16">
        <ErrorState description="We could not load your bag." onRetry={() => refetch()} />
      </div>
    );
  }

  if (isLoading || initialising) {
    return (
      <div className="container-page py-10">
        <Skeleton className="h-9 w-40" />
        <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_22rem]">
          <div className="flex flex-col gap-4">
            {Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="h-32 w-full" />
            ))}
          </div>
          <Skeleton className="h-72 w-full" />
        </div>
      </div>
    );
  }

  const items = cart?.items ?? [];

  if (items.length === 0) {
    return (
      <div className="container-page py-16">
        <EmptyState
          icon={ShoppingBag}
          title="Your bag is empty"
          description="Once you add something it will show up here, along with live stock and delivery costs."
          action={
            <Link
              href="/products"
              className="inline-flex h-11 items-center rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
            >
              Start browsing
            </Link>
          }
        />
      </div>
    );
  }

  // A line can exceed available stock if someone else bought the item after it
  // was added. Flagging it here, rather than letting checkout fail, means the
  // shopper finds out while they can still do something about it.
  const problemLines = items.filter((item) => !item.inStock || item.quantity > item.availableQuantity);

  return (
    <div className="container-page py-8 lg:py-10">
      <div className="flex items-end justify-between gap-4 border-b border-line pb-5">
        <h1 className="font-[family-name:--font-display] text-[length:--text-h1] font-semibold tracking-[-0.02em] text-ink">
          Your bag
        </h1>
        <button
          type="button"
          onClick={() => setConfirmClear(true)}
          className="cursor-pointer text-[length:--text-small] text-muted underline underline-offset-4 transition-colors hover:text-danger"
        >
          Empty bag
        </button>
      </div>

      {problemLines.length > 0 && (
        <div
          role="alert"
          className="mt-5 flex gap-3 rounded-[--radius-lg] border border-warning/30 bg-warning-soft px-4 py-3"
        >
          <TriangleAlert className="mt-0.5 size-4 shrink-0 text-warning" aria-hidden="true" />
          <p className="text-[length:--text-body] text-ink">
            Some items are no longer available in the quantity you chose. Adjust them before
            checking out.
          </p>
        </div>
      )}

      <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_22rem] lg:gap-14">
        <ul className="flex flex-col divide-y divide-line border-y border-line">
          {items.map((item) => {
            const overStock = item.quantity > item.availableQuantity || !item.inStock;
            return (
              <li key={item.productId} className="flex gap-4 py-5">
                <Link
                  href={`/products/${item.slug}`}
                  className="shrink-0 overflow-hidden rounded-[--radius-md] border border-line"
                >
                  <ProductImage
                    src={item.imageUrl}
                    alt={item.name}
                    sizes="112px"
                    className="size-24 sm:size-28"
                  />
                </Link>

                <div className="flex min-w-0 flex-1 flex-col">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h2 className="truncate text-[length:--text-body] font-medium text-ink">
                        <Link href={`/products/${item.slug}`} className="hover:underline">
                          {item.name}
                        </Link>
                      </h2>
                      <p className="tabular mt-1 text-[length:--text-small] text-muted">
                        {formatCurrency(item.unitPrice)} each
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => removeItem.mutate(item.productId)}
                      disabled={removeItem.isPending}
                      className="-mr-1 flex size-9 shrink-0 cursor-pointer items-center justify-center rounded-[--radius-md] text-muted transition-colors hover:bg-sunken hover:text-danger"
                      aria-label={`Remove ${item.name} from bag`}
                    >
                      <Trash2 className="size-4" />
                    </button>
                  </div>

                  {overStock && (
                    <p className="mt-1 text-[length:--text-caption] text-warning" role="status">
                      {item.availableQuantity === 0
                        ? "Now out of stock"
                        : `Only ${item.availableQuantity} left`}
                    </p>
                  )}

                  <div className="mt-auto flex items-end justify-between gap-3 pt-3">
                    <QuantityStepper
                      value={item.quantity}
                      onChange={(next) =>
                        setQuantity.mutate({ productId: item.productId, quantity: next })
                      }
                      max={Math.max(1, Math.min(20, item.availableQuantity))}
                      disabled={setQuantity.isPending}
                    />
                    <p className="tabular text-[length:--text-lead] font-semibold text-ink">
                      {formatCurrency(item.lineTotal)}
                    </p>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>

        <div className="lg:sticky lg:top-32 lg:h-fit">
          <OrderSummary subtotal={cart!.subtotal} currency={cart!.currency}>
            <Button
              size="lg"
              block
              disabled={problemLines.length > 0}
              // router.push, not window.location: a full page reload would
              // discard the React tree and every cached query, so the shopper
              // would watch the whole application boot again on the way to
              // checkout.
              onClick={() => router.push("/checkout")}
            >
              Checkout
            </Button>
            {problemLines.length > 0 && (
              <p className="mt-2 text-center text-[length:--text-caption] text-muted">
                Fix the flagged items to continue
              </p>
            )}
            <Link
              href="/products"
              className="mt-3 block text-center text-[length:--text-small] text-muted underline underline-offset-4 transition-colors hover:text-ink"
            >
              Continue shopping
            </Link>
          </OrderSummary>
        </div>
      </div>

      <ConfirmDialog
        open={confirmClear}
        onClose={() => setConfirmClear(false)}
        onConfirm={() => {
          clear.mutate(undefined, { onSuccess: () => setConfirmClear(false) });
        }}
        title="Empty your bag?"
        description="This removes every item. It cannot be undone."
        confirmLabel="Empty bag"
        loading={clear.isPending}
        destructive
      />
    </div>
  );
}
