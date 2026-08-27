import Link from "next/link";
import { cn } from "@/lib/cn";

/**
 * The NOVA MART wordmark with premium logo badge and subtitle.
 * Matches reference design layout.
 */
export function Wordmark({
  className,
  href = "/",
  showTagline = true,
}: {
  className?: string;
  href?: string;
  showTagline?: boolean;
}) {
  return (
    <Link
      href={href}
      className={cn("group inline-flex items-center gap-3 select-none", className)}
      aria-label="NOVA MART home"
    >
      {/* Brand Icon Badge */}
      <div className="relative flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#0c0c0e] shadow-sm transition-transform duration-[--duration-base] ease-[--ease-out-quart] group-hover:scale-105">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          className="size-5.5 text-white"
          aria-hidden="true"
        >
          {/* Shopping Bag with Gold Accent */}
          <path
            d="M16 8V6a4 4 0 0 0-8 0v2"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
          <rect
            x="4"
            y="8"
            width="16"
            height="13"
            rx="3"
            stroke="currentColor"
            strokeWidth="1.8"
          />
          <circle cx="12" cy="13.5" r="1.5" fill="#c88a2e" />
        </svg>
      </div>

      {/* Brand Name & Tagline */}
      <div className="flex flex-col">
        <div className="flex items-baseline gap-1.5 leading-none">
          <span className="font-[family-name:--font-display] text-xl font-bold tracking-tight text-ink">
            NOVA
          </span>
          <span className="text-xl font-extrabold tracking-wider text-accent font-[family-name:--font-sans]">
            MART
          </span>
        </div>
        {showTagline && (
          <span className="mt-0.5 text-[10px] font-medium tracking-wide text-muted">
            Quality. Curated. For You.
          </span>
        )}
      </div>
    </Link>
  );
}
