"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { SlidersHorizontal, X } from "lucide-react";
import * as React from "react";
import { ProductCard } from "@/components/commerce/product-card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { useBrands, useCategories, useProducts } from "@/lib/hooks/use-catalogue";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/cn";
import type { ProductQuery } from "@/lib/types";

const PAGE_SIZE = 12;

const SORT_OPTIONS = [
  { value: "createdAt,desc", label: "Newest first" },
  { value: "price,asc", label: "Price: low to high" },
  { value: "price,desc", label: "Price: high to low" },
  { value: "ratingAverage,desc", label: "Best rated" },
  { value: "name,asc", label: "Name: A to Z" },
];

const PRICE_BANDS = [
  { label: "Under Rs 5,000", min: undefined, max: 5000 },
  { label: "Rs 5,000 - 20,000", min: 5000, max: 20000 },
  { label: "Rs 20,000 - 50,000", min: 20000, max: 50000 },
  { label: "Over Rs 50,000", min: 50000, max: undefined },
];

/**
 * Catalogue browsing.
 *
 * Filter state lives in the URL rather than in component state, which is what
 * makes a filtered view shareable, bookmarkable and survivable across a browser
 * refresh or a back button. It also means the browser's history stack does the
 * undo work instead of hand-written state restoration.
 */
