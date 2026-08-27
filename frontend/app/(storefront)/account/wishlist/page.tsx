"use client";

import Link from "next/link";
import { Heart, ShoppingBag, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Price } from "@/components/ui/price";
import { Skeleton } from "@/components/ui/skeleton";
import { ProductImage } from "@/components/commerce/product-image";
import { useWishlist, useWishlistMutations } from "@/lib/hooks/use-wishlist";
import { Badge } from "@/components/ui/badge";

export default function AccountWishlistPage() {
  const { data: wishlist, isLoading } = useWishlist();
  const { removeItem, moveToCart } = useWishlistMutations();

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-6 w-36" />
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {[1, 2, 3].map((n) => (
            <Skeleton key={n} className="h-64 rounded-[--radius-lg]" />
          ))}
        </div>
      </div>
    );
  }

  const items = wishlist?.items ?? [];

  if (items.length === 0) {
    return (
      <EmptyState
        icon={Heart}
        title="Your wishlist is empty"
        description="Save your favourite items here so you can find them easily later."
        action={
          <Link
            href="/products"
            className="inline-flex h-11 items-center justify-center rounded-[--radius-md] bg-ink px-5 text-sm font-medium text-white transition-colors hover:bg-ink/90"
          >
            Explore catalogue
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-[length:--text-h2] font-semibold text-ink">My Wishlist</h2>
        <p className="mt-1 text-sm text-muted">
          {items.length} saved item{items.length > 1 ? "s" : ""}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {items.map((item) => (
          <div
            key={item.id}
            className="flex flex-col overflow-hidden rounded-[--radius-lg] border border-line bg-surface p-3 transition-shadow hover:shadow-[--shadow-raised]"
          >
            <div className="relative aspect-square overflow-hidden rounded-[--radius-md] bg-sunken">
              <ProductImage src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" />
              {!item.inStock && (
                <Badge tone="danger" className="absolute left-2 top-2">
                  Out of stock
                </Badge>
              )}
            </div>

            <div className="flex flex-1 flex-col pt-3">
              <h3 className="line-clamp-2 text-sm font-medium text-ink">
                <Link href={`/products/${item.slug}`} className="hover:underline">
                  {item.name}
                </Link>
              </h3>

              <Price amount={item.price} currency={item.currency} className="mt-2" />

              <div className="mt-4 flex items-center gap-2 pt-2 border-t border-line">
                <Button
                  size="sm"
                  className="flex-1"
                  disabled={!item.inStock}
                  loading={moveToCart.isPending}
                  onClick={() => moveToCart.mutate(item.productId)}
                >
                  <ShoppingBag className="size-3.5 mr-1.5" />
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
