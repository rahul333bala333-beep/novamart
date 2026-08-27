"use client";

import { useQuery } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { Badge } from "@/components/ui/badge";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { Skeleton } from "@/components/ui/skeleton";
import { notificationApi } from "@/lib/api/resources";
import { formatDateTime } from "@/lib/format";

export default function AdminNotificationsPage() {
  const [page, setPage] = React.useState(0);

  const notifications = useQuery({
    queryKey: ["notifications", page],
    queryFn: () => notificationApi.list({ page, size: 20 }),
    placeholderData: (previous) => previous,
  });

  if (notifications.isError) {
    return (
      <ErrorState description="We could not load notifications." onRetry={() => notifications.refetch()} />
    );
  }

  return (
    <div>
      <AdminPageHeader
        title="Notifications"
        description="Every transactional message the platform has produced."
      />

      {/* Restated here because an operator looking at a list of "SENT" messages
          would otherwise reasonably assume they were emailed. */}
      <div className="mb-5 rounded-[--radius-lg] border border-info/25 bg-info-soft px-4 py-3 text-[length:--text-small] leading-relaxed text-ink">
        <strong className="font-semibold">Delivery is simulated.</strong> Messages are recorded here
        and written to the notification-service log. No email or SMS provider is connected, so
        &ldquo;Sent&rdquo; means the mock transport accepted the message.
      </div>

      {notifications.isLoading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 5 }).map((_, index) => (
            <Skeleton key={index} className="h-24 w-full" />
          ))}
        </div>
      ) : (notifications.data?.content.length ?? 0) === 0 ? (
        <EmptyState
          icon={Bell}
          title="No notifications yet"
          description="Messages appear when an account is created or an order changes state."
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {notifications.data!.content.map((notification) => (
            <li key={notification.id} className="rounded-[--radius-lg] border border-line bg-surface p-4">
              <div className="flex flex-wrap items-center gap-2">
                <Badge tone="neutral">{notification.type.replace(/_/g, " ")}</Badge>
                <Badge
                  tone={
                    notification.status === "SENT"
                      ? "success"
                      : notification.status === "FAILED"
                        ? "danger"
                        : "warning"
                  }
                >
                  {notification.status}
                </Badge>
                <span className="ml-auto text-[length:--text-caption] text-muted">
                  {formatDateTime(notification.createdAt)}
                </span>
              </div>

              <p className="mt-2 text-[length:--text-body] font-medium text-ink">
                {notification.subject}
              </p>
              <p className="mt-1 text-[length:--text-small] leading-relaxed text-muted">
                {notification.body}
              </p>
              {notification.recipient && (
                <p className="mt-2 text-[length:--text-caption] text-muted">
                  To {notification.recipient} via {notification.channel}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}

      {notifications.data && (
        <Pagination meta={notifications.data.page} onPageChange={setPage} className="mt-6" />
      )}
    </div>
  );
}
