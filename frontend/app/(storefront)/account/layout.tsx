"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Bell, Heart, LogOut, MapPin, Package, User } from "lucide-react";
import * as React from "react";
import { useAuth } from "@/lib/auth/auth-context";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/cn";

const NAV = [
  { href: "/account", label: "Profile", icon: User, exact: true },
  { href: "/account/orders", label: "My orders", icon: Package, exact: false },
  { href: "/account/wishlist", label: "Wishlist", icon: Heart, exact: false },
  { href: "/account/addresses", label: "Addresses", icon: MapPin, exact: false },
  { href: "/account/notifications", label: "Notifications", icon: Bell, exact: false },
];

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, initialising, isAuthenticated, signOut } = useAuth();

  // Middleware already redirects an unauthenticated visitor before this renders.
  // This is the second line of defence, for the case where the cookie exists but
  // the session behind it has been revoked server-side.
  React.useEffect(() => {
    if (!initialising && !isAuthenticated) router.replace("/login?next=/account");
  }, [initialising, isAuthenticated, router]);

  if (initialising || !user) {
    return (
      <div className="container-page py-10">
        <Skeleton className="h-8 w-40" />
        <Skeleton className="mt-6 h-64 w-full" />
      </div>
    );
  }

  return (
    <div className="container-page py-8 lg:py-10">
      <header className="border-b border-line pb-5">
        <h1 className="font-[family-name:--font-display] text-[length:--text-h1] font-semibold tracking-[-0.02em] text-ink">
          My account
        </h1>
        <p className="mt-1.5 text-[length:--text-body] text-muted">
          Signed in as {user.firstName} {user.lastName}
        </p>
      </header>

      <div className="mt-8 grid gap-8 lg:grid-cols-[14rem_1fr] lg:gap-12">
        <nav aria-label="Account sections">
          {/* Horizontally scrollable on mobile, a sidebar from large up. Both are
              real layouts rather than one squeezed into the other. */}
          <ul className="-mx-1 flex gap-1 overflow-x-auto pb-2 lg:mx-0 lg:flex-col lg:overflow-visible lg:pb-0">
            {NAV.map((item) => {
              const active = item.exact ? pathname === item.href : pathname.startsWith(item.href);
              return (
                <li key={item.href} className="shrink-0">
                  <Link
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "flex h-11 items-center gap-2.5 rounded-[--radius-md] px-3 text-[length:--text-body] transition-colors",
                      active
                        ? "bg-sunken font-medium text-ink"
                        : "text-ink-soft hover:bg-sunken hover:text-ink"
                    )}
                  >
                    <item.icon className="size-4 shrink-0" aria-hidden="true" />
                    {item.label}
                  </Link>
                </li>
              );
            })}
            <li className="shrink-0 lg:mt-4 lg:border-t lg:border-line lg:pt-4">
              {/* Sign out sits apart from navigation. Grouping a destructive or
                  session-ending action with ordinary links invites a mis-click. */}
              <button
                type="button"
                onClick={() => void signOut()}
                className="flex h-11 w-full cursor-pointer items-center gap-2.5 rounded-[--radius-md] px-3 text-[length:--text-body] text-ink-soft transition-colors hover:bg-danger-soft hover:text-danger"
              >
                <LogOut className="size-4 shrink-0" aria-hidden="true" />
                Sign out
              </button>
            </li>
          </ul>
        </nav>

        <div className="min-w-0">{children}</div>
      </div>
    </div>
  );
}
