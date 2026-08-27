"use client";

import Link from "next/link";
import { Check, CheckCheck, Inbox, Mail, Package, Sparkles } from "lucide-react";
import * as React from "react";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useNotifications, useNotificationMutations } from "@/lib/hooks/use-notifications";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/cn";

export default function AccountNotificationsPage() {
  const { data, isLoading } = useNotifications({ size: 30 });
  const { markRead, markAllRead } = useNotificationMutations();

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-7 w-44" />
        <div className="space-y-3">
          {[1, 2, 3, 4].map((n) => (
            <Skeleton key={n} className="h-20 w-full rounded-[--radius-lg]" />
          ))}
        </div>
      </div>
    );
  }

  const notifications = data?.content ?? [];
  const unreadList = notifications.filter((n) => !n.read);

  if (notifications.length === 0) {
    return (
      <EmptyState
        icon={Inbox}
        title="No notifications yet"
        description="We'll send updates here about your order status, deliveries, and account activity."
        action={
          <Link
            href="/products"
            className="inline-flex h-11 items-center justify-center rounded-[--radius-md] bg-ink px-5 text-sm font-medium text-white transition-colors hover:bg-ink/90"
          >
            Browse products
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-[length:--text-h2] font-semibold text-ink">Notifications</h2>
          <p className="mt-1 text-sm text-muted">
            {unreadList.length > 0
              ? `You have ${unreadList.length} unread message${unreadList.length > 1 ? "s" : ""}`
              : "All caught up!"}
          </p>
        </div>

        {unreadList.length > 0 && (
          <Button
            size="sm"
            variant="secondary"
            onClick={() => markAllRead.mutate()}
            loading={markAllRead.isPending}
          >
            <CheckCheck className="size-4 mr-1.5" />
            Mark all as read
          </Button>
        )}
      </div>

      <div className="divide-y divide-line rounded-[--radius-lg] border border-line bg-surface overflow-hidden">
        {notifications.map((item) => {
          const isUnread = !item.read;
          return (
            <div
              key={item.id}
              className={cn(
                "flex items-start gap-4 p-4 transition-colors",
                isUnread ? "bg-accent/5 font-normal" : "bg-surface"
              )}
            >
              <div className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-full bg-sunken">
                {item.type.includes("ORDER") ? (
                  <Package className="size-4 text-accent" />
                ) : item.type.includes("PAYMENT") ? (
                  <Sparkles className="size-4 text-emerald-600 dark:text-emerald-400" />
                ) : (
                  <Mail className="size-4 text-muted" />
                )}
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-start justify-between gap-2">
                  <h3 className={cn("text-sm text-ink", isUnread ? "font-semibold" : "font-medium")}>
                    {item.subject}
                  </h3>
                  <span className="shrink-0 text-xs text-muted">
                    {formatDateTime(item.createdAt)}
                  </span>
                </div>
                <p className="mt-1 text-xs text-ink-soft leading-relaxed whitespace-pre-line">
                  {item.body}
                </p>

                {item.referenceId && item.referenceId.startsWith("NM-") && (
                  <Link
                    href={`/account/orders`}
                    className="mt-2 inline-block text-xs font-semibold text-accent hover:underline"
                  >
                    View order details &rarr;
                  </Link>
                )}
              </div>

              {isUnread && (
                <Button
                  size="icon"
                  variant="ghost"
                  aria-label="Mark as read"
                  className="size-7 shrink-0 text-muted hover:text-ink"
                  onClick={() => markRead.mutate(item.id)}
                  disabled={markRead.isPending}
                >
                  <Check className="size-3.5" />
                </Button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
