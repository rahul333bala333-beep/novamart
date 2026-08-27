import Link from "next/link";
import { ArrowRight } from "lucide-react";

/**
 * Consistent section header.
 *
 * One component so the space above a section, the size of its title and the
 * position of its "see all" link are identical everywhere. Restyling each
 * section by hand is how a page ends up with four subtly different headings.
 */
export function SectionHeading({
  title,
  description,
  href,
  linkLabel = "See all",
}: {
  title: string;
  description?: string;
  href?: string;
  linkLabel?: string;
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3 border-b border-line pb-4">
      <div>
        <h2 className="font-[family-name:--font-display] text-[length:--text-h2] font-semibold tracking-[-0.015em] text-ink">
          {title}
        </h2>
        {description && <p className="mt-1.5 text-[length:--text-body] text-muted">{description}</p>}
      </div>
      {href && (
        <Link
          href={href}
          className="group inline-flex items-center gap-1.5 text-[length:--text-small] font-medium text-ink transition-colors hover:text-ink-soft"
        >
          {linkLabel}
          <ArrowRight
            className="size-3.5 transition-transform duration-[--duration-base] group-hover:translate-x-0.5"
            aria-hidden="true"
          />
        </Link>
      )}
    </div>
  );
}
