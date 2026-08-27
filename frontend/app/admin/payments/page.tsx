"use client";

import { useQuery } from "@tanstack/react-query";
import { CreditCard } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { DataTable, type Column } from "@/components/admin/data-table";
import { Badge } from "@/components/ui/badge";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { paymentApi } from "@/lib/api/resources";
import { formatCurrency, formatDateTime } from "@/lib/format";
import type { Payment } from "@/lib/types";

export default function AdminPaymentsPage() {
  const [page, setPage] = React.useState(0);

  const payments = useQuery({
    queryKey: ["payments", page],
    queryFn: () => paymentApi.list({ page, size: 20 }),
    placeholderData: (previous) => previous,
  });

  const columns: Column<Payment>[] = [
    {
      key: "reference",
      header: "Reference",
      cell: (payment) => <span className="tabular font-medium">{payment.transactionReference}</span>,
    },
    {
      key: "created",
      header: "Opened",
      showOnMobile: false,
      cell: (payment) => (
        <span className="text-[length:--text-small] text-muted">{formatDateTime(payment.createdAt)}</span>
      ),
    },
    {
      key: "method",
      header: "Method",
      cell: (payment) => (payment.method === "CASH_ON_DELIVERY" ? "Cash on delivery" : "Card (simulated)"),
    },
    {
      key: "status",
      header: "Status",
      cell: (payment) => (
        <Badge
          tone={
            payment.status === "SUCCESS"
              ? "success"
              : payment.status === "FAILED"
                ? "danger"
                : payment.status === "REFUNDED"
                  ? "info"
                  : "warning"
          }
        >
          {payment.status}
        </Badge>
      ),
    },
    {
      key: "amount",
      header: "Amount",
      align: "right",
      cell: (payment) => (
        <span className="tabular font-medium">{formatCurrency(payment.amount, payment.currency)}</span>
      ),
    },
  ];

  if (payments.isError) {
    return <ErrorState description="We could not load payments." onRetry={() => payments.refetch()} />;
  }

  return (
    <div>
      <AdminPageHeader
        title="Payments"
        description="Every payment recorded by the simulated gateway. No real transactions are processed."
      />

      <DataTable
        caption="Payments"
        columns={columns}
        rows={payments.data?.content ?? []}
        keyOf={(payment) => payment.id}
        loading={payments.isLoading}
        emptyState={
          <EmptyState
            icon={CreditCard}
            title="No payments yet"
            description="Payments appear here once an order has been placed."
          />
        }
      />

      {payments.data && (
        <Pagination meta={payments.data.page} onPageChange={setPage} className="mt-6" />
      )}
    </div>
  );
}
