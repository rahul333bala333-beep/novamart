import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ProductImage } from "@/components/commerce/product-image";

describe("ProductImage", () => {
  it("renders the image with its alternative text", () => {
    render(<ProductImage src="https://example.test/a.jpg" alt="Aurelia Halo Headphones" />);
    expect(screen.getByAltText("Aurelia Halo Headphones")).toBeInTheDocument();
  });

  it("falls back to a designed tile when the image cannot load", () => {
    render(<ProductImage src="https://example.test/missing.jpg" alt="Aurelia Halo Headphones" />);

    fireEvent.error(screen.getByAltText("Aurelia Halo Headphones"));

    // A broken-image icon in the middle of a product grid reads as a bug; a
    // drawn tile with the product's initials reads as intentional.
    const fallback = screen.getByRole("img", { name: /image unavailable/i });
    expect(fallback).toBeInTheDocument();
    expect(fallback).toHaveTextContent("AH");
  });

  it("lazy-loads by default and eagerly when marked priority", () => {
    const { unmount } = render(<ProductImage src="https://example.test/a.jpg" alt="A" />);
    expect(screen.getByAltText("A")).toHaveAttribute("loading", "lazy");
    unmount();

    render(<ProductImage src="https://example.test/b.jpg" alt="B" priority />);
    // Above-the-fold imagery must not be deferred, or it becomes the Largest
    // Contentful Paint and the page scores badly for it.
    expect(screen.getByAltText("B")).toHaveAttribute("loading", "eager");
  });
});
