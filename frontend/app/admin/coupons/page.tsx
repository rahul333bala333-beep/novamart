"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Percent, Plus, Tag, Trash2 } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { DataTable, type Column } from "@/components/admin/data-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog, Dialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Field, Input, Select } from "@/components/ui/field";
import { useToast } from "@/components/ui/toast";
import { couponApi } from "@/lib/api/resources";
import { formatCurrency, formatDate } from "@/lib/format";
import type { Coupon, CreateCouponInput, DiscountType } from "@/lib/types";

export default function AdminCouponsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [deleteTarget, setDeleteTarget] = React.useState<Coupon | null>(null);

  // Form states
  const [code, setCode] = React.useState("");
  const [discountType, setDiscountType] = React.useState<DiscountType>("PERCENTAGE");
  const [discountValue, setDiscountValue] = React.useState(10);
  const [minOrderAmount, setMinOrderAmount] = React.useState(0);
  const [maxDiscount, setMaxDiscount] = React.useState<number | undefined>(undefined);
  const [usageLimit, setUsageLimit] = React.useState<number | undefined>(100);

  const coupons = useQuery({
    queryKey: ["admin", "coupons"],
    queryFn: () => couponApi.list(),
  });

  const createCoupon = useMutation({
    mutationFn: (input: CreateCouponInput) => couponApi.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "coupons"] });
      toast.success("Coupon created successfully");
      setIsCreateOpen(false);
      setCode("");
      setDiscountValue(10);
      setMinOrderAmount(0);
    },
    onError: (err: Error) => {
      toast.error(err.message || "Failed to create coupon");
    },
  });

  const deleteCoupon = useMutation({
    mutationFn: (id: string) => couponApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "coupons"] });
      toast.success("Coupon deleted");
      setDeleteTarget(null);
    },
    onError: (err: Error) => {
      toast.error(err.message || "Failed to delete coupon");
    },
  });

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim()) {
      toast.error("Please enter a coupon code");
      return;
    }
    createCoupon.mutate({
      code: code.trim().toUpperCase(),
      discountType,
      discountValue: Number(discountValue),
      minOrderAmount: Number(minOrderAmount) || 0,
      maxDiscount: maxDiscount ? Number(maxDiscount) : undefined,
      usageLimit: usageLimit ? Number(usageLimit) : undefined,
    });
  };

  const columns: Column<Coupon>[] = [
    {
      key: "code",
      header: "Coupon Code",
      cell: (coupon) => (
        <div className="flex items-center gap-2">
          <Tag className="size-4 text-accent" />
          <span className="font-mono font-bold text-ink uppercase tracking-wider">
            {coupon.code}
          </span>
        </div>
      ),
    },
    {
      key: "discount",
      header: "Discount",
      cell: (coupon) => (
        <span className="font-semibold text-emerald-600 dark:text-emerald-400">
          {coupon.discountType === "PERCENTAGE"
            ? `${coupon.discountValue}% OFF`
            : coupon.discountType === "FIXED_AMOUNT"
              ? `${formatCurrency(coupon.discountValue)} OFF`
              : "FREE SHIPPING"}
        </span>
      ),
    },
    {
      key: "minOrder",
      header: "Min Order",
      showOnMobile: false,
      cell: (coupon) => (
        <span className="tabular text-muted">
          {coupon.minOrderAmount > 0 ? formatCurrency(coupon.minOrderAmount) : "None"}
        </span>
      ),
    },
    {
      key: "usage",
      header: "Usage / Limit",
      cell: (coupon) => (
        <span className="tabular text-sm text-ink">
          {coupon.usageCount} / {coupon.usageLimit ?? "∞"}
        </span>
      ),
    },
    {
      key: "status",
      header: "Status",
      cell: (coupon) =>
        coupon.active ? <Badge tone="success">Active</Badge> : <Badge tone="neutral">Expired</Badge>,
    },
    {
      key: "created",
      header: "Created",
      showOnMobile: false,
      cell: (coupon) => <span className="text-xs text-muted">{formatDate(coupon.createdAt)}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      align: "right",
      cell: (coupon) => (
        <Button
          size="icon"
          variant="ghost"
          aria-label={`Delete coupon ${coupon.code}`}
          className="text-muted hover:text-danger size-8"
          onClick={() => setDeleteTarget(coupon)}
        >
          <Trash2 className="size-4" />
        </Button>
      ),
    },
  ];

  if (coupons.isError) {
    return <ErrorState description="We could not load coupons." onRetry={() => coupons.refetch()} />;
  }

  return (
    <div>
      <AdminPageHeader
        title="Promotions & Coupons"
        description="Create and manage discount codes, percentage vouchers, and free shipping promos"
        action={
          <Button onClick={() => setIsCreateOpen(true)}>
            <Plus className="size-4 mr-2" />
            Create Coupon
          </Button>
        }
      />

      <DataTable
        caption="Coupons and promotions"
        columns={columns}
        rows={coupons.data ?? []}
        keyOf={(coupon) => coupon.id}
        loading={coupons.isLoading}
        emptyState={
          <EmptyState
            icon={Percent}
            title="No coupons created yet"
            description="Create your first promotion to offer discounts to shoppers."
          />
        }
      />

      <Dialog
        open={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="Create Promotion Code"
        description="Add a new promotional voucher for customers at checkout"
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button
              type="button"
              loading={createCoupon.isPending}
              onClick={handleCreateSubmit}
            >
              Save Coupon
            </Button>
          </>
        }
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4 py-2">
          <Field label="Coupon Code" required>
            {({ id }) => (
              <Input
                id={id}
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder="e.g. FESTIVE25"
                className="uppercase font-mono font-bold"
                maxLength={30}
                required
              />
            )}
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Discount Type" required>
              {({ id }) => (
                <Select
                  id={id}
                  value={discountType}
                  onChange={(e) => setDiscountType(e.target.value as DiscountType)}
                >
                  <option value="PERCENTAGE">Percentage (%)</option>
                  <option value="FIXED_AMOUNT">Fixed Amount (Rs)</option>
                  <option value="FREE_SHIPPING">Free Shipping</option>
                </Select>
              )}
            </Field>

            <Field label="Value" required>
              {({ id }) => (
                <Input
                  id={id}
                  type="number"
                  min={1}
                  max={discountType === "PERCENTAGE" ? 100 : 100000}
                  value={discountValue}
                  onChange={(e) => setDiscountValue(Number(e.target.value))}
                  disabled={discountType === "FREE_SHIPPING"}
                  required
                />
              )}
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Min Order (Rs)">
              {({ id }) => (
                <Input
                  id={id}
                  type="number"
                  min={0}
                  value={minOrderAmount}
                  onChange={(e) => setMinOrderAmount(Number(e.target.value))}
                  placeholder="0"
                />
              )}
            </Field>

            <Field label="Max Discount (Rs)">
              {({ id }) => (
                <Input
                  id={id}
                  type="number"
                  min={1}
                  value={maxDiscount ?? ""}
                  onChange={(e) => setMaxDiscount(e.target.value ? Number(e.target.value) : undefined)}
                  placeholder="Optional cap"
                />
              )}
            </Field>
          </div>

          <Field label="Total Usage Limit">
            {({ id }) => (
              <Input
                id={id}
                type="number"
                min={1}
                value={usageLimit ?? ""}
                onChange={(e) => setUsageLimit(e.target.value ? Number(e.target.value) : undefined)}
                placeholder="e.g. 100 (leave blank for unlimited)"
              />
            )}
          </Field>
        </form>
      </Dialog>

      {deleteTarget && (
        <ConfirmDialog
          open={!!deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onConfirm={() => deleteCoupon.mutate(deleteTarget.id)}
          title={`Delete coupon ${deleteTarget.code}?`}
          description="Shoppers will no longer be able to apply this discount at checkout."
          confirmLabel="Delete Coupon"
          loading={deleteCoupon.isPending}
          destructive
        />
      )}
    </div>
  );
}
