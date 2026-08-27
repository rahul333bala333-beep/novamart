import { Badge } from "@/components/ui/badge";
import type { Availability } from "@/lib/types";

/**
 * Live availability.
 *
 * Three distinct states, plus a fourth for "we could not ask". Collapsing
 * "unknown" into "out of stock" would tell the shopper something false whenever
 * inventory-service is briefly unreachable.
 */
export function StockStatus({ availability }: { availability: Availability | null }) {
  if (!availability) {
    return (
      <Badge tone="neutral">
        Checking availability
      </Badge>
    );
  }

  if (!availability.inStock) {
    return <Badge tone="danger">Out of stock</Badge>;
  }

  if (availability.availableQuantity <= 5) {
    return (
      <Badge tone="warning">
        Only {availability.availableQuantity} left
      </Badge>
    );
  }

  return <Badge tone="success">In stock</Badge>;
}
