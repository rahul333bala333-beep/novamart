import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { OrderSummary } from "@/components/commerce/order-summary";

/**
 * The money panel.
 *
 * These are the figures a shopper checks before committing, so the arithmetic
 * and the free-delivery threshold are asserted directly rather than eyeballed.
 */
describe("OrderSummary", () => {
  it("charges delivery below the threshold", () => {
    render(<OrderSummary subtotal={500} />);
    expect(screen.getByText("₹500")).toBeInTheDocument();
    expect(screen.getByText("₹79")).toBeInTheDocument();
    expect(screen.getByText("₹579")).toBeInTheDocument();
  });

  it("waives delivery at the threshold exactly", () => {
    // Boundary: 999 must qualify, not 1000. Off-by-one here is a support ticket.
    render(<OrderSummary subtotal={999} />);
    expect(screen.getByText("Free")).toBeInTheDocument();
    // Subtotal and total are both ₹999 once delivery is waived, so this asserts
    // on both occurrences rather than requiring the text to be unique.
    expect(screen.getAllByText("₹999")).toHaveLength(2);
  });

  it("nudges towards free delivery with the exact shortfall", () => {
    render(<OrderSummary subtotal={800} />);
    expect(screen.getByText(/Spend ₹199 more for free delivery/)).toBeInTheDocument();
  });

  it("hides the nudge once it no longer applies", () => {
    render(<OrderSummary subtotal={1500} />);
    expect(screen.queryByText(/more for free delivery/)).not.toBeInTheDocument();
  });

  it("omits the discount row entirely when there is no discount", () => {
    // A permanent "Discount: ₹0" line advertises a promotion that does not exist.
    render(<OrderSummary subtotal={2000} />);
    expect(screen.queryByText("Discount")).not.toBeInTheDocument();
  });

  it("subtracts a discount when one applies", () => {
    render(<OrderSummary subtotal={2000} discount={300} />);
    expect(screen.getByText("Discount")).toBeInTheDocument();
    expect(screen.getByText("-₹300")).toBeInTheDocument();
    expect(screen.getByText("₹1,700")).toBeInTheDocument();
  });

  it("is labelled for assistive technology", () => {
    render(<OrderSummary subtotal={1000} />);
    expect(screen.getByRole("region", { name: "Order summary" })).toBeInTheDocument();
  });
});
