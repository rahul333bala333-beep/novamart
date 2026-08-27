"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ChevronRight, Heart, RotateCcw, ShieldCheck, Truck } from "lucide-react";
import * as React from "react";
import { ProductCard } from "@/components/commerce/product-card";
import { ProductImage } from "@/components/commerce/product-image";
import { ProductReviewsSection } from "@/components/commerce/product-reviews";
import { QuantityStepper } from "@/components/commerce/quantity-stepper";
import { StockStatus } from "@/components/commerce/stock-status";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Price } from "@/components/ui/price";
import { Rating } from "@/components/ui/rating";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { useAuth } from "@/lib/auth/auth-context";
import { useCartMutations } from "@/lib/hooks/use-cart";
import { useWishlist, useWishlistMutations } from "@/lib/hooks/use-wishlist";
import { useProduct, useProducts } from "@/lib/hooks/use-catalogue";
import { ApiError } from "@/lib/api/client";
import { cn } from "@/lib/cn";

export default function ProductDetailPage() {
  const params = useParams<{ slug: string }>();
  const router = useRouter();
  const toast = useToast();
  const { isAuthenticated } = useAuth();
  const { addItem } = useCartMutations();
  const { data: wishlist } = useWishlist();
  const { toggle: toggleWishlist, addItem: addWishlist, removeItem: removeWishlist } = useWishlistMutations();

  const { data, isLoading, isError, error, refetch } = useProduct(params.slug);
  const [quantity, setQuantity] = React.useState(1);
  const [activeImage, setActiveImage] = React.useState(0);

  const product = data?.product;
  const availability = data?.availability ?? null;
  const isWishlisted = product ? (wishlist?.items?.some((i) => i.productId === product.id) ?? false) : false;
  const isWishlistPending = addWishlist.isPending || removeWishlist.isPending;

  // Related products come from the same category, with the product itself
  // filtered out so the page never recommends what the shopper is looking at.
  const related = useProducts({
    category: product?.categorySlug,
    size: 5,
  });
  const relatedItems = (related.data?.content ?? []).filter((item) => item.id !== product?.id).slice(0, 4);

  const outOfStock = availability !== null && !availability.inStock;
  const maxQuantity = Math.min(20, Math.max(1, availability?.availableQuantity ?? 20));

  function requireSignIn() {
    toast.push("Sign in to start a bag");
    router.push(`/login?next=/products/${params.slug}`);
  }

  function handleAdd() {
    if (!isAuthenticated) return requireSignIn();
    if (!product) return;
    addItem.mutate({ productId: product.id, quantity });
  }

  function handleBuyNow() {
    if (!isAuthenticated) return requireSignIn();
    if (!product) return;
    addItem.mutate(
      { productId: product.id, quantity },
      { onSuccess: () => router.push("/checkout") }
    );
  }

  if (isError) {
    const notFound = error instanceof ApiError && error.status === 404;
    return (
      <div className="container-page py-16">
        {notFound ? (
          <EmptyState
            title="We could not find that product"
            description="It may have been discontinued or the address may be mistyped."
            action={
              <Link
                href="/products"
                className="inline-flex h-11 items-center rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
              >
                Browse the catalogue
              </Link>
            }
          />
        ) : (
          <ErrorState description="We could not load this product." onRetry={() => refetch()} />
        )}
      </div>
    );
  }

  if (isLoading || !product) {
    return (
      <div className="container-page grid gap-10 py-10 lg:grid-cols-2">
        <Skeleton className="aspect-square w-full" />
        <div className="flex flex-col gap-4">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-5 w-32" />
          <Skeleton className="h-9 w-40" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-12 w-full" />
        </div>
      </div>
    );
  }

  const images = data.images.length > 0 ? data.images : [product.imageUrl];

  return (
    <div className="container-page py-6 lg:py-10">
      {/* Breadcrumbs. Three levels deep is where orientation starts to matter,
          and they double as a one-click route back to the filtered category. */}
      <nav aria-label="Breadcrumb" className="mb-6">
        <ol className="flex flex-wrap items-center gap-1.5 text-[length:--text-small] text-muted">
          <li>
            <Link href="/" className="transition-colors hover:text-ink">
              Home
            </Link>
          </li>
          <ChevronRight className="size-3.5" aria-hidden="true" />
          <li>
            <Link href="/products" className="transition-colors hover:text-ink">
              Products
            </Link>
          </li>
          <ChevronRight className="size-3.5" aria-hidden="true" />
          <li>
            <Link
              href={`/products?category=${product.categorySlug}`}
              className="transition-colors hover:text-ink"
            >
              {product.categoryName}
            </Link>
          </li>
          <ChevronRight className="size-3.5" aria-hidden="true" />
          <li aria-current="page" className="truncate text-ink">
            {product.name}
          </li>
        </ol>
      </nav>

      <div className="grid gap-10 lg:grid-cols-2 lg:gap-14">
        {/* ------------------------------------------------------- gallery */}
        <div className="flex flex-col gap-3">
          <div className="overflow-hidden rounded-[--radius-lg] border border-line bg-surface">
            <ProductImage
              src={images[activeImage]}
              alt={`${product.name}, image ${activeImage + 1} of ${images.length}`}
              priority
              sizes="(min-width: 1024px) 50vw, 100vw"
              className="aspect-square w-full"
            />
          </div>

          {images.length > 1 && (
            <div className="grid grid-cols-4 gap-3" role="group" aria-label="Product images">
              {images.map((url, index) => (
                <button
                  key={url + index}
                  type="button"
                  onClick={() => setActiveImage(index)}
                  aria-label={`Show image ${index + 1}`}
                  aria-current={index === activeImage}
                  className={cn(
                    "cursor-pointer overflow-hidden rounded-[--radius-md] border transition-colors",
                    index === activeImage
                      ? "border-ink"
                      : "border-line hover:border-line-strong"
                  )}
                >
                  <ProductImage
                    src={url}
                    alt=""
                    sizes="12vw"
                    className="aspect-square w-full"
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* --------------------------------------------------------- detail */}
        <div>
          <p className="text-[length:--text-caption] uppercase tracking-[0.1em] text-muted">
            {product.brandName ?? product.categoryName}
          </p>

          <h1 className="mt-2 font-[family-name:--font-display] text-[length:--text-h1] font-semibold leading-tight tracking-[-0.02em] text-ink">
            {product.name}
          </h1>

          <div className="mt-3 flex flex-wrap items-center gap-4">
            {product.ratingCount > 0 && (
              <Rating value={product.ratingAverage} count={product.ratingCount} size="md" />
            )}
            <StockStatus availability={availability} />
          </div>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <Price
              amount={product.price}
              compareAt={product.compareAtPrice}
              currency={product.currency}
              size="lg"
            />
            {product.discountPercent ? (
              <Badge tone="accent">Save {product.discountPercent}%</Badge>
            ) : null}
          </div>
          <p className="mt-1 text-[length:--text-caption] text-muted">Inclusive of all taxes</p>

          {product.shortDescription && (
            <p className="mt-6 text-[length:--text-body] leading-relaxed text-ink-soft">
              {product.shortDescription}
            </p>
          )}

          {/* ------------------------------------------------------ actions */}
          <div className="mt-8 flex flex-col gap-3 border-t border-line pt-6">
            <div className="flex items-center gap-4">
              <span className="text-[length:--text-small] font-medium text-ink">Quantity</span>
              <QuantityStepper
                value={quantity}
                onChange={setQuantity}
                max={maxQuantity}
                disabled={outOfStock}
              />
              {availability && availability.inStock && availability.availableQuantity <= 5 && (
                <span className="text-[length:--text-caption] text-warning">
                  {availability.availableQuantity} available
                </span>
              )}
            </div>

            <div className="mt-2 flex flex-col gap-3 sm:flex-row">
              <Button
                size="lg"
                onClick={handleBuyNow}
                disabled={outOfStock}
                loading={addItem.isPending}
                className="flex-1"
              >
                Buy now
              </Button>
              <Button
                size="lg"
                variant="secondary"
                onClick={handleAdd}
                disabled={outOfStock}
                loading={addItem.isPending}
                className="flex-1"
              >
                Add to bag
              </Button>
              <Button
                size="lg"
                variant="secondary"
                onClick={() => toggleWishlist(product.id, isWishlisted)}
                disabled={isWishlistPending}
                aria-label={isWishlisted ? "Remove from wishlist" : "Add to wishlist"}
                className={cn(
                  "px-4 transition-colors",
                  isWishlisted && "border-rose-300 text-rose-600 bg-rose-50/60 dark:bg-rose-950/20"
                )}
              >
                <Heart className={cn("size-5 mr-2 transition-transform active:scale-125", isWishlisted ? "fill-rose-500 text-rose-500" : "fill-none")} />
                {isWishlisted ? "Saved" : "Wishlist"}
              </Button>
            </div>

            {/* The disabled button explains itself. A control that is greyed out
                with no reason given reads as a broken page. */}
            {outOfStock && (
              <p className="text-[length:--text-small] text-danger" role="status">
                This product is currently out of stock. Add-to-bag will return when it is restocked.
              </p>
            )}
          </div>

          {/* ---------------------------------------------------- assurances */}
          <ul className="mt-8 grid gap-3 border-t border-line pt-6 sm:grid-cols-3">
            {[
              { icon: Truck, label: "Free delivery over Rs 999" },
              { icon: RotateCcw, label: "30-day returns" },
              { icon: ShieldCheck, label: "2-year warranty" },
            ].map(({ icon: Icon, label }) => (
              <li key={label} className="flex items-center gap-2 text-[length:--text-small] text-ink-soft">
                <Icon className="size-4 shrink-0 text-muted" aria-hidden="true" />
                {label}
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* ------------------------------------------------ description + specs */}
      <div className="mt-14 grid gap-10 border-t border-line pt-10 lg:grid-cols-2 lg:gap-16">
        <section>
          <h2 className="font-[family-name:--font-display] text-[length:--text-h3] font-semibold text-ink">
            About this product
          </h2>
          <p className="mt-4 max-w-prose text-[length:--text-body] leading-relaxed text-ink-soft">
            {data.description}
          </p>
        </section>

        {data.specifications.length > 0 && (
          <section>
            <h2 className="font-[family-name:--font-display] text-[length:--text-h3] font-semibold text-ink">
              Specifications
            </h2>
            <dl className="mt-4 overflow-hidden rounded-[--radius-lg] border border-line">
              {data.specifications.map((spec, index) => (
                <div
                  key={spec.label}
                  className={cn(
                    "grid grid-cols-[minmax(0,10rem)_1fr] gap-4 px-4 py-3 text-[length:--text-body]",
                    index % 2 === 0 ? "bg-surface" : "bg-sunken"
                  )}
                >
                  <dt className="text-muted">{spec.label}</dt>
                  <dd className="text-ink">{spec.value}</dd>
                </div>
              ))}
            </dl>
          </section>
        )}
      </div>

      {/* -------------------------------------------- reviews & ratings */}
      <ProductReviewsSection
        productId={product.id}
        productName={product.name}
        initialAverage={product.ratingAverage}
        initialCount={product.ratingCount}
      />

      {/* --------------------------------------------------------- related */}
      {relatedItems.length > 0 && (
        <section className="mt-14 border-t border-line pt-10">
          <h2 className="font-[family-name:--font-display] text-[length:--text-h2] font-semibold text-ink">
            More from {product.categoryName}
          </h2>
          <div className="mt-6 grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-4">
            {relatedItems.map((item) => (
              <ProductCard key={item.id} product={item} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
