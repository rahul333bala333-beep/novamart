"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import type { Address, AddressInput } from "@/lib/types";

/**
 * Validation runs on the client for immediate feedback and again on the server,
 * which is the only side that matters. Client-side rules are a convenience, never
 * a control: anything enforced only here can be bypassed with a terminal.
 */
const schema = z.object({
  label: z.string().min(1, "Give this address a name, like Home or Office").max(40),
  recipientName: z.string().min(1, "Who should receive this?").max(120),
  phone: z
    .string()
    .min(6, "Enter a contact number")
    .max(20)
    .regex(/^[0-9+\-\s()]+$/, "Use digits, spaces and + only"),
  line1: z.string().min(1, "Enter the street address").max(200),
  line2: z.string().max(200).optional(),
  city: z.string().min(1, "Enter a city").max(80),
  state: z.string().min(1, "Enter a state").max(80),
  postalCode: z.string().min(4, "Enter a postal code").max(16),
  country: z.string().min(1, "Enter a country").max(80),
  isDefault: z.boolean().optional(),
});

type FormValues = z.infer<typeof schema>;

export function AddressForm({
  initial,
  submitting,
  onSubmit,
  onCancel,
  submitLabel = "Save address",
}: {
  initial?: Address;
  submitting?: boolean;
  onSubmit: (values: AddressInput) => void;
  onCancel?: () => void;
  submitLabel?: string;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    // Validating on blur rather than on every keystroke. Showing "this is
    // invalid" while someone is still halfway through typing their postcode is
    // both wrong and irritating.
    mode: "onBlur",
    defaultValues: {
      label: initial?.label ?? "Home",
      recipientName: initial?.recipientName ?? "",
      phone: initial?.phone ?? "",
      line1: initial?.line1 ?? "",
      line2: initial?.line2 ?? "",
      city: initial?.city ?? "",
      state: initial?.state ?? "",
      postalCode: initial?.postalCode ?? "",
      country: initial?.country ?? "India",
      isDefault: initial?.isDefault ?? false,
    },
  });

  return (
    <form
      onSubmit={handleSubmit((values) =>
        onSubmit({ ...values, line2: values.line2 || null, isDefault: values.isDefault ?? false })
      )}
      className="flex flex-col gap-4"
      noValidate
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Address name" error={errors.label?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} {...register("label")} />
          )}
        </Field>

        <Field label="Recipient" error={errors.recipientName?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              autoComplete="name"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("recipientName")}
            />
          )}
        </Field>
      </div>

      <Field label="Phone" error={errors.phone?.message} required>
        {({ id, describedBy, invalid }) => (
          <Input
            id={id}
            // type=tel brings up the numeric keypad on a phone instead of a full
            // QWERTY keyboard.
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            aria-describedby={describedBy}
            invalid={invalid}
            {...register("phone")}
          />
        )}
      </Field>

      <Field label="Address line 1" error={errors.line1?.message} required>
        {({ id, describedBy, invalid }) => (
          <Input
            id={id}
            autoComplete="address-line1"
            aria-describedby={describedBy}
            invalid={invalid}
            {...register("line1")}
          />
        )}
      </Field>

      <Field label="Address line 2" hint="Optional" error={errors.line2?.message}>
        {({ id, describedBy, invalid }) => (
          <Input
            id={id}
            autoComplete="address-line2"
            aria-describedby={describedBy}
            invalid={invalid}
            {...register("line2")}
          />
        )}
      </Field>

      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="City" error={errors.city?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              autoComplete="address-level2"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("city")}
            />
          )}
        </Field>

        <Field label="State" error={errors.state?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              autoComplete="address-level1"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("state")}
            />
          )}
        </Field>

        <Field label="Postal code" error={errors.postalCode?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              inputMode="numeric"
              autoComplete="postal-code"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("postalCode")}
            />
          )}
        </Field>
      </div>

      <Field label="Country" error={errors.country?.message} required>
        {({ id, describedBy, invalid }) => (
          <Input
            id={id}
            autoComplete="country-name"
            aria-describedby={describedBy}
            invalid={invalid}
            {...register("country")}
          />
        )}
      </Field>

      <label className="flex cursor-pointer items-center gap-2 text-[length:--text-body] text-ink">
        <input
          type="checkbox"
          {...register("isDefault")}
          className="size-4 cursor-pointer rounded-[--radius-sm] border-line-strong accent-[--color-ink]"
        />
        Use this as my default delivery address
      </label>

      <div className="mt-2 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        {onCancel && (
          <Button type="button" variant="secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </Button>
        )}
        <Button type="submit" loading={submitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
