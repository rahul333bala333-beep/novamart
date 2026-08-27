"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notificationApi } from "@/lib/api/resources";
import { useAuth } from "@/lib/auth/auth-context";

export const notificationsKey = ["notifications"] as const;
export const unreadCountKey = ["notifications", "unread-count"] as const;

export function useNotifications(query: { page?: number; size?: number; type?: string } = {}) {
  const { isAuthenticated, initialising } = useAuth();
  return useQuery({
    queryKey: [...notificationsKey, query],
    queryFn: () => notificationApi.list(query),
    enabled: isAuthenticated && !initialising,
    staleTime: 5000,
  });
}

export function useUnreadNotificationCount() {
  const { isAuthenticated, initialising } = useAuth();
  return useQuery({
    queryKey: unreadCountKey,
    queryFn: () => notificationApi.unreadCount(),
    enabled: isAuthenticated && !initialising,
    refetchInterval: 15000,
  });
}

export function useNotificationMutations() {
  const queryClient = useQueryClient();

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationsKey });
      void queryClient.invalidateQueries({ queryKey: unreadCountKey });
    },
  });

  const markAllRead = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationsKey });
      void queryClient.invalidateQueries({ queryKey: unreadCountKey });
    },
  });

  return { markRead, markAllRead };
}
