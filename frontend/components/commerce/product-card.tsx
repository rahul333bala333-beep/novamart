"use client";

import Link from "next/link";
import { Heart, ShoppingBag } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Price } from "@/components/ui/price";
import { Rating } from "@/components/ui/rating";
import { ProductImage } from "./product-image";
import { useAuth } from "@/lib/auth/auth-context";
import { useCartMutations } from "@/lib/hooks/use-cart";
import { useWishlist, useWishlistMutations } from "@/lib/hooks/use-wishlist";
import { useToast } from "@/components/ui/toast";
import { useRouter } from "next/navigation";
import type { Product } from "@/lib/types";
import { cn } from "@/lib/cn";

/**
 * A product in a grid.
 *
 * The whole card is a link to the product, with the add-to-bag and wishlist buttons layered on
 * top. Nesting a button inside an anchor is invalid HTML and produces
 * unpredictable keyboard behaviour, so the link covers the card via an absolute
 * overlay and the button sits above it in the stacking order.
 */
export function ProductCard({ product, priority = false }: { product: Product; priority?: boolean }) {
  const { isAuthenticated } = useAuth();
  const { addItem } = useCartMutations();
  const { data: wishlist } = useWishlist();
  const { toggle, addItem: addWishlist, removeItem: removeWishlist } = useWishlistMutations();
  const toast = useToast();
  const router = useRouter();

  const isWishlisted = wishlist?.items?.some((i) => i.productId === product.id) ?? false;
  const isWishlistPending = addWishlist.isPending || removeWishlist.isPending;
  const onOffer = product.discountPercent !== null && product.discountPercent > 0;

  function handleAdd(event: React.MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
    if (!isAuthenticated) {
      toast.push("Sign in to start a bag");
      router.push(`/login?next=/products/${product.slug}`);
      return;
    }
    addItem.mutate({ productId: product.id, quantity: 1 });
  }

  function handleWishlist(event: React.MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
    if (!isAuthenticated) {
      toast.push("Sign in to save items to your wishlist");
      router.push(`/login?next=/products/${product.slug}`);
      return;
    }
    toggle(product.id, isWishlisted);
  }

  return (
    <article className="group relative flex flex-col">
      <div className="relative overflow-hidden rounded-[--radius-lg] border border-line bg-surface">
        <ProductImage
          src={product.imageUrl}
          alt={product.name}
          priority={priority}
          className={cn(
            "aspect-square w-full",
            // A restrained zoom on hover. Enough to feel responsive, not enough
            // to be a distraction across a grid of twelve.
            "[&>img]:transition-transform [&>img]:duration-500 [&>img]:ease-[--ease-out-quart] group-hover:[&>img]:scale-[1.03]"
          )}
        />

        {onOffer && (
          <Badge tone="accent" className="absolute left-3 top-3 shadow-[--shadow-raised]">
            {product.discountPercent}% off
          </Badge>
        )}

        <Button
          size="icon"
          variant="secondary"
          onClick={handleWishlist}
          disabled={isWishlistPending}
          aria-label={isWishlisted ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`}
          className={cn(
            "absolute top-3 right-3 z-10 size-8 rounded-full shadow-[--shadow-raised] transition-transform hover:scale-110",
            isWishlisted ? "bg-white text-rose-500 border-rose-200" : "bg-white/90 text-muted hover:text-rose-500"
          )}
        >
          <Heart className={cn("size-4 transition-colors", isWishlisted ? "fill-rose-500 text-rose-500" : "fill-none")} />
        </Button>

        <Button
          size="icon"
          variant="secondary"
          onClick={handleAdd}
          loading={addItem.isPending}
          aria-label={`Add ${product.name} to bag`}
          className={cn(
            "absolute bottom-3 right-3 z-10 shadow-[--shadow-raised]",
            // Revealed on hover for pointer users, always visible on touch where
            // there is no hover state to reveal it.
            "opacity-100 sm:opacity-0 sm:transition-opacity sm:duration-[--duration-base]",
            "sm:group-hover:opacity-100 sm:group-focus-within:opacity-100"
          )}
        >
          <ShoppingBag className="size-4" />
        </Button>
      </div>

      <div className="flex flex-1 flex-col gap-1.5 pt-3">
        <p className="text-[length:--text-caption] uppercase tracking-[0.08em] text-muted">
          {product.brandName ?? product.categoryName}
        </p>

        <h3 className="text-[length:--text-body] font-medium leading-snug text-ink">
          {/* The overlay link. `before:` covers the card so the entire tile is
              clickable while the accessible name stays on the real anchor. */}
          <Link
            href={`/products/${product.slug}`}
            className="before:absolute before:inset-0 before:content-[''] hover:underline"
          >
            {product.name}
          </Link>
        </h3>

        {product.ratingCount > 0 && (
          <Rating value={product.ratingAverage} count={product.ratingCount} />
        )}

        <Price
          amount={product.price}
          compareAt={product.compareAtPrice}
          currency={product.currency}
          size="sm"
          className="mt-auto pt-1"
        />
      </div>
    </article>
  );
}
