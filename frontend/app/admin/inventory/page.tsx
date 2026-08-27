"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Warehouse } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { DataTable, type Column } from "@/components/admin/data-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Field, Input } from "@/components/ui/field";
import { Pagination } from "@/components/ui/pagination";
import { useToast } from "@/components/ui/toast";
import { ApiError } from "@/lib/api/client";
import { catalogueApi, inventoryApi } from "@/lib/api/resources";
import type { InventoryItem } from "@/lib/types";

/**
 * Stock management.
 *
 * The table shows total, reserved and available side by side, because the
 * difference between them is the thing an operator needs to understand: units
 * that are physically present but already promised to an in-flight order are not
 * sellable, and a single "quantity" column hides that entirely.
 */
export default function AdminInventoryPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [page, setPage] = React.useState(0);
  const [lowOnly, setLowOnly] = React.useState(false);
  const [editing, setEditing] = React.useState<InventoryItem | null>(null);
  const [quantity, setQuantity] = React.useState("");
  const [threshold, setThreshold] = React.useState("");

  const stock = useQuery({
    queryKey: ["inventory", { page, lowOnly }],
    queryFn: () => inventoryApi.list({ page, size: 20, lowStockOnly: lowOnly }),
    placeholderData: (previous) => previous,
  });

  // Inventory is keyed by product id and knows nothing about names, so the
  // catalogue is fetched alongside it purely to label the rows. Two calls the
  // browser composes, rather than a join the services are not allowed to make.
  const catalogue = useQuery({
    queryKey: ["products", "all-for-inventory"],
    queryFn: () => catalogueApi.products({ size: 100 }),
    staleTime: 5 * 60_000,
  });

  const nameOf = React.useMemo(() => {
    const map = new Map<string, { name: string; sku: string }>();
    for (const product of catalogue.data?.content ?? []) {
      map.set(product.id, { name: product.name, sku: product.sku });
    }
    return map;
  }, [catalogue.data]);

  const save = useMutation({
    mutationFn: ({ productId, total, reorder }: { productId: string; total: number; reorder: number }) =>
      inventoryApi.update(productId, total, reorder),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["inventory"] });
      setEditing(null);
      toast.success("Stock updated");
    },
    onError: (error) =>
      toast.error(error instanceof ApiError ? error.message : "We could not update stock."),
  });

  const columns: Column<InventoryItem>[] = [
    {
      key: "product",
      header: "Product",
      cell: (item) => {
        const info = nameOf.get(item.productId);
        return (
          <div className="min-w-0">
            <p className="truncate font-medium text-ink">{info?.name ?? "Unknown product"}</p>
            <p className="tabular text-[length:--text-caption] text-muted">
              {info?.sku ?? item.productId.slice(0, 8)}
            </p>
          </div>
        );
      },
    },
    {
      key: "total",
      header: "On hand",
      align: "right",
      cell: (item) => <span className="tabular">{item.totalQuantity}</span>,
    },
    {
      key: "reserved",
      header: "Reserved",
      align: "right",
      cell: (item) => <span className="tabular text-muted">{item.reservedQuantity}</span>,
    },
    {
      key: "available",
      header: "Available",
      align: "right",
      cell: (item) => <span className="tabular font-medium">{item.availableQuantity}</span>,
    },
    {
      key: "status",
      header: "Status",
      cell: (item) =>
        !item.inStock ? (
          <Badge tone="danger">Out of stock</Badge>
        ) : item.lowStock ? (
          <Badge tone="warning">Low</Badge>
        ) : (
          <Badge tone="success">In stock</Badge>
        ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      cell: (item) => (
        <Button
          size="sm"
          variant="secondary"
          onClick={(event) => {
            event.stopPropagation();
            setEditing(item);
            setQuantity(String(item.totalQuantity));
            setThreshold(String(item.reorderThreshold));
          }}
        >
          Adjust
        </Button>
      ),
    },
  ];

  if (stock.isError) {
    return <ErrorState description="We could not load stock records." onRetry={() => stock.refetch()} />;
  }

  return (
    <div>
      <AdminPageHeader
        title="Inventory"
        description="On-hand stock, live reservations and reorder thresholds."
        action={
          <Button
            variant={lowOnly ? "primary" : "secondary"}
            onClick={() => {
              setLowOnly((value) => !value);
              setPage(0);
            }}
            aria-pressed={lowOnly}
          >
            {lowOnly ? "Showing low stock" : "Show low stock only"}
          </Button>
        }
      />

      <DataTable
        caption="Stock records"
        columns={columns}
        rows={stock.data?.content ?? []}
        keyOf={(item) => item.productId}
        loading={stock.isLoading}
        emptyState={
          <EmptyState
            icon={Warehouse}
            title={lowOnly ? "Nothing is low on stock" : "No stock records"}
            description={
              lowOnly
                ? "Every product is above its reorder threshold."
                : "Stock records are created when a product is added."
            }
          />
        }
      />

      {stock.data && <Pagination meta={stock.data.page} onPageChange={setPage} className="mt-6" />}

      <Dialog
        open={Boolean(editing)}
        onClose={() => setEditing(null)}
        title="Adjust stock"
        description={editing ? nameOf.get(editing.productId)?.name : undefined}
        footer={
          <>
            <Button variant="secondary" onClick={() => setEditing(null)} disabled={save.isPending}>
              Cancel
            </Button>
            <Button
              loading={save.isPending}
              onClick={() =>
                editing &&
                save.mutate({
                  productId: editing.productId,
                  total: Number(quantity),
                  reorder: Number(threshold),
                })
              }
            >
              Save
            </Button>
          </>
        }
      >
        {editing && (
          <div className="flex flex-col gap-4">
            {/* Says out loud why the new total cannot go below the reserved
                count, rather than letting the server reject it with an error the
                operator has to interpret. */}
            {editing.reservedQuantity > 0 && (
              <p className="rounded-[--radius-md] bg-sunken px-3 py-2 text-[length:--text-small] text-ink-soft">
                {editing.reservedQuantity} unit{editing.reservedQuantity === 1 ? " is" : "s are"}{" "}
                reserved for orders in progress, so the total cannot be set below that.
              </p>
            )}

            <Field label="Units on hand" required>
              {({ id }) => (
                <Input
                  id={id}
                  type="number"
                  inputMode="numeric"
                  min={editing.reservedQuantity}
                  value={quantity}
                  onChange={(event) => setQuantity(event.target.value)}
                />
              )}
            </Field>

            <Field label="Reorder threshold" hint="Flagged as low at or below this level">
              {({ id }) => (
                <Input
                  id={id}
                  type="number"
                  inputMode="numeric"
                  min={0}
                  value={threshold}
                  onChange={(event) => setThreshold(event.target.value)}
                />
              )}
            </Field>
          </div>
        )}
      </Dialog>
    </div>
  );
}
