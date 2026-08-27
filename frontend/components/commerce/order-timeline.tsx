import { Check, CircleDashed, XCircle } from "lucide-react";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/cn";
import type { OrderEvent, OrderStatus } from "@/lib/types";

const FULFILMENT: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "PROCESSING",
  "SHIPPED",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
];

const COPY: Record<OrderStatus, { title: string; body: string }> = {
  PENDING: { title: "Order placed", body: "We have received your order." },
  CONFIRMED: { title: "Confirmed", body: "Payment settled and stock allocated." },
  PROCESSING: { title: "Processing", body: "Your order is being picked and packed." },
  SHIPPED: { title: "Shipped", body: "Your order is in transit." },
  OUT_FOR_DELIVERY: { title: "Out for delivery", body: "Your courier is on the way." },
  DELIVERED: { title: "Delivered", body: "Your order has arrived." },
  CANCELLED: { title: "Cancelled", body: "This order was cancelled." },
};

/**
 * Order progress.
 *
 * Renders the full fulfilment path with the steps not yet reached shown as
 * pending, rather than only listing what has happened. A shopper wants to know
 * what is coming next as much as what is done.
 *
 * A cancelled order abandons the ladder entirely: showing "Shipped" greyed out
 * beneath a cancellation would suggest it might still arrive.
 */
export function OrderTimeline({
  timeline,
  status,
}: {
  timeline: OrderEvent[];
  status: OrderStatus;
}) {
  const occurred = new Map(timeline.map((event) => [event.status, event]));

  if (status === "CANCELLED") {
    const cancelledAt = occurred.get("CANCELLED");
    return (
      <ol className="flex flex-col">
        {timeline
          .filter((event) => event.status !== "CANCELLED")
          .map((event) => (
            <Step
              key={event.status}
              state="done"
              title={COPY[event.status].title}
              body={COPY[event.status].body}
              at={event.occurredAt}
            />
          ))}
        <Step
          state="cancelled"
          title="Cancelled"
          body={cancelledAt?.note ?? COPY.CANCELLED.body}
          at={cancelledAt?.occurredAt}
          last
        />
      </ol>
    );
  }

  const currentIndex = FULFILMENT.indexOf(status);

  return (
    <ol className="flex flex-col">
      {FULFILMENT.map((step, index) => {
        const event = occurred.get(step);
        const state = index <= currentIndex ? "done" : "upcoming";
        return (
          <Step
            key={step}
            state={state}
            title={COPY[step].title}
            body={event?.note ?? COPY[step].body}
            at={event?.occurredAt}
            last={index === FULFILMENT.length - 1}
          />
        );
      })}
    </ol>
  );
}

function Step({
  state,
  title,
  body,
  at,
  last = false,
}: {
  state: "done" | "upcoming" | "cancelled";
  title: string;
  body: string;
  at?: string;
  last?: boolean;
}) {
  return (
    <li className="flex gap-3">
      <div className="flex flex-col items-center">
        <span
          className={cn(
            "flex size-7 shrink-0 items-center justify-center rounded-full border",
            state === "done" && "border-ink bg-ink text-white",
            state === "upcoming" && "border-line-strong bg-surface text-line-strong",
            state === "cancelled" && "border-danger bg-danger text-white"
          )}
          aria-hidden="true"
        >
          {state === "done" && <Check className="size-3.5" />}
          {state === "upcoming" && <CircleDashed className="size-3.5" />}
          {state === "cancelled" && <XCircle className="size-3.5" />}
        </span>
        {!last && (
          <span
            className={cn("w-px flex-1", state === "done" ? "bg-ink/25" : "bg-line")}
            aria-hidden="true"
          />
        )}
      </div>

      <div className={cn("pb-6", last && "pb-0")}>
        <p
          className={cn(
            "text-[length:--text-body] font-medium",
            state === "upcoming" ? "text-muted" : "text-ink"
          )}
        >
          {title}
        </p>
        <p className="mt-0.5 text-[length:--text-small] text-muted">{body}</p>
        {at && <p className="mt-0.5 text-[length:--text-caption] text-muted">{formatDateTime(at)}</p>}
      </div>
    </li>
  );
}
