import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Button } from "@/components/ui/button";

describe("Button", () => {
  it("calls its handler when clicked", async () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Add to bag</Button>);

    await userEvent.click(screen.getByRole("button", { name: "Add to bag" }));
    expect(onClick).toHaveBeenCalledOnce();
  });

  it("blocks further clicks while loading", async () => {
    const onClick = vi.fn();
    render(
      <Button onClick={onClick} loading>
        Place order
      </Button>
    );

    // This is what actually prevents a double submission. The spinner only
    // explains to the user why nothing is happening.
    await userEvent.click(screen.getByRole("button"));
    expect(onClick).not.toHaveBeenCalled();
    expect(screen.getByRole("button")).toBeDisabled();
  });

  it("announces the loading state to screen readers", () => {
    render(<Button loading loadingLabel="Placing your order">Place order</Button>);
    expect(screen.getByRole("button")).toHaveAttribute("aria-busy", "true");
    expect(screen.getByText("Placing your order")).toBeInTheDocument();
  });

  it("does not fire when disabled", async () => {
    const onClick = vi.fn();
    render(
      <Button onClick={onClick} disabled>
        Buy now
      </Button>
    );
    await userEvent.click(screen.getByRole("button"));
    expect(onClick).not.toHaveBeenCalled();
  });
});
