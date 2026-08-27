import * as React from "react";

export function AdminPageHeader({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <header className="flex flex-wrap items-start justify-between gap-4 pb-6">
      <div>
        <h1 className="font-[family-name:--font-display] text-[length:--text-h1] font-semibold tracking-[-0.02em] text-ink">
          {title}
        </h1>
        {description && <p className="mt-1 text-[length:--text-body] text-muted">{description}</p>}
      </div>
      {action}
    </header>
  );
}
