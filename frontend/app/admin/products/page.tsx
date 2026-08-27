"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Package, Pencil, Plus, Search, Trash2 } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { DataTable, type Column } from "@/components/admin/data-table";
import { ProductImage } from "@/components/commerce/product-image";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog, Dialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { useToast } from "@/components/ui/toast";
import { ApiError } from "@/lib/api/client";
import { catalogueApi } from "@/lib/api/resources";
import { useProducts } from "@/lib/hooks/use-catalogue";
import { formatCurrency } from "@/lib/format";
import { ProductForm } from "./product-form";
import type { Product } from "@/lib/types";

export default function AdminProductsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const [page, setPage] = React.useState(0);
  const [term, setTerm] = React.useState("");
  const [search, setSearch] = React.useState("");
  const [creating, setCreating] = React.useState(false);
  const [editing, setEditing] = React.useState<Product | null>(null);
  const [deleting, setDeleting] = React.useState<Product | null>(null);

  React.useEffect(() => {
    const timer = window.setTimeout(() => {
      setSearch(term.trim());
      setPage(0);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [term]);

  const products = useProducts({ page, size: 20, search: search || undefined });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ["products"] });
    void queryClient.invalidateQueries({ queryKey: ["inventory"] });
  };

  const remove = useMutation({
    mutationFn: (id: string) => catalogueApi.deleteProduct(id),
    onSuccess: () => {
      invalidate();
      setDeleting(null);
      // Says what actually happened. The backend soft-deletes so historical
      // orders keep resolving, and claiming "deleted" would be a lie an
      // administrator could act on.
      toast.success("Product withdrawn from the catalogue");
    },
    onError: (error) =>
      toast.error(error instanceof ApiError ? error.message : "We could not remove that product."),
  });

  const columns: Column<Product>[] = [
    {
      key: "product",
      header: "Product",
      cell: (product) => (
        <div className="flex items-center gap-3">
          <ProductImage
            src={product.imageUrl}
            alt={product.name}
            sizes="40px"
            className="size-10 shrink-0 rounded-[--radius-md] border border-line"
          />
          <div className="min-w-0">
            <p className="truncate font-medium text-ink">{product.name}</p>
            <p className="tabular text-[length:--text-caption] text-muted">{product.sku}</p>
          </div>
        </div>
      ),
    },
    {
      key: "category",
      header: "Category",
      showOnMobile: false,
      cell: (product) => <span className="text-muted">{product.categoryName}</span>,
    },
    {
      key: "brand",
      header: "Brand",
      showOnMobile: false,
      cell: (product) => <span className="text-muted">{product.brandName ?? "-"}</span>,
    },
    {
      key: "price",
      header: "Price",
      align: "right",
      cell: (product) => (
        <span className="tabular font-medium">{formatCurrency(product.price, product.currency)}</span>
      ),
    },
    {
      key: "flags",
      header: "Flags",
      cell: (product) => (
        <div className="flex flex-wrap gap-1">
          {product.featured && <Badge tone="accent">Featured</Badge>}
          {product.discountPercent ? <Badge tone="warning">-{product.discountPercent}%</Badge> : null}
        </div>
      ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      cell: (product) => (
        <div className="flex justify-end gap-1">
          <Button
            size="sm"
            variant="ghost"
            onClick={(event) => {
              event.stopPropagation();
              setEditing(product);
            }}
            aria-label={`Edit ${product.name}`}
          >
            <Pencil className="size-3.5" />
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={(event) => {
              event.stopPropagation();
              setDeleting(product);
            }}
            aria-label={`Remove ${product.name}`}
          >
            <Trash2 className="size-3.5" />
          </Button>
        </div>
      ),
    },
  ];

  if (products.isError) {
    return <ErrorState description="We could not load the catalogue." onRetry={() => products.refetch()} />;
  }

  return (
    <div>
      <AdminPageHeader
        title="Products"
        description={products.data ? `${products.data.page.totalElements} active products` : undefined}
        action={
          <Button onClick={() => setCreating(true)}>
            <Plus className="size-4" aria-hidden="true" />
            New product
          </Button>
        }
      />

      <div className="relative mb-4 max-w-sm">
        <Search
          className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted"
          aria-hidden="true"
        />
        <input
          type="search"
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          placeholder="Search products"
          aria-label="Search products"
          className="h-10 w-full rounded-[--radius-md] border border-line-strong bg-surface pl-9 pr-3 text-[length:--text-body] text-ink placeholder:text-muted"
        />
      </div>

      <DataTable
        caption="Catalogue products"
        columns={columns}
        rows={products.data?.content ?? []}
        keyOf={(product) => product.id}
        loading={products.isLoading}
        emptyState={
          <EmptyState
            icon={Package}
            title={search ? "No products match that search" : "No products yet"}
            action={!search ? <Button onClick={() => setCreating(true)}>Add the first product</Button> : undefined}
          />
        }
      />

      {products.data && <Pagination meta={products.data.page} onPageChange={setPage} className="mt-6" />}

      <Dialog
        open={creating}
        onClose={() => setCreating(false)}
        title="New product"
        description="Creating a product also seeds its stock record in inventory-service."
        className="max-w-3xl"
      >
        <ProductForm
          onCancel={() => setCreating(false)}
          onSaved={() => {
            invalidate();
            setCreating(false);
            toast.success("Product created");
          }}
        />
      </Dialog>

      <Dialog
        open={Boolean(editing)}
        onClose={() => setEditing(null)}
        title="Edit product"
        className="max-w-3xl"
      >
        {editing && (
          <ProductForm
            product={editing}
            onCancel={() => setEditing(null)}
            onSaved={() => {
              invalidate();
              setEditing(null);
              toast.success("Product updated");
            }}
          />
        )}
      </Dialog>

      <ConfirmDialog
        open={Boolean(deleting)}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
        title={deleting ? `Withdraw "${deleting.name}"?` : ""}
        description="The product is hidden from the catalogue but kept on record, so past orders that contain it still display correctly."
        confirmLabel="Withdraw product"
        loading={remove.isPending}
        destructive
      />
    </div>
  );
}
