"use client";

import { Suspense } from "react";
import { ProductListing } from "./product-listing";
import { ProductCardSkeleton } from "@/components/ui/skeleton";

export default function ProductsPage() {
  return (
    <Suspense fallback={<ListingFallback />}>
      <ProductListing />
    </Suspense>
  );
}

function ListingFallback() {
  return (
    <div className="container-page py-10">
      <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <ProductCardSkeleton key={index} />
        ))}
      </div>
    </div>
  );
}