export function ProductListing() {
  const router = useRouter();
  const params = useSearchParams();
  const [filtersOpen, setFiltersOpen] = React.useState(false);

  const query: ProductQuery = {
    page: Number(params.get("page") ?? 0),
    size: PAGE_SIZE,
    search: params.get("search") ?? undefined,
    category: params.get("category") ?? undefined,
    brand: params.get("brand") ?? undefined,
    minPrice: params.get("minPrice") ? Number(params.get("minPrice")) : undefined,
    maxPrice: params.get("maxPrice") ? Number(params.get("maxPrice")) : undefined,
    minRating: params.get("minRating") ? Number(params.get("minRating")) : undefined,
    featured: params.get("featured") === "true" ? true : undefined,
    sort: params.get("sort") ?? "createdAt,desc",
  };

  const { data, isLoading, isError, isFetching, refetch } = useProducts(query);
  const { data: categories } = useCategories();
  const { data: brands } = useBrands();

  function apply(changes: Record<string, string | undefined>) {
    const next = new URLSearchParams(params.toString());
    for (const [key, value] of Object.entries(changes)) {
      if (value === undefined || value === "") next.delete(key);
      else next.set(key, value);
    }
    // Any filter change returns to the first page. Staying on page 4 of a
    // narrower result set usually lands on an empty page that looks like a bug.
    if (!("page" in changes)) next.delete("page");
    router.push(`/products?${next.toString()}`, { scroll: false });
  }

  function clearAll() {
    router.push("/products", { scroll: false });
  }

  const activeFilters = [
    query.category && {
      key: "category",
      label: categories?.find((c) => c.slug === query.category)?.name ?? query.category,
    },
    query.brand && {
      key: "brand",
      label: brands?.find((b) => b.slug === query.brand)?.name ?? query.brand,
    },
    query.search && { key: "search", label: `"${query.search}"` },
    query.featured && { key: "featured", label: "Featured" },
    query.minRating && { key: "minRating", label: `${query.minRating}★ & above` },
    (query.minPrice || query.maxPrice) && {
      key: "price",
      label: `${query.minPrice ? formatCurrency(query.minPrice) : "Any"} - ${
        query.maxPrice ? formatCurrency(query.maxPrice) : "Any"
      }`,
    },
  ].filter(Boolean) as { key: string; label: string }[];

  const total = data?.page.totalElements ?? 0;

  return (
    <div className="container-page py-8 lg:py-10">
      <header className="border-b border-line pb-5">
        <h1 className="font-[family-name:--font-display] text-[length:--text-h1] font-semibold tracking-[-0.02em] text-ink">
          {query.search ? `Results for "${query.search}"` : query.category
            ? categories?.find((c) => c.slug === query.category)?.name ?? "Products"
            : "All products"}
        </h1>
        <p className="mt-2 text-[length:--text-body] text-muted" aria-live="polite">
          {isLoading ? "Loading products" : `${total} ${total === 1 ? "product" : "products"}`}
        </p>
      </header>

      {/* Active filters as removable chips. A filter the shopper cannot see is a
          filter they will blame the catalogue for. */}
      {activeFilters.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 pt-4">
          {activeFilters.map((filter) => (
            <button
              key={filter.key}
              type="button"
              onClick={() =>
                apply(
                  filter.key === "price"
                    ? { minPrice: undefined, maxPrice: undefined }
                    : { [filter.key]: undefined }
                )
              }
              className="inline-flex cursor-pointer items-center gap-1.5 rounded-[--radius-sm] border border-line-strong bg-surface px-2.5 py-1 text-[length:--text-caption] text-ink transition-colors hover:bg-sunken"
            >
              {filter.label}
              <X className="size-3" aria-hidden="true" />
              <span className="sr-only">Remove filter</span>
            </button>
          ))}
          <button
            type="button"
            onClick={clearAll}
            className="cursor-pointer text-[length:--text-caption] font-medium text-ink underline underline-offset-4 hover:text-ink-soft"
          >
            Clear all
          </button>
        </div>
      )}

      <div className="flex items-center justify-between gap-4 py-4">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => setFiltersOpen((open) => !open)}
          className="lg:hidden"
          aria-expanded={filtersOpen}
        >
          <SlidersHorizontal className="size-4" aria-hidden="true" />
          Filters
        </Button>

        <label className="ml-auto flex items-center gap-2 text-[length:--text-small] text-muted">
          <span className="hidden sm:inline">Sort by</span>
          <select
            value={query.sort}
            onChange={(event) => apply({ sort: event.target.value })}
            className="h-10 cursor-pointer rounded-[--radius-md] border border-line-strong bg-surface px-3 text-[length:--text-small] text-ink"
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="grid gap-8 lg:grid-cols-[16rem_1fr]">
        {/* --------------------------------------------------------- filters */}
        <aside
          className={cn(
            "flex flex-col gap-7 lg:sticky lg:top-32 lg:h-fit lg:pr-2",
            filtersOpen ? "block" : "hidden lg:flex"
          )}
        >
          <FilterGroup title="Category">
            <FilterOption
              label="All categories"
              active={!query.category}
              onSelect={() => apply({ category: undefined })}
            />
            {(categories ?? []).map((category) => (
              <FilterOption
                key={category.id}
                label={category.name}
                count={category.productCount}
                active={query.category === category.slug}
                onSelect={() => apply({ category: category.slug })}
              />
            ))}
          </FilterGroup>

          <FilterGroup title="Brand">
            <FilterOption
              label="All brands"
              active={!query.brand}
              onSelect={() => apply({ brand: undefined })}
            />
            {(brands ?? []).map((brand) => (
              <FilterOption
                key={brand.id}
                label={brand.name}
                count={brand.productCount}
                active={query.brand === brand.slug}
                onSelect={() => apply({ brand: brand.slug })}
              />
            ))}
          </FilterGroup>

          <FilterGroup title="Price">
            <FilterOption
              label="Any price"
              active={!query.minPrice && !query.maxPrice}
              onSelect={() => apply({ minPrice: undefined, maxPrice: undefined })}
            />
            {PRICE_BANDS.map((band) => (
              <FilterOption
                key={band.label}
                label={band.label}
                active={query.minPrice === band.min && query.maxPrice === band.max}
                onSelect={() =>
                  apply({
                    minPrice: band.min?.toString(),
                    maxPrice: band.max?.toString(),
                  })
                }
              />
            ))}
          </FilterGroup>

          <FilterGroup title="Customer Rating">
            <FilterOption
              label="All ratings"
              active={!query.minRating}
              onSelect={() => apply({ minRating: undefined })}
            />
            {[4, 3, 2, 1].map((stars) => (
              <FilterOption
                key={stars}
                label={`${stars}★ & above`}
                active={query.minRating === stars}
                onSelect={() => apply({ minRating: stars.toString() })}
              />
            ))}
          </FilterGroup>
        </aside>

        {/* ---------------------------------------------------------- grid */}
        <section aria-busy={isFetching}>
          {isError ? (
            <ErrorState
              title="We could not load the catalogue"
              description="The product service may be starting up. Try again in a moment."
              onRetry={() => refetch()}
            />
          ) : isLoading ? (
            <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 xl:grid-cols-4">
              {Array.from({ length: PAGE_SIZE }).map((_, index) => (
                <ProductCardSkeleton key={index} />
              ))}
            </div>
          ) : total === 0 ? (
            <EmptyState
              title="No products match those filters"
              description="Try widening the price range or clearing a filter."
              action={
                <Button variant="secondary" onClick={clearAll}>
                  Clear all filters
                </Button>
              }
            />
          ) : (
            <>
              <div
                className={cn(
                  "grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 xl:grid-cols-4",
                  // A subtle dim while the next page loads. The previous page
                  // stays on screen so the layout never collapses to a spinner.
                  isFetching && "opacity-60 transition-opacity"
                )}
              >
                {data?.content.map((product, index) => (
                  <ProductCard key={product.id} product={product} priority={index < 4} />
                ))}
              </div>

              {data && (
                <Pagination
                  meta={data.page}
                  onPageChange={(page) => {
                    apply({ page: String(page) });
                    window.scrollTo({ top: 0, behavior: "smooth" });
                  }}
                  className="mt-12"
                />
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}

function FilterGroup({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h2 className="text-[length:--text-caption] font-semibold uppercase tracking-[0.08em] text-ink">
        {title}
      </h2>
      <div className="mt-3 flex flex-col">{children}</div>
    </div>
  );
}

function FilterOption({
  label,
  count,
  active,
  onSelect,
}: {
  label: string;
  count?: number;
  active: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={active}
      className={cn(
        "flex cursor-pointer items-center justify-between gap-2 rounded-[--radius-sm] px-2 py-2 text-left text-[length:--text-body] transition-colors",
        active ? "bg-sunken font-medium text-ink" : "text-ink-soft hover:bg-sunken hover:text-ink"
      )}
    >
      <span>{label}</span>
      {count !== undefined && (
        <Badge tone="neutral" className="tabular">
          {count}
        </Badge>
      )}
    </button>
  );
}
