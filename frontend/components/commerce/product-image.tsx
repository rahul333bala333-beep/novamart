"use client";

import * as React from "react";
import { cn } from "@/lib/cn";

/**
 * Product imagery with honest loading and failure states.
 *
 * The demo catalogue is seeded with placeholder photography from an external
 * service, so a broken image is a real possibility offline or behind a strict
 * network. Rather than leaving a browser's torn-page icon in the middle of a
 * product grid, a failed load falls back to a drawn tile carrying the product's
 * initials, which reads as intentional.
 *
 * `next/image` is not used here: the seeded URLs are arbitrary remote hosts, and
 * whitelisting them in `next.config.ts` would tie the build config to demo data.
 * A plain `<img>` with explicit dimensions and lazy loading gets the same
 * layout-stability and bandwidth behaviour for this case.
 */
export function ProductImage({
  src,
  alt,
  className,
  sizes = "(min-width: 1024px) 25vw, (min-width: 640px) 33vw, 50vw",
  priority = false,
}: {
  src: string;
  alt: string;
  className?: string;
  sizes?: string;
  priority?: boolean;
}) {
  const [state, setState] = React.useState<"loading" | "ready" | "failed">("loading");

  const initials = React.useMemo(
    () =>
      alt
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((word) => word[0])
        .join("")
        .toUpperCase(),
    [alt]
  );

  return (
    <div className={cn("relative overflow-hidden bg-sunken", className)}>
      {state === "loading" && <div className="absolute inset-0 skeleton" aria-hidden="true" />}

      {state !== "failed" ? (
        /* next/image is deliberately not used here: the seeded catalogue points
           at arbitrary remote hosts, and whitelisting them in next.config.ts
           would tie the build configuration to demo data. Explicit dimensions,
           lazy loading and the skeleton above give the same layout stability
           and bandwidth behaviour for this case. */
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={src}
          alt={alt}
          sizes={sizes}
          loading={priority ? "eager" : "lazy"}
          decoding="async"
          onLoad={() => setState("ready")}
          onError={() => setState("failed")}
          className={cn(
            "size-full object-cover transition-opacity duration-[--duration-slow]",
            state === "ready" ? "opacity-100" : "opacity-0"
          )}
        />
      ) : (
        <div
          className="flex size-full items-center justify-center bg-sunken"
          role="img"
          aria-label={`${alt} (image unavailable)`}
        >
          <span className="font-[family-name:--font-display] text-2xl font-semibold text-line-strong">
            {initials || "NM"}
          </span>
        </div>
      )}
    </div>
  );
}
