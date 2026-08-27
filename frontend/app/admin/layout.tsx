"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Bell,
  CreditCard,
  LayoutDashboard,
  Menu,
  Package,
  Percent,
  ShoppingCart,
  Store,
  Tags,
  Users,
  Warehouse,
  X,
} from "lucide-react";
import * as React from "react";
import { Wordmark } from "@/components/brand/wordmark";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth/auth-context";
import { cn } from "@/lib/cn";

const NAV = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard, exact: true },
  { href: "/admin/orders", label: "Orders", icon: ShoppingCart, exact: false },
  { href: "/admin/products", label: "Products", icon: Package, exact: false },
  { href: "/admin/categories", label: "Categories", icon: Tags, exact: false },
  { href: "/admin/coupons", label: "Coupons", icon: Percent, exact: false },
  { href: "/admin/inventory", label: "Inventory", icon: Warehouse, exact: false },
  { href: "/admin/payments", label: "Payments", icon: CreditCard, exact: false },
  { href: "/admin/users", label: "Customers", icon: Users, exact: false },
  { href: "/admin/notifications", label: "Notifications", icon: Bell, exact: false },
];

/**
 * Back-office shell.
 *
 * Visually distinct from the storefront on purpose: a denser layout, a permanent
 * sidebar and no marketing chrome. An administrator should never be uncertain
 * about which side of the product they are looking at.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, initialising, isAuthenticated, isAdmin } = useAuth();
  const [navOpen, setNavOpen] = React.useState(false);

  React.useEffect(() => {
    if (!initialising && !isAuthenticated) router.replace("/login?next=/admin");
  }, [initialising, isAuthenticated, router]);

  /**
   * Close the menu whenever the route changes.
   *
   * Adjusting state during render, comparing against the previous path, rather
   * than in an effect. React re-runs the component immediately without
   * committing the intermediate result, so there is no extra paint and no
   * cascading render — which is exactly what `setState` inside `useEffect`
   * causes here.
   */
  const [lastPath, setLastPath] = React.useState(pathname);
  if (pathname !== lastPath) {
    setLastPath(pathname);
    setNavOpen(false);
  }

  if (initialising || !user) {
    return (
      <div className="container-page py-10">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-6 h-64 w-full" />
      </div>
    );
  }

  /*
   * The role check happens against the profile fetched from the server, not
   * against a claim read out of the token in the browser. Even so, this is a
   * usability guard rather than a security boundary: every admin endpoint is
   * independently authorised by its owning service, so a shopper who forced
   * their way to this route would see an interface where every request returns
   * 403.
   */
  if (!isAdmin) {
    return (
      <div className="container-page py-20">
        <EmptyState
          title="Administrator access required"
          description="This area is limited to staff accounts. If you believe you should have access, contact your administrator."
          action={
            <Link
              href="/"
              className="inline-flex h-11 items-center rounded-[--radius-md] bg-ink px-5 text-[length:--text-body] font-medium text-white transition-colors hover:bg-ink/90"
            >
              Back to the shop
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="flex min-h-dvh flex-col bg-canvas lg:flex-row">
      {/* ------------------------------------------------------- mobile bar */}
      <header className="flex h-14 items-center gap-3 border-b border-line bg-surface px-4 lg:hidden">
        <button
          type="button"
          onClick={() => setNavOpen((open) => !open)}
          className="-ml-2 flex size-10 items-center justify-center rounded-[--radius-md] text-ink"
          aria-expanded={navOpen}
          aria-controls="admin-nav"
          aria-label={navOpen ? "Close menu" : "Open menu"}
        >
          {navOpen ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
        <span className="font-[family-name:--font-display] text-[length:--text-lead] font-semibold text-ink">
          Nova Mart Admin
        </span>
      </header>

      {/* ---------------------------------------------------------- sidebar */}
      <aside
        id="admin-nav"
        className={cn(
          "border-line bg-surface lg:sticky lg:top-0 lg:h-dvh lg:w-60 lg:shrink-0 lg:border-r",
          navOpen ? "block border-b" : "hidden lg:block"
        )}
      >
        <div className="hidden h-16 items-center border-b border-line px-5 lg:flex">
          <Wordmark href="/admin" />
        </div>

        <nav aria-label="Admin sections" className="p-3">
          <ul className="flex flex-col gap-0.5">
            {NAV.map((item) => {
              const active = item.exact ? pathname === item.href : pathname.startsWith(item.href);
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "flex h-10 items-center gap-2.5 rounded-[--radius-md] px-3 text-[length:--text-body] transition-colors",
                      active
                        ? "bg-ink text-white"
                        : "text-ink-soft hover:bg-sunken hover:text-ink"
                    )}
                  >
                    <item.icon className="size-4 shrink-0" aria-hidden="true" />
                    {item.label}
                  </Link>
                </li>
              );
            })}
          </ul>

          <div className="mt-4 border-t border-line pt-3">
            <Link
              href="/"
              className="flex h-10 items-center gap-2.5 rounded-[--radius-md] px-3 text-[length:--text-body] text-ink-soft transition-colors hover:bg-sunken hover:text-ink"
            >
              <Store className="size-4 shrink-0" aria-hidden="true" />
              View storefront
            </Link>
          </div>

          <div className="mt-4 rounded-[--radius-md] bg-sunken px-3 py-2.5">
            <p className="text-[length:--text-caption] text-muted">Signed in as</p>
            <p className="truncate text-[length:--text-small] font-medium text-ink">{user.email}</p>
          </div>
        </nav>
      </aside>

      <main id="main" className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        {children}
      </main>
    </div>
  );
}
