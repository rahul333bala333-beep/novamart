"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/client";
import { cartApi } from "@/lib/api/resources";
import { useAuth } from "@/lib/auth/auth-context";
import { useToast } from "@/components/ui/toast";
import type { Cart } from "@/lib/types";

export const cartKey = ["cart"] as const;

export function useCart() {
  const { isAuthenticated, initialising } = useAuth();
  return useQuery({
    queryKey: cartKey,
    queryFn: () => cartApi.get(),
    // A signed-out visitor has no server-side cart. Asking for one would return
    // a guaranteed 401 on every page that shows the header count.
    enabled: isAuthenticated && !initialising,
    staleTime: 0,
  });
}

/**
 * Cart mutations.
 *
 * Every one of them writes the server's response straight into the cache rather
 * than triggering a refetch. The endpoints already return the recalculated cart,
 * so a refetch would be a second round trip for data the client is holding.
 */
export function useCartMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const write = (cart: Cart) => queryClient.setQueryData(cartKey, cart);

  const onError = (error: unknown) => {
    toast.error(
      error instanceof ApiError
        ? error.message
        : "We could not update your cart. Please try again."
    );
  };

  const addItem = useMutation({
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
      cartApi.addItem(productId, quantity),
    onSuccess: (cart) => {
      write(cart);
      toast.success("Added to your bag");
    },
    onError,
  });

  const setQuantity = useMutation({
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
      cartApi.setQuantity(productId, quantity),
    onSuccess: write,
    onError,
  });

  const removeItem = useMutation({
    mutationFn: (productId: string) => cartApi.removeItem(productId),
    onSuccess: (cart) => {
      write(cart);
      toast.success("Removed from your bag");
    },
    onError,
  });

  const clear = useMutation({
    mutationFn: () => cartApi.clear(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: cartKey });
      toast.success("Bag emptied");
    },
    onError,
  });

  return { addItem, setQuantity, removeItem, clear };
}
