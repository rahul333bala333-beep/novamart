import { Badge } from "@/components/ui/badge";
import type { OrderStatus } from "@/lib/types";

/**
 * One mapping from order status to tone, used everywhere an order is shown.
 *
 * Defining it once means the storefront and the admin dashboard cannot end up
 * describing the same order differently, which is the kind of inconsistency that
 * makes a support conversation impossible.
 */
const TONE: Record<OrderStatus, "neutral" | "success" | "warning" | "danger" | "info"> = {
  PENDING: "warning",
  CONFIRMED: "info",
  PROCESSING: "info",
  SHIPPED: "info",
  OUT_FOR_DELIVERY: "warning",
  DELIVERED: "success",
  CANCELLED: "danger",
};

const LABEL: Record<OrderStatus, string> = {
  PENDING: "Pending",
  CONFIRMED: "Confirmed",
  PROCESSING: "Processing",
  SHIPPED: "Shipped",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <Badge tone={TONE[status]}>{LABEL[status]}</Badge>;
}

export const orderStatusLabel = (status: OrderStatus) => LABEL[status];
