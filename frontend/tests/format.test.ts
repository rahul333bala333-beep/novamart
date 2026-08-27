import { describe, expect, it } from "vitest";
import {
  formatCurrency,
  formatCurrencyExact,
  formatDate,
  formatNumber,
  initialsOf,
} from "@/lib/format";

/**
 * Money and dates as the shopper reads them.
 *
 * Worth testing because Indian numbering does not group in thousands, and
 * getting 1,00,000 wrong on a storefront priced in rupees is immediately visible
 * to exactly the audience being sold to.
 */
describe("formatCurrency", () => {
  it("groups in the Indian system rather than in thousands", () => {
    // 100000 is one lakh: 1,00,000 and not 100,000.
    expect(formatCurrency(100000)).toBe("₹1,00,000");
    expect(formatCurrency(134999)).toBe("₹1,34,999");
  });

  it("drops paise on catalogue prices", () => {
    expect(formatCurrency(6499)).toBe("₹6,499");
    expect(formatCurrency(6499.4)).toBe("₹6,499");
  });

  it("renders zero rather than an empty string", () => {
    expect(formatCurrency(0)).toBe("₹0");
  });
});

describe("formatCurrencyExact", () => {
  it("keeps paise only when they are non-zero", () => {
    expect(formatCurrencyExact(1200)).toBe("₹1,200");
    expect(formatCurrencyExact(1200.5)).toBe("₹1,200.50");
  });
});

describe("formatNumber", () => {
  it("groups counts the same way as prices", () => {
    expect(formatNumber(2143)).toBe("2,143");
    expect(formatNumber(1200000)).toBe("12,00,000");
  });
});

describe("formatDate", () => {
  it("renders an ISO instant as a readable date", () => {
    expect(formatDate("2026-02-14T09:31:08.442Z")).toContain("2026");
  });

  it("returns a dash rather than 'Invalid Date' for missing values", () => {
    // These fields are genuinely nullable in the API (an order that has not
    // shipped has no delivery date), so this is a real code path.
    expect(formatDate(null)).toBe("-");
    expect(formatDate(undefined)).toBe("-");
  });
});

describe("initialsOf", () => {
  it("builds initials from a name", () => {
    expect(initialsOf("Ananya", "Iyer")).toBe("AI");
  });

  it("degrades rather than throwing on missing names", () => {
    expect(initialsOf(undefined, undefined)).toBe("?");
    expect(initialsOf("Ananya", undefined)).toBe("A");
  });
});
