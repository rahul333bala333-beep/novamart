"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { addressApi } from "@/lib/api/resources";
import { useAuth } from "@/lib/auth/auth-context";
import type { AddressInput } from "@/lib/types";

export const addressKey = ["addresses"] as const;

export function useAddresses() {
  const { isAuthenticated, initialising } = useAuth();
  return useQuery({
    queryKey: addressKey,
    queryFn: () => addressApi.list(),
    enabled: isAuthenticated && !initialising,
  });
}

export function useAddressMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: addressKey });

  return {
    create: useMutation({
      mutationFn: (input: AddressInput) => addressApi.create(input),
      onSuccess: invalidate,
    }),
    update: useMutation({
      mutationFn: ({ id, input }: { id: string; input: AddressInput }) => addressApi.update(id, input),
      onSuccess: invalidate,
    }),
    remove: useMutation({
      mutationFn: (id: string) => addressApi.remove(id),
      onSuccess: invalidate,
    }),
  };
}
