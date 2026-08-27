"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import * as React from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, Input, Select, Textarea } from "@/components/ui/field";
import { ImageUpload } from "@/components/ui/image-upload";
import { useToast } from "@/components/ui/toast";
import { ApiError } from "@/lib/api/client";
import { catalogueApi } from "@/lib/api/resources";
import { useBrands, useCategories } from "@/lib/hooks/use-catalogue";
import type { Product, ProductInput } from "@/lib/types";

/**
 * Mirrors the validation in `ProductRequest` on the server. Where the two could
 * drift, the server is authoritative; these rules exist to give feedback before
 * a round trip, not to replace it.
 */
const schema = z.object({
  sku: z.string().min(2, "At least 2 characters").max(40),
  name: z.string().min(2, "At least 2 characters").max(180),
  shortDescription: z.string().max(300).optional(),
  description: z.string().min(10, "Write at least a sentence").max(5000),
  price: z.coerce.number().positive("Price must be greater than zero"),
  compareAtPrice: z.coerce.number().nonnegative().optional(),
  categoryId: z.string().min(1, "Choose a category"),
  brandId: z.string().optional(),
  featured: z.boolean().optional(),
  initialStock: z.coerce.number().int().nonnegative().optional(),
});

type FormValues = z.input<typeof schema>;

