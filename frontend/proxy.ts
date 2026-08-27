import { NextResponse, type NextRequest } from "next/server";

/**
 * Coarse route protection at the edge (Next.js `proxy` convention,
 * which replaced `middleware` in Next 16).
 *
 * This checks only for the *presence* of a session cookie, and deliberately does
 * not verify the signature. Verification needs the shared secret, and putting a
 * signing key into the Next.js runtime to re-check something the gateway and
 * every service already check would widen the blast radius for no gain.
 *
 * What this buys is a redirect to the sign-in page before a protected route is
 * streamed, instead of a flash of an empty dashboard followed by a client-side
 * bounce. The real enforcement is server-side: every API call is authorised by
 * the owning service, so a forged cookie gets an unstyled page and 401s on
 * every request it tries to make.
 *
 * Role checks are not done here either. Roles live inside the token, and reading
 * them without verifying the signature would mean trusting a value the client
 * controls. `/admin` is gated by role in the layout, against the profile fetched
 * from the server.
 */
const PROTECTED_PREFIXES = ["/account", "/admin", "/checkout"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const needsSession = PROTECTED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)
  );
  if (!needsSession) return NextResponse.next();

  const hasSession = Boolean(request.cookies.get("nm_at")?.value);
  if (hasSession) return NextResponse.next();

  const signIn = new URL("/login", request.url);
  // Carrying the destination means the user lands where they were going rather
  // than on the home page having forgotten what they wanted.
  signIn.searchParams.set("next", pathname);
  return NextResponse.redirect(signIn);
}

export const config = {
  matcher: ["/account/:path*", "/admin/:path*", "/checkout/:path*"],
};
