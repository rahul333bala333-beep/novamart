import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /**
   * Emits a self-contained server bundle at `.next/standalone` carrying only the
   * modules actually imported. The Docker runtime stage copies that instead of
   * the whole `node_modules` tree, which is the difference between an image of
   * roughly 180MB and one well over a gigabyte.
   */
  output: "standalone",

  poweredByHeader: false,

  async rewrites() {
    const gatewayUrl = process.env.NEXT_PUBLIC_GATEWAY_URL || "http://localhost:8080";
    return [
      {
        source: "/uploads/:path*",
        destination: `${gatewayUrl}/uploads/:path*`,
      },
    ];
  },

  /**
   * Security headers applied to every response.
   *
   * These are set here rather than in the gateway because they govern the
   * document the browser renders, which the gateway never sees: it serves the
   * API, and the frontend is a separate origin.
   */
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          // Stops a browser from second-guessing a declared Content-Type, which
          // is how an uploaded text file gets executed as a script.
          { key: "X-Content-Type-Options", value: "nosniff" },
          // Refuses to be framed, which removes clickjacking.
          { key: "X-Frame-Options", value: "DENY" },
          // Sends the origin but not the path to other sites, so a referer
          // header never leaks a product or order URL.
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          // Nothing here needs a camera, a microphone or a location.
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=(), payment=()",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
