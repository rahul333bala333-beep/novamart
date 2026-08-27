import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Field, Input } from "@/components/ui/field";

/**
 * Form accessibility.
 *
 * Every one of these is a WCAG requirement that is easy to break during a
 * refactor and invisible when testing with a mouse.
 */
describe("Field", () => {
  it("associates a visible label with its control", () => {
    render(
      <Field label="Email">
        {({ id, describedBy, invalid }) => (
          <Input id={id} aria-describedby={describedBy} invalid={invalid} />
        )}
      </Field>
    );
    // getByLabelText only finds it if the htmlFor/id wiring is correct, which is
    // the same wiring a screen reader relies on.
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
  });

  it("marks required fields for sighted and screen-reader users alike", () => {
    render(
      <Field label="Password" required>
        {({ id }) => <Input id={id} />}
      </Field>
    );
    // The asterisk alone is not announced; the visually hidden word is.
    expect(screen.getByText("(required)")).toBeInTheDocument();
  });

  it("announces an error and links it to the input", () => {
    render(
      <Field label="Email" error="That does not look like an email address">
        {({ id, describedBy, invalid }) => (
          <Input id={id} aria-describedby={describedBy} invalid={invalid} />
        )}
      </Field>
    );

    const error = screen.getByRole("alert");
    expect(error).toHaveTextContent("That does not look like an email address");

    const input = screen.getByLabelText("Email");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input.getAttribute("aria-describedby")).toContain(error.id);
  });

  it("shows the hint until an error replaces it", () => {
    const { rerender } = render(
      <Field label="Phone" hint="Used for delivery updates">
        {({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} />}
      </Field>
    );
    expect(screen.getByText("Used for delivery updates")).toBeInTheDocument();

    rerender(
      <Field label="Phone" hint="Used for delivery updates" error="Enter a contact number">
        {({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} />}
      </Field>
    );
    // Showing both at once competes for the same space and buries the error.
    expect(screen.queryByText("Used for delivery updates")).not.toBeInTheDocument();
    expect(screen.getByRole("alert")).toBeInTheDocument();
  });
});
