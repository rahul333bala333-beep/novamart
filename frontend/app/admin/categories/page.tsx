"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Tags, Trash2 } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { DataTable, type Column } from "@/components/admin/data-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog, Dialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Field, Input, Textarea } from "@/components/ui/field";
import { useToast } from "@/components/ui/toast";
import { ApiError } from "@/lib/api/client";
import { catalogueApi } from "@/lib/api/resources";
import { useCategories } from "@/lib/hooks/use-catalogue";
import type { Category } from "@/lib/types";

export default function AdminCategoriesPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { data, isLoading, isError, refetch } = useCategories();

  const [dialogOpen, setDialogOpen] = React.useState(false);
  const [editing, setEditing] = React.useState<Category | null>(null);
  const [deleting, setDeleting] = React.useState<Category | null>(null);
  const [name, setName] = React.useState("");
  const [description, setDescription] = React.useState("");
  const [imageUrl, setImageUrl] = React.useState("");

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["categories"] });

  const save = useMutation({
    mutationFn: () =>
      editing
        ? catalogueApi.updateCategory(editing.id, { name, description, imageUrl })
        : catalogueApi.createCategory({ name, description, imageUrl }),
    onSuccess: () => {
      void invalidate();
      setDialogOpen(false);
      toast.success(editing ? "Category updated" : "Category created");
    },
    onError: (error) =>
      toast.error(error instanceof ApiError ? error.message : "We could not save that category."),
  });

  const remove = useMutation({
    mutationFn: (id: string) => catalogueApi.deleteCategory(id),
    onSuccess: () => {
      void invalidate();
      setDeleting(null);
      toast.success("Category deleted");
    },
    onError: (error) => {
      setDeleting(null);
      // CATEGORY_NOT_EMPTY comes back with an explanation the operator can act
      // on, so it is shown rather than replaced with a generic failure.
      toast.error(error instanceof ApiError ? error.message : "We could not delete that category.");
    },
  });

  function openCreate() {
    setEditing(null);
    setName("");
    setDescription("");
    setImageUrl("");
    setDialogOpen(true);
  }

  function openEdit(category: Category) {
    setEditing(category);
    setName(category.name);
    setDescription(category.description ?? "");
    setImageUrl(category.imageUrl ?? "");
    setDialogOpen(true);
  }

  const columns: Column<Category>[] = [
    {
      key: "name",
      header: "Category",
      cell: (category) => (
        <div className="min-w-0">
          <p className="truncate font-medium text-ink">{category.name}</p>
          <p className="truncate text-[length:--text-caption] text-muted">/{category.slug}</p>
        </div>
      ),
    },
    {
      key: "description",
      header: "Description",
      showOnMobile: false,
      cell: (category) => (
        <span className="line-clamp-1 text-muted">{category.description ?? "-"}</span>
      ),
    },
    {
      key: "count",
      header: "Products",
      align: "right",
      cell: (category) => (
        <Badge tone={category.productCount > 0 ? "neutral" : "warning"} className="tabular">
          {category.productCount}
        </Badge>
      ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      cell: (category) => (
        <div className="flex justify-end gap-1">
          <Button size="sm" variant="ghost" onClick={() => openEdit(category)} aria-label={`Edit ${category.name}`}>
            <Pencil className="size-3.5" />
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setDeleting(category)}
            aria-label={`Delete ${category.name}`}
          >
            <Trash2 className="size-3.5" />
          </Button>
        </div>
      ),
    },
  ];

  if (isError) {
    return <ErrorState description="We could not load categories." onRetry={() => refetch()} />;
  }

  return (
    <div>
      <AdminPageHeader
        title="Categories"
        description="The taxonomy customers browse by."
        action={
          <Button onClick={openCreate}>
            <Plus className="size-4" aria-hidden="true" />
            New category
          </Button>
        }
      />

      <DataTable
        caption="Product categories"
        columns={columns}
        rows={data ?? []}
        keyOf={(category) => category.id}
        loading={isLoading}
        emptyState={
          <EmptyState
            icon={Tags}
            title="No categories yet"
            action={<Button onClick={openCreate}>Add the first category</Button>}
          />
        }
      />

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? "Edit category" : "New category"}
        footer={
          <>
            <Button variant="secondary" onClick={() => setDialogOpen(false)} disabled={save.isPending}>
              Cancel
            </Button>
            <Button onClick={() => save.mutate()} loading={save.isPending} disabled={name.trim().length < 2}>
              {editing ? "Save changes" : "Create category"}
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <Field label="Name" required hint={editing ? "The URL slug does not change when you rename a category, so existing links keep working." : undefined}>
            {({ id, describedBy }) => (
              <Input id={id} value={name} onChange={(event) => setName(event.target.value)} aria-describedby={describedBy} />
            )}
          </Field>

          <Field label="Description">
            {({ id }) => (
              <Textarea
                id={id}
                rows={3}
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            )}
          </Field>

          <Field label="Image URL">
            {({ id }) => (
              <Input
                id={id}
                type="url"
                placeholder="https://"
                value={imageUrl}
                onChange={(event) => setImageUrl(event.target.value)}
              />
            )}
          </Field>
        </div>
      </Dialog>

      <ConfirmDialog
        open={Boolean(deleting)}
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
        title={deleting ? `Delete "${deleting.name}"?` : ""}
        description={
          deleting && deleting.productCount > 0
            ? `This category still contains ${deleting.productCount} products. The server will refuse to delete it until they are moved.`
            : "This cannot be undone."
        }
        confirmLabel="Delete category"
        loading={remove.isPending}
        destructive
      />
    </div>
  );
}
