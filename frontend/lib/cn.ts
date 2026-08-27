import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Merges class names, with later Tailwind utilities beating earlier ones.
 *
 * Plain string concatenation leaves `px-4 px-6` in the DOM, where which one wins
 * depends on stylesheet order rather than on the order they were written. That
 * makes a component's `className` prop unreliable for overrides, which is the
 * whole reason it exists.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
