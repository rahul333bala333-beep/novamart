"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orderApi } from "@/lib/api/resources";
import { cartKey } from "./use-cart";
import type { CreateOrderInput, OrderStatus } from "@/lib/types";

export const orderKeys = {
  list: (query: Record<string, unknown>) => ["orders", query] as const,
  detail: (id: string) => ["order", id] as const,
  stats: ["order-stats"] as const,
};

export function useOrders(query: { page?: number; size?: number; status?: OrderStatus; userId?: string } = {}) {
  return useQuery({
    queryKey: orderKeys.list(query),
    queryFn: () => orderApi.list(query),
    placeholderData: (previous) => previous,
  });
}

export function useOrder(id: string) {
  return useQuery({
    queryKey: orderKeys.detail(id),
    queryFn: () => orderApi.detail(id),
    enabled: Boolean(id),
  });
}

export function useOrderStats() {
  return useQuery({
    queryKey: orderKeys.stats,
    queryFn: () => orderApi.stats(),
    staleTime: 30_000,
  });
}

/**
 * Places an order.
 *
 * The idempotency key is minted once per mounted checkout form, not per click.
 * A key regenerated on each attempt would defeat the protection entirely: two
 * rapid submissions would carry two different keys and the server would treat
 * them as two genuinely different orders.
 */
export function usePlaceOrder(idempotencyKey: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: CreateOrderInput) => orderApi.create(input, idempotencyKey),
    onSuccess: () => {
      // Checkout emptied the cart and changed stock server-side; both caches are
      // now wrong and must not be shown again.
      void queryClient.invalidateQueries({ queryKey: cartKey });
      void queryClient.invalidateQueries({ queryKey: ["orders"] });
      void queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });
}

export function useCancelOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) => orderApi.cancel(id, reason),
    onSuccess: (_order, variables) => {
      void queryClient.invalidateQueries({ queryKey: orderKeys.detail(variables.id) });
      void queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
  });
}
