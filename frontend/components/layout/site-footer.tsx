import Link from "next/link";
import { Wordmark } from "@/components/brand/wordmark";

/**
 * The storefront footer.
 */
export function SiteFooter() {
  return (
    <footer className="mt-20 border-t border-line bg-surface">
      <div className="container-page py-12">
        <div className="flex flex-col gap-10 md:flex-row md:justify-between">
          <div className="max-w-xs">
            <Wordmark />
            <p className="mt-3 text-xs leading-relaxed text-muted">
              A deliberately chosen range of high-end electronics and technology products. Free
              delivery across India on orders over Rs 999.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-10 sm:grid-cols-3">
            <div>
              <h2 className="text-xs font-bold uppercase tracking-wider text-ink">
                Shop
              </h2>
              <ul className="mt-3.5 flex flex-col gap-2">
                <li>
                  <Link href="/products" className="text-xs text-muted transition-colors hover:text-accent">
                    All products
                  </Link>
                </li>
                <li>
                  <Link
                    href="/products?sort=price,asc"
                    className="text-xs text-muted transition-colors hover:text-accent"
                  >
                    Best value
                  </Link>
                </li>
                <li>
                  <Link
                    href="/products?featured=true"
                    className="text-xs text-muted transition-colors hover:text-accent"
                  >
                    Featured collections
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <h2 className="text-xs font-bold uppercase tracking-wider text-ink">
                Account
              </h2>
              <ul className="mt-3.5 flex flex-col gap-2">
                <li>
                  <Link href="/account/orders" className="text-xs text-muted transition-colors hover:text-accent">
                    My orders
                  </Link>
                </li>
                <li>
                  <Link href="/account" className="text-xs text-muted transition-colors hover:text-accent">
                    Profile & Settings
                  </Link>
                </li>
                <li>
                  <Link href="/cart" className="text-xs text-muted transition-colors hover:text-accent">
                    Shopping Bag
                  </Link>
                </li>
                <li>
                  <Link href="/wishlist" className="text-xs text-muted transition-colors hover:text-accent">
                    Wishlist
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <h2 className="text-xs font-bold uppercase tracking-wider text-ink">
                Services
              </h2>
              <ul className="mt-3.5 flex flex-col gap-2">
                <li>
                  <a
                    href="http://localhost:8082/swagger-ui.html"
                    className="text-xs text-muted transition-colors hover:text-accent"
                    target="_blank"
                    rel="noreferrer"
                  >
                    API documentation
                  </a>
                </li>
                <li>
                  <a
                    href="http://localhost:8080/actuator/health"
                    className="text-xs text-muted transition-colors hover:text-accent"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Gateway health
                  </a>
                </li>
                <li>
                  <Link href="/account/orders" className="text-xs text-muted transition-colors hover:text-accent">
                    Order Tracking
                  </Link>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div className="mt-10 flex flex-col gap-2 border-t border-line pt-6 text-[11px] text-muted sm:flex-row sm:items-center sm:justify-between">
          <p>&copy; 2026 NOVA MART. All rights reserved.</p>
          <p>Curated High-End Electronics & Lifestyle Products.</p>
        </div>
      </div>
    </footer>
  );
}
