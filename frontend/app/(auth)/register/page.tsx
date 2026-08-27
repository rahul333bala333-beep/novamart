"use client";

import { Suspense } from "react";
import { RegisterForm } from "./register-form";
import { Skeleton } from "@/components/ui/skeleton";

export default function RegisterPage() {
  return (
    <Suspense fallback={<Skeleton className="h-[32rem] w-full" />}>
      <RegisterForm />
    </Suspense>
  );
}
