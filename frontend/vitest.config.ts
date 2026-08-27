import path from "node:path";
import { defineConfig } from "vitest/config";

/**
 * Vitest rather than Jest: it reuses the same transform pipeline as the build,
 * so there is no second copy of the TypeScript and JSX configuration to keep in
 * step with the first.
 *
 * `@vitejs/plugin-react` is deliberately NOT used. Its only contribution here
 * would be Fast Refresh, which tests do not need, and it pulls in a different
 * major of Vite from the one Vitest ships — two incompatible `Plugin` types in
 * one dependency tree, which breaks `tsc --noEmit`. Configuring esbuild's JSX
 * transform directly removes the dependency and the conflict together.
 */
export default defineConfig({
  esbuild: {
    // The project's tsconfig uses `jsx: preserve` because Next.js does its own
    // transform. Vitest has no such step, so the automatic runtime is selected
    // here explicitly.
    jsx: "automatic",
  },
  resolve: {
    // Mirrors the `@/*` alias in tsconfig.json so imports read identically in
    // tests and in application code.
    alias: { "@": path.resolve(__dirname, ".") },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./tests/setup.ts"],
    include: ["tests/**/*.test.{ts,tsx}"],
    restoreMocks: true,
  },
});
