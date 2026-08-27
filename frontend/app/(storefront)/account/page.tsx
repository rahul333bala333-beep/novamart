"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import * as React from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import { useToast } from "@/components/ui/toast";
import { authApi } from "@/lib/api/resources";
import { useAuth } from "@/lib/auth/auth-context";
import { formatDate } from "@/lib/format";

const schema = z.object({
  firstName: z.string().min(1, "Enter your first name").max(60),
  lastName: z.string().min(1, "Enter your last name").max(60),
  phone: z.string().max(20).optional(),
});

type FormValues = z.infer<typeof schema>;

export default function ProfilePage() {
  const { user, refresh } = useAuth();
  const toast = useToast();
  const [saving, setSaving] = React.useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    values: {
      firstName: user?.firstName ?? "",
      lastName: user?.lastName ?? "",
      phone: user?.phone ?? "",
    },
  });

  async function onSubmit(values: FormValues) {
    setSaving(true);
    try {
      await authApi.updateMe({ ...values, phone: values.phone || null });
      await refresh();
      toast.success("Profile updated");
    } catch {
      toast.error("We could not save your profile. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="max-w-xl">
      <h2 className="text-[length:--text-h3] font-semibold text-ink">Profile</h2>
      <p className="mt-1 text-[length:--text-body] text-muted">
        Member since {formatDate(user?.createdAt)}
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 flex flex-col gap-4" noValidate>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="First name" error={errors.firstName?.message} required>
            {({ id, describedBy, invalid }) => (
              <Input id={id} autoComplete="given-name" aria-describedby={describedBy} invalid={invalid} {...register("firstName")} />
            )}
          </Field>
          <Field label="Last name" error={errors.lastName?.message} required>
            {({ id, describedBy, invalid }) => (
              <Input id={id} autoComplete="family-name" aria-describedby={describedBy} invalid={invalid} {...register("lastName")} />
            )}
          </Field>
        </div>

        {/* Email is shown but not editable. Changing it would require re-verifying
            the new address, and there is no mail transport to do that with, so
            offering an editable field would be offering something that cannot
            work. */}
        <Field label="Email" hint="Contact support to change the email on your account">
          {({ id, describedBy }) => (
            <Input id={id} value={user?.email ?? ""} readOnly disabled aria-describedby={describedBy} />
          )}
        </Field>

        <Field label="Phone" hint="Used for delivery updates" error={errors.phone?.message}>
          {({ id, describedBy, invalid }) => (
            <Input id={id} type="tel" autoComplete="tel" aria-describedby={describedBy} invalid={invalid} {...register("phone")} />
          )}
        </Field>

        <div className="mt-2">
          {/* Disabled until something actually changed, so the button never
              submits an identical payload. */}
          <Button type="submit" loading={saving} disabled={!isDirty}>
            Save changes
          </Button>
        </div>
      </form>
    </div>
  );
}
