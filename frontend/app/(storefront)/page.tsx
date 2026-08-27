"use client";

import Image from "next/image";
import Link from "next/link";
import {
  ArrowRight,
  ChevronRight,
  PackageCheck,
  RotateCcw,
  ShieldCheck,
  Sparkles,
  Star,
  Truck,
} from "lucide-react";
import * as React from "react";
import { ProductCard } from "@/components/commerce/product-card";
import { ProductImage } from "@/components/commerce/product-image";
import { Price } from "@/components/ui/price";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/ui/empty-state";
import { useProducts } from "@/lib/hooks/use-catalogue";
import { SectionHeading } from "@/components/layout/section-heading";

/**
 * Visual metadata for curated category showcase cards matching reference design.
 */
const CATEGORY_SHOWCASE = [
  {
    name: "Audio",
    slug: "audio",
    subtitle: "High-fidelity sound for every moment",
    image: "/images/category-audio.jpg",
  },
  {
    name: "Computing",
    slug: "computing",
    subtitle: "Powerful. Portable. Productive.",
    image: "/images/category-computing.jpg",
  },
  {
    name: "Gaming",
    slug: "gaming",
    subtitle: "Level up your experience",
    image: "/images/category-gaming.jpg",
  },
  {
    name: "Home & Kitchen",
    slug: "home-kitchen",
    subtitle: "Smart solutions for every home",
    image: "/images/category-home.jpg",
  },
  {
    name: "Photography",
    slug: "photography",
    subtitle: "Capture more. Create better.",
    image: "/images/category-photography.jpg",
  },
];

