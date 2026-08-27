"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Check, CreditCard, Banknote, MapPin, Plus, TriangleAlert } from "lucide-react";
import * as React from "react";
import { AddressForm } from "@/components/commerce/address-form";
import { OrderSummary } from "@/components/commerce/order-summary";
import { ProductImage } from "@/components/commerce/product-image";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { ApiError } from "@/lib/api/client";
import { useAddresses, useAddressMutations } from "@/lib/hooks/use-addresses";
import { useCart } from "@/lib/hooks/use-cart";
import { usePlaceOrder } from "@/lib/hooks/use-orders";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/cn";
import type { PaymentMethod } from "@/lib/types";

/**
 * Checkout.
 *
 * One page with three clearly separated sections rather than a multi-step wizard.
 * A wizard hides the total until the last step and makes going back to change an
 * address feel like losing progress; a single reviewable page does not.
 */
export default function CheckoutPage() {
  const router = useRouter();
  const toast = useToast();

  const { data: cart, isLoading: cartLoading } = useCart();
  const { data: addresses, isLoading: addressesLoading } = useAddresses();
  const { create: createAddress } = useAddressMutations();

  /**
   * One idempotency key per mounted checkout.
   *
   * Deliberately created with `useState` rather than regenerated on each submit.
   * A fresh key per click would make two rapid submissions look like two
   * different orders to the server, which is exactly what the key exists to
   * prevent. Retrying the same attempt reuses this key and returns the original
   * order; only a genuinely new checkout (a remount) gets a new one.
   */
  const [idempotencyKey] = React.useState(() =>
    typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : `checkout-${Date.now()}-${Math.random().toString(36).slice(2)}`
  );

  const placeOrder = usePlaceOrder(idempotencyKey);

  // The chosen address, or empty while the shopper has not picked one.
  const [chosenAddressId, setChosenAddressId] = React.useState<string>("");
  const [method, setMethod] = React.useState<PaymentMethod>("MOCK_CARD");
  const [notes, setNotes] = React.useState("");
  const [simulateFailure, setSimulateFailure] = React.useState(false);
  const [showAddressForm, setShowAddressForm] = React.useState(false);
  const [appliedCoupon, setAppliedCoupon] = React.useState<import("@/lib/types").ValidateCouponResponse | null>(null);
  const [failure, setFailure] = React.useState<string | null>(null);

  /**
   * The effective selection: whatever the shopper picked, otherwise their
   * default address.
   *
   * Derived during render rather than copied into state by an effect. Syncing it
   * with `useEffect` + `setState` causes a second render pass on every load and
   * leaves a frame where nothing is selected; deriving it means the default is
   * already chosen on the very first paint.
   */
  const addressId =
    chosenAddressId || (addresses?.find((a) => a.isDefault) ?? addresses?.[0])?.id || "";
  const setAddressId = setChosenAddressId;

  const items = cart?.items ?? [];

  if (cartLoading || addressesLoading) {
    return (
      <div className="container-page py-10">
        <Skeleton className="h-9 w-48" />
        <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_22rem]">
          <div className="flex flex-col gap-6">
            <Skeleton className="h-48 w-full" />
            <Skeleton className="h-40 w-full" />
          </div>
          <Skeleton className="h-80 w-full" />
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="container-page py-16">
        <EmptyState
          title="There is nothing to check out"
          description="Your bag is empty."
          action={
            <Link
              href="/products"
              className="inline-flex h-11 items-center rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
            >
              Browse products
            </Link>
          }
        />
      </div>
    );
  }

  function submit() {
    setFailure(null);
    if (!addressId) {
      setFailure("Choose a delivery address before placing your order.");
      return;
    }

    placeOrder.mutate(
      {
        addressId,
        paymentMethod: method,
        notes: notes || undefined,
        simulateFailure,
        couponCode: appliedCoupon?.code || undefined,
      },
      {
        onSuccess: (order) => {
          toast.success("Order placed");
          router.push(`/order-confirmation/${order.id}`);
        },
        onError: (error) => {
          // The message shown comes from the API envelope, so a declined payment
          // says it was declined and an out-of-stock line says which. A generic
          // "checkout failed" would leave the shopper with nothing to act on.
          setFailure(
            error instanceof ApiError
              ? error.message
              : "We could not place your order. Please try again."
          );
        },
      }
    );
  }

  return (
    <div className="container-page py-8 lg:py-10">
      <h1 className="font-[family-name:--font-display] text-[length:--text-h1] font-semibold tracking-[-0.02em] text-ink">
        Checkout
      </h1>

      <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_22rem] lg:gap-14">
        <div className="flex flex-col gap-8">
          {/* ------------------------------------------------- 1. address */}
          <section aria-labelledby="address-heading">
            <div className="flex items-center justify-between gap-4 border-b border-line pb-3">
              <h2 id="address-heading" className="flex items-center gap-2 text-[length:--text-h3] font-semibold text-ink">
                <MapPin className="size-4 text-muted" aria-hidden="true" />
                Delivery address
              </h2>
              {!showAddressForm && (
                <Button variant="ghost" size="sm" onClick={() => setShowAddressForm(true)}>
                  <Plus className="size-4" aria-hidden="true" />
                  New address
                </Button>
              )}
            </div>

            {showAddressForm ? (
              <div className="mt-4 rounded-[--radius-lg] border border-line bg-surface p-5">
                <AddressForm
                  submitting={createAddress.isPending}
                  submitLabel="Use this address"
                  onCancel={() => setShowAddressForm(false)}
                  onSubmit={(values) =>
                    createAddress.mutate(values, {
                      onSuccess: (created) => {
                        setAddressId(created.id);
                        setShowAddressForm(false);
                        toast.success("Address saved");
                      },
                      onError: () => toast.error("We could not save that address."),
                    })
                  }
                />
              </div>
            ) : addresses && addresses.length > 0 ? (
              <fieldset className="mt-4">
                <legend className="sr-only">Choose a delivery address</legend>
                <div className="grid gap-3 sm:grid-cols-2">
                  {addresses.map((address) => (
                    <label
                      key={address.id}
                      className={cn(
                        "relative flex cursor-pointer flex-col gap-1 rounded-[--radius-lg] border bg-surface p-4 transition-colors",
                        addressId === address.id
                          ? "border-ink ring-1 ring-ink"
                          : "border-line hover:border-line-strong"
                      )}
                    >
                      <input
                        type="radio"
                        name="address"
                        value={address.id}
                        checked={addressId === address.id}
                        onChange={() => setAddressId(address.id)}
                        className="sr-only"
                      />
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-[length:--text-body] font-medium text-ink">
                          {address.label}
                        </span>
                        {address.isDefault && <Badge tone="neutral">Default</Badge>}
                        {addressId === address.id && (
                          <Check className="size-4 text-ink" aria-hidden="true" />
                        )}
                      </div>
                      <address className="not-italic text-[length:--text-small] leading-relaxed text-muted">
                        {address.recipientName}
                        <br />
                        {address.line1}
                        {address.line2 ? `, ${address.line2}` : ""}
                        <br />
                        {address.city}, {address.state} {address.postalCode}
                        <br />
                        {address.phone}
                      </address>
                    </label>
                  ))}
                </div>
              </fieldset>
            ) : (
              <div className="mt-4">
                <EmptyState
                  icon={MapPin}
                  title="No saved addresses"
                  description="Add one to continue with your order."
                  action={<Button onClick={() => setShowAddressForm(true)}>Add an address</Button>}
                />
              </div>
            )}
          </section>

          {/* -------------------------------------------------- 2. payment */}
          <section aria-labelledby="payment-heading">
            <h2
              id="payment-heading"
              className="flex items-center gap-2 border-b border-line pb-3 text-[length:--text-h3] font-semibold text-ink"
            >
              <CreditCard className="size-4 text-muted" aria-hidden="true" />
              Payment
            </h2>

            {/* Stated before the shopper chooses, not buried in the footer. */}
            <div className="mt-4 flex gap-3 rounded-[--radius-lg] border border-info/25 bg-info-soft px-4 py-3">
              <TriangleAlert className="mt-0.5 size-4 shrink-0 text-info" aria-hidden="true" />
              <p className="text-[length:--text-small] leading-relaxed text-ink">
                <strong className="font-semibold">This is a simulated payment gateway.</strong> No
                card details are collected and no money is taken. The order, the stock movement and
                the receipt are all real.
              </p>
            </div>

            <fieldset className="mt-4">
              <legend className="sr-only">Choose a payment method</legend>
              <div className="grid gap-3 sm:grid-cols-2">
                {(
                  [
                    {
                      value: "MOCK_CARD" as const,
                      icon: CreditCard,
                      title: "Card (simulated)",
                      body: "Settles immediately through the mock gateway.",
                    },
                    {
                      value: "CASH_ON_DELIVERY" as const,
                      icon: Banknote,
                      title: "Cash on delivery",
                      body: "Payment is collected when your order arrives.",
                    },
                  ]
                ).map((option) => (
                  <label
                    key={option.value}
                    className={cn(
                      "flex cursor-pointer gap-3 rounded-[--radius-lg] border bg-surface p-4 transition-colors",
                      method === option.value
                        ? "border-ink ring-1 ring-ink"
                        : "border-line hover:border-line-strong"
                    )}
                  >
                    <input
                      type="radio"
                      name="payment"
                      value={option.value}
                      checked={method === option.value}
                      onChange={() => setMethod(option.value)}
                      className="sr-only"
                    />
                    <option.icon className="mt-0.5 size-4 shrink-0 text-muted" aria-hidden="true" />
                    <div>
                      <p className="text-[length:--text-body] font-medium text-ink">{option.title}</p>
                      <p className="mt-0.5 text-[length:--text-caption] text-muted">{option.body}</p>
                    </div>
                  </label>
                ))}
              </div>
            </fieldset>

            {/* The demo switch is labelled as such and kept visually distinct
                from the real controls, so nobody mistakes it for a feature. */}
            {method === "MOCK_CARD" && (
              <label className="mt-4 flex cursor-pointer items-start gap-2.5 rounded-[--radius-md] border border-dashed border-line-strong bg-sunken px-4 py-3">
                <input
                  type="checkbox"
                  checked={simulateFailure}
                  onChange={(event) => setSimulateFailure(event.target.checked)}
                  className="mt-0.5 size-4 cursor-pointer accent-[--color-ink]"
                />
                <span className="text-[length:--text-small] leading-relaxed text-ink-soft">
                  <strong className="font-medium text-ink">Demonstration:</strong> force this payment
                  to be declined. The order will be cancelled and the stock it was holding released,
                  which is the compensation path of the checkout saga.
                </span>
              </label>
            )}
          </section>

          {/* --------------------------------------------------- 3. review */}
          <section aria-labelledby="review-heading">
            <h2
              id="review-heading"
              className="border-b border-line pb-3 text-[length:--text-h3] font-semibold text-ink"
            >
              Review your order
            </h2>

            <ul className="mt-4 flex flex-col divide-y divide-line rounded-[--radius-lg] border border-line bg-surface">
              {items.map((item) => (
                <li key={item.productId} className="flex items-center gap-4 p-4">
                  <ProductImage
                    src={item.imageUrl}
                    alt={item.name}
                    sizes="64px"
                    className="size-16 shrink-0 rounded-[--radius-md]"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[length:--text-body] font-medium text-ink">
                      {item.name}
                    </p>
                    <p className="tabular text-[length:--text-small] text-muted">
                      {item.quantity} &times; {formatCurrency(item.unitPrice)}
                    </p>
                  </div>
                  <p className="tabular text-[length:--text-body] font-semibold text-ink">
                    {formatCurrency(item.lineTotal)}
                  </p>
                </li>
              ))}
            </ul>

            <div className="mt-4">
              <label
                htmlFor="delivery-notes"
                className="text-[length:--text-small] font-medium text-ink"
              >
                Delivery notes
                <span className="ml-1 font-normal text-muted">(optional)</span>
              </label>
              <textarea
                id="delivery-notes"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                maxLength={500}
                rows={3}
                placeholder="Gate code, preferred time, where to leave it"
                className="mt-1.5 w-full rounded-[--radius-md] border border-line-strong bg-surface px-3 py-2 text-[16px] text-ink placeholder:text-muted sm:text-[length:--text-body]"
              />
            </div>
          </section>
        </div>

        {/* ---------------------------------------------------------- panel */}
        <div className="lg:sticky lg:top-32 lg:h-fit">
          <OrderSummary
            subtotal={cart!.subtotal}
            currency={cart!.currency}
            appliedCoupon={appliedCoupon}
            onCouponChange={setAppliedCoupon}
          >
            {failure && (
              <div
                role="alert"
                className="mb-3 rounded-[--radius-md] border border-danger/30 bg-danger-soft px-3 py-2.5 text-[length:--text-small] text-ink"
              >
                {failure}
              </div>
            )}

            <Button
              size="lg"
              block
              onClick={submit}
              loading={placeOrder.isPending}
              loadingLabel="Placing your order"
              disabled={!addressId}
            >
              Place order
            </Button>

            <p className="mt-3 text-center text-[length:--text-caption] leading-relaxed text-muted">
              By placing this order you agree that this is a demonstration system and no real
              payment will be processed.
            </p>
          </OrderSummary>
        </div>
      </div>
    </div>
  );
}
