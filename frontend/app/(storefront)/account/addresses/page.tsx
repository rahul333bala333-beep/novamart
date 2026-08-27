"use client";

import { MapPin, Pencil, Plus, Trash2 } from "lucide-react";
import * as React from "react";
import { AddressForm } from "@/components/commerce/address-form";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog, Dialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { useAddresses, useAddressMutations } from "@/lib/hooks/use-addresses";
import type { Address } from "@/lib/types";

export default function AddressesPage() {
  const { data, isLoading, isError, refetch } = useAddresses();
  const { create, update, remove } = useAddressMutations();
  const toast = useToast();

  const [editing, setEditing] = React.useState<Address | null>(null);
  const [creating, setCreating] = React.useState(false);
  const [deleting, setDeleting] = React.useState<Address | null>(null);

  if (isError) {
    return <ErrorState description="We could not load your addresses." onRetry={() => refetch()} />;
  }

  return (
    <div>
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="text-[length:--text-h3] font-semibold text-ink">Addresses</h2>
          <p className="mt-1 text-[length:--text-body] text-muted">
            Where we deliver your orders.
          </p>
        </div>
        <Button onClick={() => setCreating(true)}>
          <Plus className="size-4" aria-hidden="true" />
          Add
        </Button>
      </div>

      {isLoading ? (
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-40 w-full" />
        </div>
      ) : (data?.length ?? 0) === 0 ? (
        <div className="mt-6">
          <EmptyState
            icon={MapPin}
            title="No saved addresses"
            description="Add an address to make checkout quicker."
            action={<Button onClick={() => setCreating(true)}>Add an address</Button>}
          />
        </div>
      ) : (
        <ul className="mt-6 grid gap-4 sm:grid-cols-2">
          {data!.map((address) => (
            <li
              key={address.id}
              className="flex flex-col rounded-[--radius-lg] border border-line bg-surface p-4"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="text-[length:--text-body] font-medium text-ink">{address.label}</span>
                {address.isDefault && <Badge tone="neutral">Default</Badge>}
              </div>

              <address className="mt-2 not-italic text-[length:--text-small] leading-relaxed text-muted">
                {address.recipientName}
                <br />
                {address.line1}
                {address.line2 ? `, ${address.line2}` : ""}
                <br />
                {address.city}, {address.state} {address.postalCode}
                <br />
                {address.country}
                <br />
                {address.phone}
              </address>

              <div className="mt-auto flex gap-2 pt-4">
                <Button variant="secondary" size="sm" onClick={() => setEditing(address)}>
                  <Pencil className="size-3.5" aria-hidden="true" />
                  Edit
                </Button>
                <Button variant="ghost" size="sm" onClick={() => setDeleting(address)}>
                  <Trash2 className="size-3.5" aria-hidden="true" />
                  Delete
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Dialog
        open={creating}
        onClose={() => setCreating(false)}
        title="Add an address"
        className="max-w-2xl"
      >
        <AddressForm
          submitting={create.isPending}
          onCancel={() => setCreating(false)}
          onSubmit={(values) =>
            create.mutate(values, {
              onSuccess: () => {
                setCreating(false);
                toast.success("Address saved");
              },
              onError: () => toast.error("We could not save that address."),
            })
          }
        />
      </Dialog>

      <Dialog
        open={Boolean(editing)}
        onClose={() => setEditing(null)}
        title="Edit address"
        className="max-w-2xl"
      >
        {editing && (
          <AddressForm
            initial={editing}
            submitting={update.isPending}
            onCancel={() => setEditing(null)}
            onSubmit={(values) =>
              update.mutate(
                { id: editing.id, input: values },
                {
                  onSuccess: () => {
                    setEditing(null);
                    toast.success("Address updated");
                  },
                  onError: () => toast.error("We could not update that address."),
                }
              )
            }
          />
        )}
      </Dialog>

      <ConfirmDialog
        open={Boolean(deleting)}
        onClose={() => setDeleting(null)}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => {
              setDeleting(null);
              toast.success("Address deleted");
            },
            onError: () => toast.error("We could not delete that address."),
          })
        }
        title="Delete this address?"
        description={deleting ? `"${deleting.label}" will be removed from your account.` : undefined}
        confirmLabel="Delete address"
        loading={remove.isPending}
        destructive
      />
    </div>
  );
}
