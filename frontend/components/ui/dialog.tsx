"use client";

import { X } from "lucide-react";
import * as React from "react";
import { cn } from "@/lib/cn";
import { Button } from "./button";

/**
 * A modal dialog.
 *
 * Built on the native `<dialog>` element, which brings focus trapping, the top
 * layer and Escape-to-close from the platform rather than from several hundred
 * lines of hand-written focus management that will be subtly wrong.
 *
 * The three things a modal must get right, and does here: focus moves into it on
 * open and returns on close, Escape dismisses it, and clicking the backdrop
 * dismisses it.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  className,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children?: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
}) {
  const ref = React.useRef<HTMLDialogElement>(null);

  React.useEffect(() => {
    const node = ref.current;
    if (!node) return;
    if (open && !node.open) {
      node.showModal();
      // The page behind must not scroll while a modal is up, or dismissing it
      // leaves the reader somewhere they never navigated to.
      document.body.style.overflow = "hidden";
    } else if (!open && node.open) {
      node.close();
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  return (
    <dialog
      ref={ref}
      onCancel={(event) => {
        // Escape fires `cancel`; intercepting it keeps React state and the DOM
        // in agreement about whether the dialog is open.
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        // The backdrop is the dialog element itself, so a click whose target is
        // the dialog (rather than its content) is a backdrop click.
        if (event.target === ref.current) onClose();
      }}
      aria-labelledby="dialog-title"
      className={cn(
        "m-auto w-[calc(100%-2rem)] max-w-lg rounded-[--radius-xl] border border-line bg-surface p-0 text-ink shadow-[--shadow-dialog]",
        "backdrop:bg-ink/45",
        "open:animate-in-scale",
        className
      )}
    >
      <div className="flex items-start justify-between gap-4 border-b border-line px-6 py-4">
        <div>
          <h2 id="dialog-title" className="text-[length:--text-h3] font-semibold">
            {title}
          </h2>
          {description && <p className="mt-1 text-[length:--text-body] text-muted">{description}</p>}
        </div>
        <Button variant="ghost" size="icon" onClick={onClose} aria-label="Close dialog" className="-mr-2 -mt-1 shrink-0">
          <X className="size-5" />
        </Button>
      </div>

      {children && <div className="px-6 py-5">{children}</div>}

      {footer && (
        <div className="flex flex-col-reverse gap-2 border-t border-line bg-sunken px-6 py-4 sm:flex-row sm:justify-end">
          {footer}
        </div>
      )}
    </dialog>
  );
}

/**
 * Confirmation before something irreversible.
 *
 * The action is named in the button ("Cancel order"), not labelled "OK". A
 * button that says OK forces the reader back to the prose to work out what they
 * are agreeing to.
 */
export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = "Confirm",
  loading = false,
  destructive = false,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description?: string;
  confirmLabel?: string;
  loading?: boolean;
  destructive?: boolean;
}) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={loading}>
            Keep it
          </Button>
          <Button variant={destructive ? "danger" : "primary"} onClick={onConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </>
      }
    />
  );
}
