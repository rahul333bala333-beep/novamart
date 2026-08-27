"use client";

import * as React from "react";
import { Tag, X } from "lucide-react";
import { couponApi } from "@/lib/api/resources";
import { formatCurrency } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/field";
import { useToast } from "@/components/ui/toast";
import type { ValidateCouponResponse } from "@/lib/types";

/** Free delivery threshold. Mirrors `novamart.pricing` in order-service. */
const FREE_DELIVERY_THRESHOLD = 999;
const DELIVERY_FEE = 79;

/**
 * The money panel, used on both the bag and checkout.
 *
 * The figures shown here are computed client-side for display only. The order
 * total that is actually charged is calculated by order-service from prices it
 * reads itself, so a tampered client cannot change what it pays. If the two ever
 * disagree, the server is right.
 */
export function OrderSummary({
  subtotal,
  currency = "INR",
  deliveryFee,
  discount: externalDiscount,
  appliedCoupon,
  onCouponChange,
  children,
}: {
  subtotal: number;
  currency?: string;
  deliveryFee?: number;
  discount?: number;
  appliedCoupon?: ValidateCouponResponse | null;
  onCouponChange?: (coupon: ValidateCouponResponse | null) => void;
  children?: React.ReactNode;
}) {
  const toast = useToast();
  const [couponCode, setCouponCode] = React.useState("");
  const [validating, setValidating] = React.useState(false);
  const [internalCoupon, setInternalCoupon] = React.useState<ValidateCouponResponse | null>(null);

  const activeCoupon = appliedCoupon !== undefined ? appliedCoupon : internalCoupon;

  const discount = activeCoupon?.discountAmount ?? (externalDiscount || 0);
  const isFreeShipCoupon = activeCoupon?.discountType === "FREE_SHIPPING";
  const defaultDelivery = subtotal >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
  const delivery = isFreeShipCoupon ? 0 : (deliveryFee ?? defaultDelivery);
  const total = Math.max(0, subtotal + delivery - discount);
  const remainingForFreeDelivery = Math.max(0, FREE_DELIVERY_THRESHOLD - subtotal);

  async function handleApplyCoupon(e: React.FormEvent) {
    e.preventDefault();
    if (!couponCode.trim()) return;

    try {
      setValidating(true);
      const res = await couponApi.validate(couponCode.trim().toUpperCase(), subtotal);
      if (res.valid) {
        setInternalCoupon(res);
        onCouponChange?.(res);
        toast.success(`Coupon ${res.code} applied: ${res.message}`);
        setCouponCode("");
      } else {
        toast.error(res.message || "Invalid coupon code");
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to validate coupon";
      toast.error(message);
    } finally {
      setValidating(false);
    }
  }

  function handleRemoveCoupon() {
    setInternalCoupon(null);
    onCouponChange?.(null);
    toast.push("Coupon removed");
  }

  return (
    <section
      aria-label="Order summary"
      className="rounded-[--radius-lg] border border-line bg-surface p-5"
    >
      <h2 className="text-[length:--text-lead] font-semibold text-ink">Summary</h2>

      {/* Coupon promo form */}
      <div className="mt-4 border-b border-line pb-4">
        {activeCoupon ? (
          <div className="flex items-center justify-between rounded-[--radius-md] border border-emerald-200 bg-emerald-50/80 px-3 py-2 text-sm dark:border-emerald-900/40 dark:bg-emerald-950/20">
            <div className="flex items-center gap-2">
              <Tag className="size-4 text-emerald-600 dark:text-emerald-400" />
              <span className="font-semibold text-emerald-700 dark:text-emerald-300">
                {activeCoupon.code}
              </span>
              <span className="text-xs text-emerald-600 dark:text-emerald-400">
                ({activeCoupon.discountType === "FREE_SHIPPING" ? "Free Shipping" : `Saved ${formatCurrency(discount, currency)}`})
              </span>
            </div>
            <button
              type="button"
              onClick={handleRemoveCoupon}
              className="text-muted hover:text-danger focus:outline-none"
              aria-label="Remove coupon"
            >
              <X className="size-4" />
            </button>
          </div>
        ) : (
          <form onSubmit={handleApplyCoupon} className="flex gap-2">
            <Input
              value={couponCode}
              onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
              placeholder="Promo code (e.g. SAVE20)"
              className="uppercase font-mono text-sm"
            />
            <Button
              type="submit"
              variant="secondary"
              size="sm"
              loading={validating}
              disabled={!couponCode.trim()}
            >
              Apply
            </Button>
          </form>
        )}
      </div>

      <dl className="mt-4 flex flex-col gap-2.5 text-[length:--text-body]">
        <div className="flex justify-between">
          <dt className="text-muted">Subtotal</dt>
          <dd className="tabular text-ink">{formatCurrency(subtotal, currency)}</dd>
        </div>

        <div className="flex justify-between">
          <dt className="text-muted">Delivery</dt>
          <dd className="tabular text-ink">
            {delivery === 0 ? (
              <span className="text-success">Free</span>
            ) : (
              formatCurrency(delivery, currency)
            )}
          </dd>
        </div>

        {discount > 0 && (
          <div className="flex justify-between">
            <dt className="text-muted">Discount</dt>
            <dd className="tabular text-success">-{formatCurrency(discount, currency)}</dd>
          </div>
        )}

        <div className="mt-2 flex justify-between border-t border-line pt-3">
          <dt className="text-[length:--text-lead] font-semibold text-ink">Total</dt>
          <dd className="tabular text-[length:--text-lead] font-semibold text-ink">
            {formatCurrency(total, currency)}
          </dd>
        </div>
      </dl>

      {remainingForFreeDelivery > 0 && !isFreeShipCoupon && (
        <p className="mt-3 rounded-[--radius-md] bg-sunken px-3 py-2 text-[length:--text-caption] text-ink-soft">
          Spend {formatCurrency(remainingForFreeDelivery, currency)} more for free delivery.
        </p>
      )}

      {children && <div className="mt-5">{children}</div>}
    </section>
  );
}

