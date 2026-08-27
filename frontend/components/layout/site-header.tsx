"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Bell,
  Heart,
  Menu,
  RotateCcw,
  Search,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  Truck,
  User,
  X,
} from "lucide-react";
import * as React from "react";
import { Wordmark } from "@/components/brand/wordmark";
import { useAuth } from "@/lib/auth/auth-context";
import { useCart } from "@/lib/hooks/use-cart";
import { useWishlist } from "@/lib/hooks/use-wishlist";
import { useUnreadNotificationCount } from "@/lib/hooks/use-notifications";
import { useCategories } from "@/lib/hooks/use-catalogue";
import { cn } from "@/lib/cn";

/**
 * Static categories list with slugs for primary navigation fallback,
 * enriched with dynamic categories from the API.
 */
const DEFAULT_NAV_CATEGORIES = [
  { name: "Audio", slug: "audio" },
  { name: "Computing", slug: "computing" },
  { name: "Gaming", slug: "gaming" },
  { name: "Home & Kitchen", slug: "home-kitchen" },
  { name: "Mobile & Tablets", slug: "mobile-tablets" },
  { name: "Photography", slug: "photography" },
];

export function SiteHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAuthenticated, isAdmin, user } = useAuth();
  const { data: cart } = useCart();
  const { data: wishlist } = useWishlist();
  const { data: unreadNotifications } = useUnreadNotificationCount();
  const { data: categories } = useCategories();

  const [menuOpen, setMenuOpen] = React.useState(false);
  const [term, setTerm] = React.useState(searchParams.get("search") ?? "");

  const [lastPath, setLastPath] = React.useState(pathname);
  if (pathname !== lastPath) {
    setLastPath(pathname);
    setMenuOpen(false);
  }

  const itemCount = cart?.totalQuantity ?? 0;
  const wishlistCount = wishlist?.items?.length ?? 0;
  const unreadCount = unreadNotifications ?? 0;

  // Merge dynamic API categories with fallback categories for clean display
  const navCategories = React.useMemo(() => {
    if (categories && categories.length > 0) {
      return categories.slice(0, 7);
    }
    return DEFAULT_NAV_CATEGORIES;
  }, [categories]);

  function submitSearch(event: React.FormEvent) {
    event.preventDefault();
    const query = term.trim();
    router.push(query ? `/products?search=${encodeURIComponent(query)}` : "/products");
  }

  const currentCategory = searchParams.get("category");
  const isAllProductsActive = pathname === "/products" && !currentCategory && !searchParams.get("featured");

  return (
    <header className="sticky top-0 z-50 bg-surface shadow-xs">
      {/* ---------------- Top Utility / Announcement Bar (Dark Black) ---------------- */}
      <div className="bg-[#0c0c0e] text-zinc-300 border-b border-white/5 py-1.5 text-[11px] sm:text-xs">
        <div className="container-page flex items-center justify-between">
          {/* Left Service Guarantees */}
          <div className="flex items-center gap-4 sm:gap-6 text-zinc-300">
            <span className="inline-flex items-center gap-1.5 font-medium">
              <Truck className="size-3.5 text-accent" />
              <span>Free delivery over Rs 999</span>
            </span>
            <span className="hidden sm:inline-flex items-center gap-1.5 font-medium">
              <RotateCcw className="size-3.5 text-accent" />
              <span>30-day returns</span>
            </span>
            <span className="hidden md:inline-flex items-center gap-1.5 font-medium">
              <ShieldCheck className="size-3.5 text-accent" />
              <span>2-year warranty</span>
            </span>
          </div>

          {/* Right Help & Track Order */}
          <div className="flex items-center gap-3 sm:gap-5 text-zinc-400">
            <span className="hidden sm:inline">
              Need help?{" "}
              <a
                href="mailto:support@novamart.com"
                className="text-zinc-200 hover:text-accent transition-colors"
              >
                support@novamart.com
              </a>
            </span>
            <span className="hidden sm:inline text-zinc-600">|</span>
            <Link
              href={isAuthenticated ? "/account/orders" : "/login?next=/account/orders"}
              className="text-zinc-200 hover:text-accent font-medium transition-colors"
            >
              Track Order
            </Link>
          </div>
        </div>
      </div>

      {/* ---------------- Main Header Row (White Background) ---------------- */}
      <div className="border-b border-line bg-surface py-3.5">
        <div className="container-page flex items-center justify-between gap-4 lg:gap-8">
          {/* Mobile Menu Button */}
          <button
            type="button"
            onClick={() => setMenuOpen((open) => !open)}
            className="-ml-2 flex size-10 items-center justify-center rounded-lg text-ink transition-colors hover:bg-sunken lg:hidden"
            aria-expanded={menuOpen}
            aria-controls="mobile-nav"
            aria-label={menuOpen ? "Close menu" : "Open menu"}
          >
            {menuOpen ? <X className="size-5" /> : <Menu className="size-5" />}
          </button>

          {/* Brand Logo */}
          <Wordmark />

          {/* Center Search Bar */}
          <form
            onSubmit={submitSearch}
            role="search"
            className="hidden flex-1 max-w-xl mx-auto md:flex items-center"
          >
            <div className="relative w-full">
              <input
                type="search"
                value={term}
                onChange={(event) => setTerm(event.target.value)}
                placeholder="Search audio, computing, home & more..."
                aria-label="Search audio, computing, home and more"
                className="h-11 w-full rounded-lg border border-line-strong bg-white pl-4 pr-13 text-sm text-ink placeholder:text-muted/80 transition-all focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
              />
              <button
                type="submit"
                aria-label="Search"
                className="absolute right-1 top-1 bottom-1 w-10 flex items-center justify-center rounded-md bg-accent text-white hover:bg-accent-hover active:scale-95 transition-all"
              >
                <Search className="size-4" />
              </button>
            </div>
          </form>

          {/* Right Action Icons with Sub-Labels */}
          <nav className="flex items-center gap-3 sm:gap-5" aria-label="Account and shopping">
            {isAdmin && (
              <Link
                href="/admin"
                className="hidden xl:inline-flex h-8 items-center rounded-md bg-zinc-900 px-2.5 text-xs font-semibold text-white transition-colors hover:bg-accent"
              >
                Admin Panel
              </Link>
            )}

            {/* Wishlist */}
            <Link
              href={isAuthenticated ? "/wishlist" : "/login?next=/wishlist"}
              className="group flex flex-col items-center justify-center text-ink hover:text-accent transition-colors py-0.5"
              aria-label={wishlistCount > 0 ? `Wishlist, ${wishlistCount} items` : "Wishlist"}
            >
              <div className="relative flex size-6 items-center justify-center">
                <Heart className="size-5 transition-transform group-hover:scale-110" />
                {wishlistCount > 0 && (
                  <span className="tabular absolute -right-2 -top-1.5 flex min-w-4 h-4 items-center justify-center rounded-full bg-accent px-1 text-[9px] font-bold text-white shadow-xs">
                    {wishlistCount > 99 ? "99+" : wishlistCount}
                  </span>
                )}
              </div>
              <span className="mt-1 text-[11px] font-medium text-ink group-hover:text-accent">
                Wishlist
              </span>
            </Link>

            {/* Notifications */}
            {isAuthenticated ? (
              <Link
                href="/account/notifications"
                className="group flex flex-col items-center justify-center text-ink hover:text-accent transition-colors py-0.5"
                aria-label={unreadCount > 0 ? `${unreadCount} unread notifications` : "Notifications"}
              >
                <div className="relative flex size-6 items-center justify-center">
                  <Bell className="size-5 transition-transform group-hover:scale-110" />
                  {unreadCount > 0 && (
                    <span className="tabular absolute -right-2 -top-1.5 flex min-w-4 h-4 items-center justify-center rounded-full bg-accent px-1 text-[9px] font-bold text-white shadow-xs">
                      {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                  )}
                </div>
                <span className="mt-1 text-[11px] font-medium text-ink group-hover:text-accent">
                  Notifications
                </span>
              </Link>
            ) : (
              <Link
                href="/login"
                className="group flex flex-col items-center justify-center text-ink hover:text-accent transition-colors py-0.5"
                aria-label="Notifications"
              >
                <div className="relative flex size-6 items-center justify-center">
                  <Bell className="size-5 transition-transform group-hover:scale-110" />
                </div>
                <span className="mt-1 text-[11px] font-medium text-ink group-hover:text-accent">
                  Notifications
                </span>
              </Link>
            )}

            {/* Account */}
            <Link
              href={isAuthenticated ? "/account" : "/login"}
              className="group flex flex-col items-center justify-center text-ink hover:text-accent transition-colors py-0.5"
              aria-label={isAuthenticated ? `Account, signed in as ${user?.firstName}` : "Sign in"}
            >
              <div className="relative flex size-6 items-center justify-center">
                <User className="size-5 transition-transform group-hover:scale-110" />
              </div>
              <span className="mt-1 text-[11px] font-medium text-ink group-hover:text-accent">
                {isAuthenticated ? "Account" : "Sign In"}
              </span>
            </Link>

            {/* Cart */}
            <Link
              href="/cart"
              className="group flex flex-col items-center justify-center text-ink hover:text-accent transition-colors py-0.5"
              aria-label={itemCount > 0 ? `Cart, ${itemCount} items` : "Cart, empty"}
            >
              <div className="relative flex size-6 items-center justify-center">
                <ShoppingBag className="size-5 transition-transform group-hover:scale-110" />
                <span className="tabular absolute -right-2 -top-1.5 flex min-w-4 h-4 items-center justify-center rounded-full bg-accent px-1 text-[9px] font-bold text-white shadow-xs">
                  {itemCount}
                </span>
              </div>
              <span className="mt-1 text-[11px] font-medium text-ink group-hover:text-accent">
                Cart
              </span>
            </Link>
          </nav>
        </div>
      </div>

      {/* ---------------- Sub-Header Category Navigation Rail ---------------- */}
      <div className="border-b border-line bg-surface">
        <div className="container-page">
          <div className="flex h-11 items-center justify-between">
            {/* Horizontal Categories */}
            <nav aria-label="Product categories" className="hidden lg:flex items-center gap-7 overflow-x-auto py-1">
              <Link
                href="/products"
                className={cn(
                  "relative py-2.5 text-xs font-semibold transition-colors hover:text-accent whitespace-nowrap",
                  isAllProductsActive
                    ? "text-accent after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-accent"
                    : "text-ink"
                )}
              >
                All Products
              </Link>

              {navCategories.map((category) => {
                const isActive = currentCategory === category.slug;
                return (
                  <Link
                    key={category.slug}
                    href={`/products?category=${category.slug}`}
                    className={cn(
                      "relative py-2.5 text-xs font-medium transition-colors hover:text-accent whitespace-nowrap",
                      isActive
                        ? "font-semibold text-accent after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-accent"
                        : "text-ink-soft hover:text-ink"
                    )}
                  >
                    {category.name}
                  </Link>
                );
              })}

              <Link
                href="/products"
                className="text-xs font-medium text-ink-soft hover:text-ink transition-colors whitespace-nowrap"
              >
                Brands
              </Link>
            </nav>

            {/* Deals Pill Badge */}
            <div className="hidden lg:flex items-center ml-auto">
              <Link
                href="/products?featured=true"
                className="inline-flex items-center gap-1.5 rounded-full border border-accent/40 bg-accent-soft px-3.5 py-1 text-xs font-semibold text-accent transition-all hover:bg-accent hover:text-white"
              >
                <Sparkles className="size-3.5" />
                <span>Deals</span>
              </Link>
            </div>

            {/* Mobile Search Row if screen is small */}
            <div className="flex md:hidden w-full py-1.5">
              <form onSubmit={submitSearch} role="search" className="w-full">
                <div className="relative">
                  <input
                    type="search"
                    value={term}
                    onChange={(event) => setTerm(event.target.value)}
                    placeholder="Search audio, computing, home..."
                    className="h-9 w-full rounded-md border border-line-strong bg-white pl-3 pr-10 text-xs text-ink placeholder:text-muted"
                  />
                  <button
                    type="submit"
                    className="absolute right-0.5 top-0.5 bottom-0.5 w-8 flex items-center justify-center rounded bg-accent text-white"
                  >
                    <Search className="size-3.5" />
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>

      {/* ---------------- Mobile Menu Disclosure ---------------- */}
      {menuOpen && (
        <div id="mobile-nav" className="animate-in-up border-t border-line bg-surface lg:hidden">
          <div className="container-page py-4">
            <ul className="flex flex-col divide-y divide-line">
              <li>
                <Link
                  href="/products"
                  className="flex h-11 items-center justify-between text-sm font-semibold text-ink"
                >
                  <span>All Products</span>
                  <span className="text-xs text-accent">Browse &rarr;</span>
                </Link>
              </li>
              {navCategories.map((category) => (
                <li key={category.slug}>
                  <Link
                    href={`/products?category=${category.slug}`}
                    className="flex h-11 items-center justify-between text-sm text-ink-soft hover:text-accent"
                  >
                    <span>{category.name}</span>
                  </Link>
                </li>
              ))}
              <li>
                <Link
                  href="/products?featured=true"
                  className="flex h-11 items-center justify-between text-sm font-semibold text-accent"
                >
                  <span className="flex items-center gap-1.5">
                    <Sparkles className="size-4" />
                    Special Deals
                  </span>
                </Link>
              </li>
              {isAdmin && (
                <li className="pt-2">
                  <Link
                    href="/admin"
                    className="flex h-11 items-center text-sm font-semibold text-ink"
                  >
                    Admin Dashboard &rarr;
                  </Link>
                </li>
              )}
            </ul>
          </div>
        </div>
      )}
    </header>
  );
}
