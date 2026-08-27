"use client";

import Link from "next/link";
import { Heart, ShoppingBag, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Price } from "@/components/ui/price";
import { Skeleton } from "@/components/ui/skeleton";
import { ProductImage } from "@/components/commerce/product-image";
import { useWishlist, useWishlistMutations } from "@/lib/hooks/use-wishlist";
import { useAuth } from "@/lib/auth/auth-context";
import { Badge } from "@/components/ui/badge";

export default function WishlistPage() {
  const { isAuthenticated, initialising } = useAuth();
  const { data: wishlist, isLoading } = useWishlist();
  const { removeItem, moveToCart } = useWishlistMutations();

  if (initialising || (isAuthenticated && isLoading)) {
    return (
      <div className="container-page py-10 space-y-6">
        <Skeleton className="h-10 w-48" />
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {[1, 2, 3, 4].map((n) => (
            <Skeleton key={n} className="h-72 rounded-[--radius-lg]" />
          ))}
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="container-page py-16">
        <EmptyState
          icon={Heart}
          title="Sign in to view your wishlist"
          description="Save all your favourite products in one place across devices."
          action={
            <Link
              href="/login?next=/wishlist"
              className="inline-flex h-11 items-center justify-center rounded-[--radius-md] bg-ink px-5 text-sm font-medium text-white transition-colors hover:bg-ink/90"
            >
              Sign in
            </Link>
          }
        />
      </div>
    );
  }

  const items = wishlist?.items ?? [];

  if (items.length === 0) {
    return (
      <div className="container-page py-16">
        <EmptyState
          icon={Heart}
          title="Your wishlist is empty"
          description="Explore our collections and tap the heart icon on any product to save it here."
          action={
            <Link
              href="/products"
              className="inline-flex h-11 items-center justify-center rounded-[--radius-md] bg-ink px-5 text-sm font-medium text-white transition-colors hover:bg-ink/90"
            >
              Browse products
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="container-page py-8 lg:py-12 space-y-8">
      <header className="border-b border-line pb-6 flex items-baseline justify-between">
        <div>
          <h1 className="font-[family-name:--font-display] text-[length:--text-h1] font-semibold text-ink">
            My Wishlist
          </h1>
          <p className="mt-1 text-[length:--text-body] text-muted">
            {items.length} saved item{items.length > 1 ? "s" : ""}
          </p>
        </div>
      </header>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {items.map((item) => (
          <div
            key={item.id}
            className="flex flex-col overflow-hidden rounded-[--radius-lg] border border-line bg-surface p-4 transition-all hover:shadow-[--shadow-raised]"
          >
            <div className="relative aspect-square overflow-hidden rounded-[--radius-md] bg-sunken">
              <ProductImage src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" />
              {!item.inStock && (
                <Badge tone="danger" className="absolute left-3 top-3 shadow-[--shadow-raised]">
                  Out of stock
                </Badge>
              )}
            </div>

            <div className="flex flex-1 flex-col pt-4">
              <h3 className="line-clamp-2 text-base font-medium text-ink">
                <Link href={`/products/${item.slug}`} className="hover:underline">
                  {item.name}
                </Link>
              </h3>

              <Price amount={item.price} currency={item.currency} className="mt-2" size="sm" />

              <div className="mt-6 flex items-center gap-2 pt-3 border-t border-line">
                <Button
                  size="sm"
                  className="flex-1"
                  disabled={!item.inStock}
                  loading={moveToCart.isPending}
                  onClick={() => moveToCart.mutate(item.productId)}
                >
                  <ShoppingBag className="size-4 mr-2" />
                  Move to Bag
                </Button>
                <Button
                  size="icon"
                  variant="ghost"
                  aria-label={`Remove ${item.name} from wishlist`}
                  disabled={removeItem.isPending}
                  onClick={() => removeItem.mutate(item.productId)}
                  className="text-muted hover:text-danger"
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