export default function HomePage() {
  const featured = useProducts({ featured: true, size: 8 });
  const newest = useProducts({ size: 4, sort: "createdAt,desc" });
  const offers = useProducts({ size: 12, sort: "price,desc" });

  // Highlight genuine catalogue discounts
  const bestOffer = (offers.data?.content ?? [])
    .filter((product) => (product.discountPercent ?? 0) > 0)
    .sort((a, b) => (b.discountPercent ?? 0) - (a.discountPercent ?? 0))[0];

  return (
    <>
      {/* ------------------------------------------------------------- HERO SECTION (Dark Luxury) */}
      <section className="relative overflow-hidden bg-[#0c0c0e] text-white">
        {/* Subtle Ambient Radial Gold Glow */}
        <div
          className="pointer-events-none absolute -top-40 right-1/4 h-[500px] w-[500px] rounded-full bg-[radial-gradient(circle_at_center,rgba(200,138,46,0.12),transparent_70%)] blur-3xl"
          aria-hidden="true"
        />

        <div className="container-page py-12 sm:py-16 lg:py-20">
          <div className="grid items-center gap-10 lg:grid-cols-[1.1fr_1.1fr] lg:gap-14">
            {/* Left Copy & Actions */}
            <div className="flex flex-col items-start max-w-xl">
              {/* Gold Collection Badge */}
              <div className="inline-flex items-center gap-2 rounded-full border border-accent/40 bg-accent/10 px-3.5 py-1 text-[11px] font-semibold tracking-widest uppercase text-[#e5a93c]">
                <span className="h-px w-3 bg-[#e5a93c]" />
                CURATED 2026 COLLECTION
                <span className="h-px w-3 bg-[#e5a93c]" />
              </div>

              {/* Display Headline */}
              <h1 className="mt-5 font-[family-name:--font-display] text-4xl sm:text-5xl lg:text-[3.5rem] font-bold leading-[1.08] tracking-tight text-white">
                Fewer things, <br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#e5a93c] via-[#f7d594] to-[#d49b45]">
                  chosen properly.
                </span>
              </h1>

              {/* Supporting Text */}
              <p className="mt-5 text-sm sm:text-base leading-relaxed text-zinc-300">
                NOVA MART carries a deliberately small, uncompromising range across audio,
                computing, home and photography. Everything here earned its place.
              </p>

              {/* Call-to-Actions */}
              <div className="mt-8 flex flex-wrap items-center gap-3.5">
                <Link
                  href="/products"
                  className="inline-flex h-12 items-center gap-2 rounded-lg bg-gradient-to-r from-[#d49b45] to-[#b87d28] px-7 text-sm font-bold text-white shadow-gold transition-all duration-200 hover:scale-[1.02] hover:brightness-110 active:scale-[0.98]"
                >
                  <span>Browse catalogue</span>
                  <ArrowRight className="size-4" aria-hidden="true" />
                </Link>

                <Link
                  href="/products?featured=true"
                  className="inline-flex h-12 items-center gap-2 rounded-lg border border-white/20 bg-white/5 px-6 text-sm font-medium text-white backdrop-blur-sm transition-all duration-200 hover:bg-white/10 hover:border-white/40"
                >
                  <span>Season picks</span>
                  <Star className="size-3.5 fill-[#e5a93c]/40 text-[#e5a93c]" />
                </Link>
              </div>

              {/* Service Highlights */}
              <div className="mt-10 flex flex-wrap items-center gap-4 sm:gap-6 border-t border-white/10 pt-6 text-xs text-zinc-400">
                <div className="flex items-center gap-2">
                  <Truck className="size-4 text-accent" />
                  <span>
                    <strong className="text-zinc-200 font-medium">Free delivery</strong> over Rs 999
                  </span>
                </div>
                <span className="hidden sm:inline text-zinc-600">&middot;</span>
                <div className="flex items-center gap-2">
                  <RotateCcw className="size-4 text-accent" />
                  <span>
                    <strong className="text-zinc-200 font-medium">30-day</strong> returns
                  </span>
                </div>
                <span className="hidden md:inline text-zinc-600">&middot;</span>
                <div className="hidden md:flex items-center gap-2">
                  <ShieldCheck className="size-4 text-accent" />
                  <span>
                    <strong className="text-zinc-200 font-medium">2-year</strong> warranty
                  </span>
                </div>
              </div>
            </div>

            {/* Right Hero Visual Showcase */}
            <div className="relative">
              <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-[#141417] shadow-2xl">
                <div className="relative aspect-[16/10] sm:aspect-[16/9] w-full">
                  <Image
                    src="/images/hero-showcase.jpg"
                    alt="NOVA MART Premium Electronics Showcase"
                    fill
                    priority
                    className="object-cover transition-transform duration-700 hover:scale-105"
                    sizes="(min-width: 1024px) 50vw, 100vw"
                  />
                  {/* Subtle Dark Gradient Overlay at edges */}
                  <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-black/20" />
                </div>

                {/* Floating Social Proof Card (Bottom Right) */}
                <div className="absolute bottom-4 right-4 z-10 flex items-center gap-3 rounded-2xl border border-white/15 bg-black/80 px-4 py-2.5 backdrop-blur-md shadow-xl">
                  {/* Overlapping Avatar Group */}
                  <div className="flex -space-x-2 overflow-hidden">
                    <div className="inline-flex size-7 items-center justify-center rounded-full bg-gradient-to-tr from-amber-600 to-amber-400 text-[10px] font-bold text-white ring-2 ring-black">
                      AN
                    </div>
                    <div className="inline-flex size-7 items-center justify-center rounded-full bg-gradient-to-tr from-zinc-700 to-zinc-500 text-[10px] font-bold text-white ring-2 ring-black">
                      RK
                    </div>
                    <div className="inline-flex size-7 items-center justify-center rounded-full bg-gradient-to-tr from-stone-600 to-stone-400 text-[10px] font-bold text-white ring-2 ring-black">
                      SP
                    </div>
                  </div>

                  <div className="flex flex-col leading-tight">
                    <span className="text-xs font-bold text-white">10K+</span>
                    <span className="text-[10px] text-zinc-400">Happy customers</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------- CATEGORY SHOWCASE SECTION */}
      <section className="bg-surface py-10 sm:py-12 border-b border-line">
        <div className="container-page">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
            {CATEGORY_SHOWCASE.map((cat) => (
              <Link
                key={cat.slug}
                href={`/products?category=${cat.slug}`}
                className="group relative flex flex-col justify-between overflow-hidden rounded-2xl border border-line-strong/60 bg-gradient-to-b from-[#f2efe9] to-[#e6e2d8] p-4 transition-all duration-300 hover:-translate-y-1 hover:shadow-card hover:border-accent/40"
              >
                  {/* Top Image Preview */}
                  <div className="relative aspect-square w-full overflow-hidden rounded-xl bg-white/40 mb-3">
                    <Image
                      src={cat.image}
                      alt={cat.name}
                      fill
                      sizes="(min-width: 1024px) 20vw, 50vw"
                      className="object-cover transition-transform duration-500 group-hover:scale-108"
                    />
                  </div>

                  {/* Bottom Text & Floating Arrow Button */}
                  <div className="flex items-end justify-between gap-2 pt-1">
                    <div className="min-w-0 flex-1">
                      <h3 className="text-sm font-bold text-ink group-hover:text-accent transition-colors">
                        {cat.name}
                      </h3>
                      <p className="mt-0.5 text-[11px] text-muted line-clamp-2 leading-tight">
                        {cat.subtitle}
                      </p>
                    </div>

                    {/* Circular Action Button */}
                    <div className="flex size-7.5 shrink-0 items-center justify-center rounded-full bg-white text-ink shadow-xs transition-all duration-200 group-hover:bg-accent group-hover:text-white group-hover:scale-110">
                      <ChevronRight className="size-4" />
                    </div>
                  </div>
                </Link>
              ))}
          </div>
        </div>
      </section>

      {/* --------------------------------------------------------- FEATURED PRODUCTS */}
      <section className="container-page py-14 sm:py-16">
        <SectionHeading
          title="Featured this season"
          description="Hand-picked for exceptional craftsmanship, performance, and durability."
          href="/products?featured=true"
          linkLabel="See all featured"
        />

        <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-4">
          {featured.isLoading &&
            Array.from({ length: 8 }).map((_, index) => <ProductCardSkeleton key={index} />)}

          {featured.isError && (
            <ErrorState
              className="col-span-full"
              description="We could not load the featured range."
              onRetry={() => featured.refetch()}
            />
          )}

          {featured.data?.content.map((product, index) => (
            <ProductCard key={product.id} product={product} priority={index < 4} />
          ))}
        </div>
      </section>

      {/* ------------------------------------------------------ PROMOTIONAL BANNER */}
      {bestOffer && (
        <section className="border-y border-line bg-gradient-to-r from-[#faf7f2] via-sunken to-[#faf7f2]">
          <div className="container-page grid items-center gap-8 py-12 md:grid-cols-2 lg:gap-16">
            <div className="order-2 md:order-1">
              <div className="inline-flex items-center gap-1.5 rounded-full bg-accent/15 px-3 py-1 text-xs font-bold text-accent">
                <Sparkles className="size-3.5" />
                <span>Special Offer &middot; {bestOffer.discountPercent}% off</span>
              </div>
              <h2 className="mt-4 font-[family-name:--font-display] text-2xl sm:text-3xl font-bold leading-tight text-ink">
                {bestOffer.name}
              </h2>
              <p className="mt-3 max-w-md text-sm leading-relaxed text-ink-soft">
                {bestOffer.shortDescription}
              </p>
              <Price
                amount={bestOffer.price}
                compareAt={bestOffer.compareAtPrice}
                currency={bestOffer.currency}
                size="lg"
                className="mt-5"
              />
              <Link
                href={`/products/${bestOffer.slug}`}
                className="mt-6 inline-flex h-11 items-center gap-2 rounded-lg bg-ink px-6 text-sm font-semibold text-white transition-all hover:bg-accent active:scale-95"
              >
                <span>View this product</span>
                <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
            </div>

            <Link
              href={`/products/${bestOffer.slug}`}
              className="group order-1 overflow-hidden rounded-2xl border border-line bg-white shadow-sm md:order-2"
            >
              <ProductImage
                src={bestOffer.imageUrl}
                alt={bestOffer.name}
                sizes="(min-width: 768px) 50vw, 100vw"
                className="aspect-[4/3] w-full [&>img]:transition-transform [&>img]:duration-700 group-hover:[&>img]:scale-105"
              />
            </Link>
          </div>
        </section>
      )}

      {/* -------------------------------------------------------- NEW ARRIVALS */}
      <section className="container-page py-14 sm:py-16">
        <SectionHeading
          title="New Arrivals"
          description="The latest additions to the curated NOVA MART catalogue."
          href="/products?sort=createdAt,desc"
          linkLabel="See all new"
        />
        <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-4">
          {newest.isLoading
            ? Array.from({ length: 4 }).map((_, index) => <ProductCardSkeleton key={index} />)
            : newest.data?.content.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
        </div>
      </section>

      {/* -------------------------------------------------------- BENEFITS */}
      <section className="border-t border-line bg-surface">
        <div className="container-page grid gap-8 py-12 sm:grid-cols-2 lg:grid-cols-4">
          {[
            {
              icon: Truck,
              title: "Free delivery over Rs 999",
              body: "Fast, tracked shipping across India. The threshold is applied automatically at checkout.",
            },
            {
              icon: RotateCcw,
              title: "30-day returns",
              body: "Hassle-free returns on unused items in original packaging. Initiate directly from your account.",
            },
            {
              icon: ShieldCheck,
              title: "2-Year Warranty",
              body: "Comprehensive manufacturer warranty support and genuine guaranteed electronics.",
            },
            {
              icon: PackageCheck,
              title: "Live Stock Verification",
              body: "Real-time inventory reservations ensure what you order is guaranteed in stock.",
            },
          ].map(({ icon: Icon, title, body }) => (
            <div key={title} className="flex items-start gap-3.5">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-accent-soft text-accent">
                <Icon className="size-5" aria-hidden="true" />
              </span>
              <div>
                <h3 className="text-sm font-bold text-ink">{title}</h3>
                <p className="mt-1 text-xs leading-relaxed text-muted">{body}</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
