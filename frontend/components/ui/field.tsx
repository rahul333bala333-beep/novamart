"use client";

import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * Form field primitives.
 *
 * Every input gets a real, visible `<label>` wired by `htmlFor`. Placeholder-as-
 * label is the most common accessibility failure in modern forms: the label
 * disappears the moment typing starts, so anyone who is interrupted has no way
 * to recover what the field was for, and screen readers may not announce it.
 */

/**
 * A stable id for wiring a label to its control.
 *
 * `React.useId` rather than a module-level counter. A counter produces different
 * values on the server and on the client, because each starts at zero and the
 * render order differs, so `htmlFor` and `id` disagree after hydration. React
 * warns about the mismatch and, worse, the label stops pointing at its input,
 * which is an accessibility regression that is invisible unless you are using a
 * screen reader. `useId` is generated identically on both sides by design.
 */
function useFieldId(provided?: string) {
  const generated = React.useId();
  return provided ?? generated;
}

interface FieldProps {
  label: string;
  htmlFor?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: (props: { id: string; describedBy?: string; invalid: boolean }) => React.ReactNode;
}

export function Field({ label, htmlFor, hint, error, required, className, children }: FieldProps) {
  const id = useFieldId(htmlFor);
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <label htmlFor={id} className="text-[length:--text-small] font-medium text-ink">
        {label}
        {required && (
          <span className="ml-1 text-danger" aria-hidden="true">
            *
          </span>
        )}
        {required && <span className="sr-only"> (required)</span>}
      </label>

      {children({ id, describedBy, invalid: Boolean(error) })}

      {/* Hint stays visible rather than living in the placeholder, so it is still
          there while the field is being filled in. */}
      {hint && !error && (
        <p id={hintId} className="text-[length:--text-caption] text-muted">
          {hint}
        </p>
      )}

      {/* The error sits directly under its own field, not in a summary at the top
          of the form where the user has to work out which input it refers to.
          role="alert" announces it the moment it appears. */}
      {error && (
        <p id={errorId} role="alert" className="text-[length:--text-caption] text-danger">
          {error}
        </p>
      )}
    </div>
  );
}

const controlBase = [
  "w-full bg-surface text-ink placeholder:text-muted",
  "border border-line-strong rounded-[--radius-md]",
  "transition-colors duration-[--duration-fast]",
  "hover:border-muted",
  "disabled:bg-sunken disabled:text-muted disabled:cursor-not-allowed",
  // 16px on mobile. Anything smaller makes iOS Safari zoom the viewport on
  // focus, which yanks the layout sideways mid-form.
  "text-[16px] sm:text-[length:--text-body]",
].join(" ");

export const Input = React.forwardRef<
  HTMLInputElement,
  React.InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }
>(function Input({ className, invalid, ...props }, ref) {
  return (
    <input
      ref={ref}
      className={cn(controlBase, "h-11 px-3", invalid && "border-danger", className)}
      aria-invalid={invalid || undefined}
      {...props}
    />
  );
});

export const Textarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }
>(function Textarea({ className, invalid, ...props }, ref) {
  return (
    <textarea
      ref={ref}
      className={cn(controlBase, "min-h-24 px-3 py-2 leading-relaxed", invalid && "border-danger", className)}
      aria-invalid={invalid || undefined}
      {...props}
    />
  );
});

export const Select = React.forwardRef<
  HTMLSelectElement,
  React.SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }
>(function Select({ className, invalid, children, ...props }, ref) {
  return (
    <select
      ref={ref}
      className={cn(controlBase, "h-11 px-3 pr-8 cursor-pointer", invalid && "border-danger", className)}
      aria-invalid={invalid || undefined}
      {...props}
    >
      {children}
    </select>
  );
});
