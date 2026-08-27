"use client";

import { Suspense } from "react";
import { SignInForm } from "./sign-in-form";
import { Skeleton } from "@/components/ui/skeleton";

export default function LoginPage() {
  return (
    <Suspense fallback={<Skeleton className="h-96 w-full" />}>
      <SignInForm />
    </Suspense>
  );
}
