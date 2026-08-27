"use client";

import Link from "next/link";
import {
  AlertTriangle,
  ArrowRight,
  IndianRupee,
  Package,
  ShoppingCart,
  Users,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { StatTile } from "@/components/admin/stat-tile";
import { OrderStatusBadge } from "@/components/commerce/order-status-badge";
import { ErrorState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useOrderStats, useOrders } from "@/lib/hooks/use-orders";
import { useProducts } from "@/lib/hooks/use-catalogue";
import { inventoryApi, authApi } from "@/lib/api/resources";
import { useQuery } from "@tanstack/react-query";
import { formatCurrency, formatDate, formatNumber } from "@/lib/format";

/**
 * Back-office overview.
 *
 * Two charts, both of which answer a question an operator actually asks: is
 * revenue moving, and where are orders piling up. There is no third chart added
 * for visual balance, and no donut of a metric that a single number states more
 * clearly.
 */
export default function AdminDashboard() {
  const stats = useOrderStats();
  const recentOrders = useOrders({ size: 6 });
  const products = useProducts({ size: 1 });
  const lowStock = useQuery({
    queryKey: ["inventory", "low"],
    queryFn: () => inventoryApi.list({ lowStockOnly: true, size: 100 }),
  });
  const customers = useQuery({
    queryKey: ["users", "count"],
    queryFn: () => authApi.listUsers({ size: 1 }),
  });

  if (stats.isError) {
    return (
      <ErrorState
        title="We could not load the dashboard"
        description="The order service may be unavailable."
        onRetry={() => stats.refetch()}
      />
    );
  }

  const data = stats.data;
  const lowStockCount = lowStock.data?.page.totalElements ?? 0;

  const revenueSeries = (data?.revenueByDay ?? []).map((point) => ({
    date: point.date,
    label: new Date(point.date).toLocaleDateString("en-IN", { day: "numeric", month: "short" }),
    revenue: point.revenue,
    orders: point.orders,
  }));

  const statusSeries = (data?.statusBreakdown ?? []).map((entry) => ({
    status: entry.status,
    count: entry.count,
  }));

  const STATUS_FILL: Record<string, string> = {
    PENDING: "#a15c07",
    CONFIRMED: "#1d4ed8",
    PROCESSING: "#1d4ed8",
    SHIPPED: "#6366f1",
    OUT_FOR_DELIVERY: "#f59e0b",
    DELIVERED: "#146b3a",
    CANCELLED: "#b42318",
  };

  return (
    <div>
      <header>
        <h1 className="font-[family-name:--font-display] text-[length:--text-h1] font-semibold tracking-[-0.02em] text-ink">
          Dashboard
        </h1>
        <p className="mt-1 text-[length:--text-body] text-muted">
          Live figures across every service.
        </p>
      </header>

      {/* --------------------------------------------------------- tiles */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <StatTile
          label="Revenue"
          value={formatCurrency(data?.totalRevenue ?? 0)}
          hint="Confirmed orders, excluding cancellations"
          icon={IndianRupee}
          tone="success"
          loading={stats.isLoading}
        />
        <StatTile
          label="Orders"
          value={formatNumber(data?.totalOrders ?? 0)}
          hint={`Average order ${formatCurrency(data?.averageOrderValue ?? 0)}`}
          icon={ShoppingCart}
          loading={stats.isLoading}
        />
        <StatTile
          label="Pending orders"
          value={formatNumber(data?.pendingOrders ?? 0)}
          hint="Awaiting payment settlement"
          icon={ShoppingCart}
          tone={(data?.pendingOrders ?? 0) > 0 ? "warning" : "neutral"}
          loading={stats.isLoading}
        />
        <StatTile
          label="Customers"
          value={formatNumber(customers.data?.page.totalElements ?? 0)}
          hint="Registered accounts"
          icon={Users}
          loading={customers.isLoading}
        />
        <StatTile
          label="Products"
          value={formatNumber(products.data?.page.totalElements ?? 0)}
          hint="Active in the catalogue"
          icon={Package}
          loading={products.isLoading}
        />
        <StatTile
          label="Low stock"
          value={formatNumber(lowStockCount)}
          hint="At or below reorder threshold"
          icon={AlertTriangle}
          tone={lowStockCount > 0 ? "danger" : "neutral"}
          loading={lowStock.isLoading}
        />
      </div>

      {/* -------------------------------------------------------- charts */}
      <div className="mt-6 grid gap-4 lg:grid-cols-[1.6fr_1fr]">
        <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
          <h2 className="text-[length:--text-body] font-semibold text-ink">Revenue, last 14 days</h2>
          <p className="mt-0.5 text-[length:--text-caption] text-muted">
            Days with no orders are shown as zero rather than skipped.
          </p>

          {stats.isLoading ? (
            <Skeleton className="mt-4 h-56 w-full" />
          ) : (
            <div className="mt-4 h-56">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={revenueSeries} margin={{ top: 4, right: 4, bottom: 0, left: -12 }}>
                  <defs>
                    <linearGradient id="revenueFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#1a1815" stopOpacity={0.16} />
                      <stop offset="100%" stopColor="#1a1815" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  {/* Horizontal grid lines only. Vertical ones add ink without
                      helping anyone read a value off the axis. */}
                  <CartesianGrid stroke="#e6e2da" vertical={false} />
                  <XAxis
                    dataKey="label"
                    tick={{ fill: "#78716a", fontSize: 11 }}
                    tickLine={false}
                    axisLine={{ stroke: "#e6e2da" }}
                    interval="preserveStartEnd"
                    minTickGap={16}
                  />
                  <YAxis
                    tick={{ fill: "#78716a", fontSize: 11 }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value: number) =>
                      value >= 1000 ? `${Math.round(value / 1000)}k` : String(value)
                    }
                  />
                  <Tooltip
                    cursor={{ stroke: "#d3ccc0" }}
                    contentStyle={{
                      borderRadius: 10,
                      border: "1px solid #e6e2da",
                      fontSize: 13,
                      boxShadow: "0 4px 16px rgb(26 24 21 / 0.08)",
                    }}
                    formatter={(value, name) =>
                      name === "revenue"
                        ? [formatCurrency(Number(value ?? 0)), "Revenue"]
                        : [String(value ?? 0), "Orders"]
                    }
                  />
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke="#1a1815"
                    strokeWidth={2}
                    fill="url(#revenueFill)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="rounded-[--radius-lg] border border-line bg-surface p-4">
          <h2 className="text-[length:--text-body] font-semibold text-ink">Orders by status</h2>
          <p className="mt-0.5 text-[length:--text-caption] text-muted">
            Where orders are sitting right now.
          </p>

          {stats.isLoading ? (
            <Skeleton className="mt-4 h-56 w-full" />
          ) : statusSeries.length === 0 ? (
            <p className="mt-8 text-center text-[length:--text-body] text-muted">No orders yet.</p>
          ) : (
            <div className="mt-4 h-56">
              <ResponsiveContainer width="100%" height="100%">
                {/* Horizontal bars: status names are words, and words read far
                    better along the y-axis than rotated under a vertical bar. */}
                <BarChart
                  data={statusSeries}
                  layout="vertical"
                  margin={{ top: 4, right: 12, bottom: 0, left: 8 }}
                >
                  <CartesianGrid stroke="#e6e2da" horizontal={false} />
                  <XAxis type="number" tick={{ fill: "#78716a", fontSize: 11 }} tickLine={false} axisLine={false} allowDecimals={false} />
                  <YAxis
                    type="category"
                    dataKey="status"
                    tick={{ fill: "#57524b", fontSize: 11 }}
                    tickLine={false}
                    axisLine={false}
                    width={82}
                  />
                  <Tooltip
                    cursor={{ fill: "#f4f2ee" }}
                    contentStyle={{
                      borderRadius: 10,
                      border: "1px solid #e6e2da",
                      fontSize: 13,
                    }}
                    formatter={(value) => [String(value ?? 0), "Orders"]}
                  />
                  <Bar dataKey="count" radius={[0, 4, 4, 0]} barSize={16}>
                    {statusSeries.map((entry) => (
                      <Cell key={entry.status} fill={STATUS_FILL[entry.status] ?? "#78716a"} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>
      </div>

      {/* -------------------------------------------------- recent orders */}
      <section className="mt-6 rounded-[--radius-lg] border border-line bg-surface">
        <div className="flex items-center justify-between gap-4 border-b border-line px-4 py-3">
          <h2 className="text-[length:--text-body] font-semibold text-ink">Recent orders</h2>
          <Link
            href="/admin/orders"
            className="inline-flex items-center gap-1 text-[length:--text-small] font-medium text-ink transition-colors hover:text-ink-soft"
          >
            All orders
            <ArrowRight className="size-3.5" aria-hidden="true" />
          </Link>
        </div>

        {recentOrders.isLoading ? (
          <div className="p-4">
            <Skeleton className="h-40 w-full" />
          </div>
        ) : (recentOrders.data?.content.length ?? 0) === 0 ? (
          <p className="px-4 py-10 text-center text-[length:--text-body] text-muted">
            No orders have been placed yet.
          </p>
        ) : (
          <ul className="divide-y divide-line">
            {recentOrders.data!.content.map((order) => (
              <li key={order.id}>
                <Link
                  href={`/admin/orders/${order.id}`}
                  className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 transition-colors hover:bg-sunken"
                >
                  <span className="tabular text-[length:--text-body] font-medium text-ink">
                    {order.orderNumber}
                  </span>
                  <OrderStatusBadge status={order.status} />
                  <span className="text-[length:--text-small] text-muted">
                    {formatDate(order.placedAt)}
                  </span>
                  <span className="tabular ml-auto text-[length:--text-body] font-semibold text-ink">
                    {formatCurrency(order.total, order.currency)}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
