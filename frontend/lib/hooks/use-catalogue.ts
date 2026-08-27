"use client";

import { useQuery } from "@tanstack/react-query";
import { catalogueApi } from "@/lib/api/resources";
import type { ProductQuery } from "@/lib/types";

/**
 * Query keys are centralised so an invalidation cannot miss a cache entry
 * because a key was spelled differently at the two ends.
 */
export const catalogueKeys = {
  products: (query: ProductQuery) => ["products", query] as const,
  product: (idOrSlug: string) => ["product", idOrSlug] as const,
  categories: ["categories"] as const,
  brands: ["brands"] as const,
};

export function useProducts(query: ProductQuery) {
  return useQuery({
    queryKey: catalogueKeys.products(query),
    queryFn: () => catalogueApi.products(query),
    // Keeps the previous page on screen while the next one loads, so paging and
    // filtering do not collapse the layout to a spinner and back.
    placeholderData: (previous) => previous,
  });
}

export function useProduct(idOrSlug: string) {
  return useQuery({
    queryKey: catalogueKeys.product(idOrSlug),
    queryFn: () => catalogueApi.product(idOrSlug),
    enabled: Boolean(idOrSlug),
  });
}

export function useCategories() {
  return useQuery({
    queryKey: catalogueKeys.categories,
    queryFn: () => catalogueApi.categories(),
    // The taxonomy changes very rarely; refetching it on every page is waste.
    staleTime: 10 * 60_000,
  });
}

export function useBrands() {
  return useQuery({
    queryKey: catalogueKeys.brands,
    queryFn: () => catalogueApi.brands(),
    staleTime: 10 * 60_000,
  });
}
