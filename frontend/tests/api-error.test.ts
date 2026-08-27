import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/client";

/**
 * How the client classifies failures.
 *
 * The retry policy and every error banner in the application branch on these,
 * so a mistake here is the difference between "try again" and a silent hang.
 */
describe("ApiError", () => {
  it("recognises a validation failure", () => {
    const error = new ApiError(400, "VALIDATION_FAILED", "Request validation failed", [
      { field: "email", message: "Must be a well-formed email address" },
    ]);
    expect(error.isValidation).toBe(true);
    expect(error.fieldErrors).toHaveLength(1);
    expect(error.isRetryable).toBe(false);
  });

  it("treats 401 as an auth problem", () => {
    expect(new ApiError(401, "UNAUTHORIZED", "Sign in required").isAuth).toBe(true);
  });

  it("does not mark client errors as retryable", () => {
    // Retrying a 404 or a 409 cannot succeed; it only delays telling the user.
    expect(new ApiError(404, "PRODUCT_NOT_FOUND", "Not found").isRetryable).toBe(false);
    expect(new ApiError(409, "INSUFFICIENT_STOCK", "Out of stock").isRetryable).toBe(false);
    expect(new ApiError(403, "FORBIDDEN", "Nope").isRetryable).toBe(false);
  });

  it("marks server and network failures as retryable", () => {
    expect(new ApiError(503, "SERVICE_UNAVAILABLE", "Down").isRetryable).toBe(true);
    expect(new ApiError(500, "INTERNAL_ERROR", "Oops").isRetryable).toBe(true);
    // Status 0 is the client's own marker for "never reached the server".
    expect(new ApiError(0, "NETWORK_ERROR", "Offline").isRetryable).toBe(true);
  });

  it("carries the machine-readable code that components branch on", () => {
    const error = new ApiError(402, "PAYMENT_FAILED", "Your payment was declined.");
    expect(error.code).toBe("PAYMENT_FAILED");
    expect(error.message).toBe("Your payment was declined.");
    expect(error).toBeInstanceOf(Error);
  });
});
