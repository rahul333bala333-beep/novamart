import type { Metadata, Viewport } from "next";
import { Fraunces, Inter } from "next/font/google";
import { Providers } from "./providers";
import "./globals.css";

/**
 * Fonts are loaded through `next/font`, which self-hosts them at build time.
 * That removes a render-blocking round trip to Google, removes a third party
 * from the critical path, and means no external request carries the visitor's IP
 * on every page view.
 */
const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

const fraunces = Fraunces({
  subsets: ["latin"],
  variable: "--font-fraunces",
  display: "swap",
  axes: ["SOFT", "WONK", "opsz"],
});

export const metadata: Metadata = {
  title: {
    default: "NOVA MART - High-End Electronics & Curated Technology",
    template: "%s | NOVA MART",
  },
  description:
    "NOVA MART carries a small, deliberately chosen range of audio, computing, gaming, home and photography products. Free delivery over Rs 999 across India.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // Deliberately not setting maximumScale or userScalable. Blocking pinch-zoom
  // is a common copy-paste that makes a site unusable for anyone who needs to
  // magnify it.
  themeColor: "#faf9f7",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${inter.variable} ${fraunces.variable}`} suppressHydrationWarning>
      <body className="min-h-dvh antialiased" suppressHydrationWarning>
        {/* First tab stop on every page. Without it, a keyboard user has to tab
            through the entire header and navigation on each new page before
            reaching the content. */}
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[200] focus:rounded-[--radius-md] focus:bg-ink focus:px-4 focus:py-2 focus:text-white"
        >
          Skip to main content
        </a>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
