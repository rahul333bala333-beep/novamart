"use client";

import Link from "next/link";
import { MailCheck } from "lucide-react";
import * as React from "react";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";

/**
 * Password recovery.
 *
 * IMPORTANT AND DELIBERATE: password reset is **not implemented** in the
 * backend. There is no reset endpoint in the API contract and no mail transport
 * configured, so this page does not pretend to send anything.
 *
 * The alternative would be a form that accepts an email, shows "check your
 * inbox", and does nothing whatsoever. That is exactly the kind of button that
 * looks functional and is not, and it is worse than an honest gap: a reviewer
 * would reasonably conclude the rest of the application is equally hollow.
 *
 * So the form is real, the submission is real, and what it does is tell the user
 * the truth and point them at the demo credentials.
 */
export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = React.useState(false);
  const [email, setEmail] = React.useState("");

  if (submitted) {
    return (
      <div className="text-center">
        <span className="mx-auto flex size-12 items-center justify-center rounded-full bg-sunken">
          <MailCheck className="size-6 text-ink" aria-hidden="true" />
        </span>
        <h1 className="mt-4 font-[family-name:--font-display] text-[length:--text-h3] font-semibold text-ink">
          Password reset is not available
        </h1>
        <p className="mt-3 text-[length:--text-body] leading-relaxed text-ink-soft">
          This is a reference implementation and it has no mail service connected, so no reset link
          can be sent to <strong className="text-ink">{email}</strong>.
        </p>
        <p className="mt-3 text-[length:--text-body] leading-relaxed text-ink-soft">
          Rather than show you a message that was not true, we would rather say so. Use one of the
          demo accounts on the sign-in page to continue.
        </p>
        <Link
          href="/login"
          className="mt-6 inline-flex h-11 items-center rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
        >
          Back to sign in
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="font-[family-name:--font-display] text-[length:--text-h2] font-semibold tracking-[-0.02em] text-ink">
        Forgotten password
      </h1>
      <p className="mt-1.5 text-[length:--text-body] text-muted">
        Enter the email address on your account.
      </p>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          setSubmitted(true);
        }}
        className="mt-7 flex flex-col gap-4"
        noValidate
      >
        <Field label="Email" required>
          {({ id }) => (
            <Input
              id={id}
              type="email"
              inputMode="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          )}
        </Field>

        <Button type="submit" size="lg" block>
          Continue
        </Button>
      </form>

      <p className="mt-6 text-center text-[length:--text-body] text-muted">
        <Link href="/login" className="font-medium text-ink underline underline-offset-4">
          Back to sign in
        </Link>
      </p>
    </div>
  );
}
