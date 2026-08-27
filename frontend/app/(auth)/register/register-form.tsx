"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check, X } from "lucide-react";
import * as React from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/auth-context";
import { cn } from "@/lib/cn";

/**
 * The password rule mirrors the server's `@Size(min = 8)`. Client rules are for
 * feedback; the server's are the ones that hold.
 */
const schema = z.object({
  firstName: z.string().min(1, "Enter your first name").max(60),
  lastName: z.string().min(1, "Enter your last name").max(60),
  email: z.string().min(1, "Enter your email").email("That does not look like an email address").max(180),
  password: z.string().min(8, "Use at least 8 characters").max(100),
  phone: z.string().max(20).optional(),
});

type FormValues = z.infer<typeof schema>;

export function RegisterForm() {
  const router = useRouter();
  const params = useSearchParams();
  const { register: createAccount } = useAuth();
  const [formError, setFormError] = React.useState<string | null>(null);

  const next = params.get("next") ?? "/";

  const {
    register,
    handleSubmit,
    control,
    setFocus,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), mode: "onBlur" });

  // `useWatch` rather than `watch()`. The latter returns a fresh function on
  // every render, which the React Compiler cannot memoize safely, so it bails
  // out of optimising this component entirely. `useWatch` is a proper
  // subscription and re-renders only this value.
  const password = useWatch({ control, name: "password" }) ?? "";
  const requirements = [
    { label: "At least 8 characters", met: password.length >= 8 },
    { label: "Contains a number", met: /\d/.test(password) },
    { label: "Contains a letter", met: /[a-zA-Z]/.test(password) },
  ];

  async function onSubmit(values: FormValues) {
    setFormError(null);
    try {
      await createAccount(values);
      router.push(next);
      router.refresh();
    } catch (error) {
      if (error instanceof ApiError && error.code === "EMAIL_ALREADY_EXISTS") {
        setFormError("An account with this email already exists. Try signing in instead.");
        setFocus("email");
        return;
      }
      setFormError(
        error instanceof ApiError ? error.message : "We could not create your account."
      );
    }
  }

  return (
    <div>
      <h1 className="font-[family-name:--font-display] text-[length:--text-h2] font-semibold tracking-[-0.02em] text-ink">
        Create an account
      </h1>
      <p className="mt-1.5 text-[length:--text-body] text-muted">
        It takes a moment and saves your bag across devices.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-7 flex flex-col gap-4" noValidate>
        {formError && (
          <div
            role="alert"
            className="rounded-[--radius-md] border border-danger/30 bg-danger-soft px-3 py-2.5 text-[length:--text-small] text-ink"
          >
            {formError}
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="First name" error={errors.firstName?.message} required>
            {({ id, describedBy, invalid }) => (
              <Input
                id={id}
                autoComplete="given-name"
                autoFocus
                aria-describedby={describedBy}
                invalid={invalid}
                {...register("firstName")}
              />
            )}
          </Field>

          <Field label="Last name" error={errors.lastName?.message} required>
            {({ id, describedBy, invalid }) => (
              <Input
                id={id}
                autoComplete="family-name"
                aria-describedby={describedBy}
                invalid={invalid}
                {...register("lastName")}
              />
            )}
          </Field>
        </div>

        <Field label="Email" error={errors.email?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="email"
              inputMode="email"
              autoComplete="email"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("email")}
            />
          )}
        </Field>

        <Field label="Phone" hint="Optional, used for delivery updates" error={errors.phone?.message}>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("phone")}
            />
          )}
        </Field>

        <Field label="Password" error={errors.password?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="password"
              autoComplete="new-password"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("password")}
            />
          )}
        </Field>

        {/* Requirements shown as they are met, rather than only failing after
            submission. The user should never have to guess what the rule was. */}
        <ul className="-mt-1 flex flex-col gap-1">
          {requirements.map((requirement) => (
            <li
              key={requirement.label}
              className={cn(
                "flex items-center gap-1.5 text-[length:--text-caption]",
                requirement.met ? "text-success" : "text-muted"
              )}
            >
              {requirement.met ? (
                <Check className="size-3.5" aria-hidden="true" />
              ) : (
                <X className="size-3.5" aria-hidden="true" />
              )}
              {requirement.label}
            </li>
          ))}
        </ul>

        <Button type="submit" size="lg" block loading={isSubmitting} loadingLabel="Creating account">
          Create account
        </Button>
      </form>

      <p className="mt-6 text-center text-[length:--text-body] text-muted">
        Already have an account?{" "}
        <Link
          href={`/login${next !== "/" ? `?next=${encodeURIComponent(next)}` : ""}`}
          className="font-medium text-ink underline underline-offset-4"
        >
          Sign in
        </Link>
      </p>
    </div>
  );
}
