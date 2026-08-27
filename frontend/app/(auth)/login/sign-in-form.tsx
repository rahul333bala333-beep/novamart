"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import * as React from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/auth-context";
import { Eye, EyeOff } from "lucide-react";

const schema = z.object({
  email: z.string().min(1, "Enter your email").email("That does not look like an email address"),
  password: z.string().min(1, "Enter your password"),
});

type FormValues = z.infer<typeof schema>;

export function SignInForm() {
  const router = useRouter();
  const params = useSearchParams();
  const { signIn } = useAuth();
  const [formError, setFormError] = React.useState<string | null>(null);
  const [showPassword, setShowPassword] = React.useState(false);

  const next = params.get("next") ?? "/";

  const {
    register,
    handleSubmit,
    setFocus,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), mode: "onBlur" });

  async function onSubmit(values: FormValues) {
    setFormError(null);
    try {
      await signIn(values.email, values.password);
      router.push(next);
      router.refresh();
    } catch (error) {
      // The backend answers "incorrect email or password" for both a wrong
      // password and an unknown account, which is what stops this form being
      // used to discover which addresses are registered. It is repeated here
      // verbatim rather than being made more specific.
      setFormError(
        error instanceof ApiError ? error.message : "We could not sign you in. Please try again."
      );
      setFocus("password");
    }
  }

  return (
    <div>
      <h1 className="font-[family-name:--font-display] text-[length:--text-h2] font-semibold tracking-[-0.02em] text-ink">
        Sign in
      </h1>
      <p className="mt-1.5 text-[length:--text-body] text-muted">
        Welcome back. Your bag is waiting.
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

        <Field label="Email" error={errors.email?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="email"
              inputMode="email"
              autoComplete="email"
              autoFocus
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("email")}
            />
          )}
        </Field>

        <Field label="Password" error={errors.password?.message} required>
          {({ id, describedBy, invalid }) => (
            <div className="relative">
              <Input
                id={id}
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                aria-describedby={describedBy}
                invalid={invalid}
                className="pr-11"
                {...register("password")}
              />
              {/* Reveal toggle. Typing a password blind on a phone keyboard is
                  the single biggest cause of failed sign-ins. */}
              <button
                type="button"
                onClick={() => setShowPassword((shown) => !shown)}
                className="absolute right-1 top-1/2 flex size-9 -translate-y-1/2 cursor-pointer items-center justify-center rounded-[--radius-sm] text-muted transition-colors hover:text-ink"
                aria-label={showPassword ? "Hide password" : "Show password"}
                aria-pressed={showPassword}
              >
                {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
          )}
        </Field>

        <div className="flex justify-end">
          <Link
            href="/forgot-password"
            className="text-[length:--text-small] text-muted underline underline-offset-4 transition-colors hover:text-ink"
          >
            Forgotten your password?
          </Link>
        </div>

        <Button type="submit" size="lg" block loading={isSubmitting} loadingLabel="Signing in">
          Sign in
        </Button>
      </form>

      <p className="mt-6 text-center text-[length:--text-body] text-muted">
        New to Nova Mart?{" "}
        <Link
          href={`/register${next !== "/" ? `?next=${encodeURIComponent(next)}` : ""}`}
          className="font-medium text-ink underline underline-offset-4"
        >
          Create an account
        </Link>
      </p>

      {/* Demo credentials belong on the page for a reference implementation that
          is meant to be opened and tried. They are the seeded accounts, and the
          README says the same thing. */}
      <div className="mt-8 rounded-[--radius-lg] border border-dashed border-line-strong bg-sunken px-4 py-3">
        <p className="text-[length:--text-caption] font-semibold uppercase tracking-[0.08em] text-muted">
          Demo accounts
        </p>
        <dl className="mt-2 flex flex-col gap-1 text-[length:--text-small] text-ink-soft">
          <div className="flex justify-between gap-3">
            <dt>Shopper</dt>
            <dd className="tabular">demo@novamart.dev / Demo@12345</dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt>Administrator</dt>
            <dd className="tabular">admin@novamart.dev / Admin@12345</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}
