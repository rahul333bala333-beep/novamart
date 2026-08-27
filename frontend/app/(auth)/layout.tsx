import Link from "next/link";
import { Wordmark } from "@/components/brand/wordmark";

/**
 * Shell for sign in, registration and password recovery.
 *
 * Deliberately stripped back: no navigation, no category rail, no footer links.
 * These pages have exactly one job, and every extra control on them is an
 * invitation to abandon it.
 */
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col bg-canvas">
      <header className="border-b border-line bg-surface">
        <div className="container-page flex h-16 items-center">
          <Wordmark />
        </div>
      </header>

      <main id="main" className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-[26rem]">{children}</div>
      </main>

      <footer className="border-t border-line bg-surface py-5">
        <div className="container-page text-center text-[length:--text-caption] text-muted">
          <Link href="/" className="transition-colors hover:text-ink">
            Back to Nova Mart
          </Link>
        </div>
      </footer>
    </div>
  );
}
