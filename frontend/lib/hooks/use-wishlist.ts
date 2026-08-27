"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/lib/api/client";
import { wishlistApi } from "@/lib/api/resources";
import { useAuth } from "@/lib/auth/auth-context";
import { useToast } from "@/components/ui/toast";
import { cartKey } from "./use-cart";
import type { Wishlist } from "@/lib/types";

export const wishlistKey = ["wishlist"] as const;

export function useWishlist() {
  const { isAuthenticated, initialising } = useAuth();
  return useQuery({
    queryKey: wishlistKey,
    queryFn: () => wishlistApi.get(),
    enabled: isAuthenticated && !initialising,
    staleTime: 0,
  });
}

export function useWishlistMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { isAuthenticated } = useAuth();

  const write = (wishlist: Wishlist) => queryClient.setQueryData(wishlistKey, wishlist);

  const onError = (error: unknown) => {
    toast.error(
      error instanceof ApiError
        ? error.message
        : "We could not update your wishlist. Please try again."
    );
  };

  const addItem = useMutation({
    mutationFn: (productId: string) => wishlistApi.addItem(productId),
    onSuccess: (wishlist) => {
      write(wishlist);
      toast.success("Added to your wishlist");
    },
    onError,
  });

  const removeItem = useMutation({
    mutationFn: (productId: string) => wishlistApi.removeItem(productId),
    onSuccess: (wishlist) => {
      write(wishlist);
      toast.success("Removed from your wishlist");
    },
    onError,
  });

  const moveToCart = useMutation({
    mutationFn: (productId: string) => wishlistApi.moveToCart(productId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: wishlistKey });
      void queryClient.invalidateQueries({ queryKey: cartKey });
      toast.success("Moved item to your bag");
    },
    onError,
  });

  const toggle = (productId: string, currentlyInWishlist: boolean) => {
    if (!isAuthenticated) {
      toast.error("Please sign in to save items to your wishlist");
      return;
    }
    if (currentlyInWishlist) {
      removeItem.mutate(productId);
    } else {
      addItem.mutate(productId);
    }
  };

  return { addItem, removeItem, moveToCart, toggle };
}