export function ProductForm({
  product,
  onSaved,
  onCancel,
}: {
  product?: Product;
  onSaved: () => void;
  onCancel: () => void;
}) {
  const { data: categories } = useCategories();
  const { data: brands } = useBrands();
  const toast = useToast();
  const [formError, setFormError] = React.useState<string | null>(null);
  const [selectedImageFile, setSelectedImageFile] = React.useState<File | null>(null);
  const [imageRemoved, setImageRemoved] = React.useState(false);
  const [isUploading, setIsUploading] = React.useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: {
      sku: product?.sku ?? "",
      name: product?.name ?? "",
      shortDescription: product?.shortDescription ?? "",
      description: "",
      price: product?.price ?? undefined,
      compareAtPrice: product?.compareAtPrice ?? undefined,
      categoryId: product?.categoryId ?? "",
      brandId: product?.brandId ?? "",
      featured: product?.featured ?? false,
      initialStock: product ? undefined : 10,
    },
  });

  const save = useMutation({
    mutationFn: async (values: ProductInput) => {
      if (product) {
        // Update flow
        const updated = await catalogueApi.updateProduct(product.id, values);
        if (selectedImageFile) {
          setIsUploading(true);
          try {
            await catalogueApi.uploadProductImage(product.id, selectedImageFile);
          } finally {
            setIsUploading(false);
          }
        }
        return updated;
      } else {
        // Creation flow
        const created = await catalogueApi.createProduct(values);
        if (selectedImageFile) {
          setIsUploading(true);
          try {
            await catalogueApi.uploadProductImage(created.id, selectedImageFile);
          } finally {
            setIsUploading(false);
          }
        }
        return created;
      }
    },
    onSuccess: () => {
      toast.success(product ? "Product updated successfully" : "Product created successfully");
      onSaved();
    },
    onError: (error) => {
      setIsUploading(false);
      // Field-level errors from the server are surfaced verbatim, so a duplicate
      // SKU says "SKU already exists" rather than "something went wrong".
      if (error instanceof ApiError) {
        setFormError(
          error.fieldErrors.length > 0
            ? error.fieldErrors.map((fieldError) => `${fieldError.field}: ${fieldError.message}`).join("; ")
            : error.message
        );
      } else {
        setFormError("We could not save this product.");
      }
      toast.error("Could not save product");
    },
  });

  function onSubmit(values: FormValues) {
    setFormError(null);
    const parsed = schema.parse(values);
    const effectiveImageUrl = imageRemoved
      ? "/uploads/products/placeholder.webp"
      : product?.imageUrl ?? "/uploads/products/placeholder.webp";

    save.mutate({
      sku: parsed.sku,
      name: parsed.name,
      shortDescription: parsed.shortDescription || undefined,
      description: parsed.description,
      price: parsed.price,
      compareAtPrice: parsed.compareAtPrice ? parsed.compareAtPrice : null,
      categoryId: parsed.categoryId,
      brandId: parsed.brandId || null,
      imageUrl: effectiveImageUrl,
      featured: parsed.featured ?? false,
      active: true,
      initialStock: parsed.initialStock,
    });
  }

  const isSubmitting = save.isPending || isUploading;

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      {formError && (
        <div
          role="alert"
          className="rounded-[--radius-md] border border-danger/30 bg-danger-soft px-3 py-2.5 text-[length:--text-small] text-ink"
        >
          {formError}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="SKU" error={errors.sku?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} {...register("sku")} />
          )}
        </Field>
        <Field label="Name" error={errors.name?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} {...register("name")} />
          )}
        </Field>
      </div>

      <Field label="Short description" hint="One line, shown on catalogue cards" error={errors.shortDescription?.message}>
        {({ id, describedBy, invalid }) => (
          <Input id={id} aria-describedby={describedBy} invalid={invalid} {...register("shortDescription")} />
        )}
      </Field>

      <Field label="Description" error={errors.description?.message} required>
        {({ id, describedBy, invalid }) => (
          <Textarea id={id} rows={5} aria-describedby={describedBy} invalid={invalid} {...register("description")} />
        )}
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Price" hint="In rupees" error={errors.price?.message} required>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="number"
              step="0.01"
              inputMode="decimal"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("price")}
            />
          )}
        </Field>
        <Field
          label="Compare-at price"
          hint="Leave empty when not on offer"
          error={errors.compareAtPrice?.message}
        >
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="number"
              step="0.01"
              inputMode="decimal"
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("compareAtPrice")}
            />
          )}
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Category" error={errors.categoryId?.message} required>
          {({ id, describedBy, invalid }) => (
            <Select id={id} aria-describedby={describedBy} invalid={invalid} {...register("categoryId")}>
              <option value="">Choose a category</option>
              {(categories ?? []).map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Brand" hint="Optional" error={errors.brandId?.message}>
          {({ id, describedBy, invalid }) => (
            <Select id={id} aria-describedby={describedBy} invalid={invalid} {...register("brandId")}>
              <option value="">No brand</option>
              {(brands ?? []).map((brand) => (
                <option key={brand.id} value={brand.id}>
                  {brand.name}
                </option>
              ))}
            </Select>
          )}
        </Field>
      </div>

      {/* Local Device Product Image Upload */}
      <ImageUpload
        label="Product Image"
        hint="PNG, JPG, JPEG, WEBP • Max 5 MB"
        value={imageRemoved ? null : product?.imageUrl}
        disabled={isSubmitting}
        onChange={(file) => {
          setSelectedImageFile(file);
          setImageRemoved(false);
        }}
        onRemoveExisting={() => {
          setImageRemoved(true);
          setSelectedImageFile(null);
        }}
      />

      {!product && (
        <Field
          label="Opening stock"
          hint="Creates the stock record in inventory-service"
          error={errors.initialStock?.message}
        >
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="number"
              inputMode="numeric"
              min={0}
              aria-describedby={describedBy}
              invalid={invalid}
              {...register("initialStock")}
            />
          )}
        </Field>
      )}

      <label className="flex cursor-pointer items-center gap-2 text-[length:--text-body] text-ink">
        <input
          type="checkbox"
          {...register("featured")}
          className="size-4 cursor-pointer accent-[--color-ink]"
        />
        Feature on the storefront home page
      </label>

      <div className="mt-2 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {isUploading ? "Uploading image..." : product ? "Save changes" : "Create product"}
        </Button>
      </div>
    </form>
  );
}
