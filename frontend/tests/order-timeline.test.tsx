import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { OrderTimeline } from "@/components/commerce/order-timeline";
import type { OrderEvent } from "@/lib/types";

const at = (status: OrderEvent["status"]): OrderEvent => ({
  status,
  note: null,
  occurredAt: "2026-02-14T10:00:00Z",
});

describe("OrderTimeline", () => {
  it("shows the whole fulfilment path, including steps not yet reached", () => {
    render(<OrderTimeline timeline={[at("PENDING"), at("CONFIRMED")]} status="CONFIRMED" />);

    // A shopper wants to know what is coming next as much as what is done.
    expect(screen.getByText("Order placed")).toBeInTheDocument();
    expect(screen.getByText("Confirmed")).toBeInTheDocument();
    expect(screen.getByText("Shipped")).toBeInTheDocument();
    expect(screen.getByText("Delivered")).toBeInTheDocument();
  });

  it("abandons the fulfilment ladder for a cancelled order", () => {
    render(
      <OrderTimeline
        timeline={[at("PENDING"), { ...at("CANCELLED"), note: "Payment was declined" }]}
        status="CANCELLED"
      />
    );

    expect(screen.getByText("Cancelled")).toBeInTheDocument();
    expect(screen.getByText("Payment was declined")).toBeInTheDocument();
    // Showing a greyed-out "Shipped" under a cancellation would suggest the
    // parcel might still turn up.
    expect(screen.queryByText("Shipped")).not.toBeInTheDocument();
    expect(screen.queryByText("Delivered")).not.toBeInTheDocument();
  });

  it("renders as an ordered list", () => {
    render(<OrderTimeline timeline={[at("PENDING")]} status="PENDING" />);
    expect(screen.getByRole("list")).toBeInTheDocument();
  });
});
