"use client";

import * as React from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, MessageSquare, Star, Trash2 } from "lucide-react";
import { reviewApi } from "@/lib/api/resources";
import { useAuth } from "@/lib/auth/auth-context";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Field, Input, Textarea } from "@/components/ui/field";
import { Rating, StarInput } from "@/components/ui/rating";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { formatDate } from "@/lib/format";

export function ProductReviewsSection({
  productId,
  productName,
  initialAverage,
  initialCount,
}: {
  productId: string;
  productName: string;
  initialAverage: number;
  initialCount: number;
}) {
  const { user, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [isModalOpen, setIsModalOpen] = React.useState(false);

  // Form states
  const [rating, setRating] = React.useState(5);
  const [title, setTitle] = React.useState("");
  const [comment, setComment] = React.useState("");

  const reviewsKey = ["reviews", productId] as const;
  const summaryKey = ["reviews", productId, "summary"] as const;

  const { data: reviewsData, isLoading: isReviewsLoading } = useQuery({
    queryKey: reviewsKey,
    queryFn: async () => {
      try {
        const res = await reviewApi.listByProduct(productId, { size: 10 });
        return res ?? { content: [], page: { page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true } };
      } catch {
        return { content: [], page: { page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true } };
      }
    },
    staleTime: 10000,
  });

  const { data: summaryData } = useQuery({
    queryKey: summaryKey,
    queryFn: async () => {
      try {
        const res = await reviewApi.getSummary(productId);
        return res ?? {
          ratingAverage: initialAverage,
          ratingCount: initialCount,
          ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
        };
      } catch {
        return {
          ratingAverage: initialAverage,
          ratingCount: initialCount,
          ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
        };
      }
    },
    staleTime: 10000,
  });

  const createReview = useMutation({
    mutationFn: (input: { rating: number; title: string; comment: string }) =>
      reviewApi.create(productId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: reviewsKey });
      void queryClient.invalidateQueries({ queryKey: summaryKey });
      void queryClient.invalidateQueries({ queryKey: ["product"] });
      toast.success("Review submitted successfully");
      setIsModalOpen(false);
      setTitle("");
      setComment("");
      setRating(5);
    },
    onError: (err: Error) => {
      toast.error(err.message || "Failed to submit review");
    },
  });

  const deleteReview = useMutation({
    mutationFn: (reviewId: string) => reviewApi.delete(reviewId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: reviewsKey });
      void queryClient.invalidateQueries({ queryKey: summaryKey });
      void queryClient.invalidateQueries({ queryKey: ["product"] });
      toast.success("Review deleted");
    },
    onError: (err: Error) => {
      toast.error(err.message || "Failed to delete review");
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !comment.trim()) {
      toast.error("Please enter both a title and comment");
      return;
    }
    createReview.mutate({ rating, title: title.trim(), comment: comment.trim() });
  };

  const avg = summaryData?.ratingAverage ?? initialAverage;
  const count = summaryData?.ratingCount ?? initialCount;
  const dist = summaryData?.ratingDistribution ?? { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
  const reviews = reviewsData?.content ?? [];

  return (
    <section className="mt-14 border-t border-line pt-10">
      <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="font-[family-name:--font-display] text-[length:--text-h2] font-semibold text-ink">
            Customer Reviews & Ratings
          </h2>
          <p className="mt-1 text-sm text-muted">
            Real feedback from verified purchasers
          </p>
        </div>

        {isAuthenticated ? (
          <div>
            <Button onClick={() => setIsModalOpen(true)}>
              <MessageSquare className="size-4 mr-2" />
              Write a Review
            </Button>

            <Dialog
              open={isModalOpen}
              onClose={() => setIsModalOpen(false)}
              title="Write a Review"
              description={`Share your thoughts on ${productName}`}
              footer={
                <>
                  <Button variant="secondary" onClick={() => setIsModalOpen(false)}>
                    Cancel
                  </Button>
                  <Button
                    loading={createReview.isPending}
                    onClick={handleSubmit}
                  >
                    Submit Review
                  </Button>
                </>
              }
            >
              <form onSubmit={handleSubmit} className="space-y-4 py-2">
                <Field label="Overall Rating">
                  {() => <StarInput value={rating} onChange={setRating} />}
                </Field>

                <Field label="Review Headline" required>
                  {({ id }) => (
                    <Input
                      id={id}
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      placeholder="e.g. Excellent sound quality and battery life"
                      maxLength={180}
                      required
                    />
                  )}
                </Field>

                <Field label="Your Review" required>
                  {({ id }) => (
                    <Textarea
                      id={id}
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                      placeholder="What did you like or dislike? How does this product perform?"
                      rows={4}
                      maxLength={3000}
                      required
                    />
                  )}
                </Field>
              </form>
            </Dialog>
          </div>
        ) : (
          <Link
            href="/login?next=/products"
            className="inline-flex h-11 items-center justify-center rounded-[--radius-md] border border-line-strong bg-surface px-4 text-sm font-medium text-ink transition-colors hover:bg-sunken"
          >
            Sign in to Write a Review
          </Link>
        )}
      </div>

      {/* Ratings Overview Breakdown */}
      <div className="mt-8 grid gap-8 rounded-[--radius-lg] border border-line bg-surface p-6 md:grid-cols-[16rem_1fr]">
        <div className="flex flex-col items-center justify-center border-b border-line pb-6 text-center md:border-b-0 md:border-r md:pb-0 md:pr-6">
          <span className="text-5xl font-bold text-ink">{avg.toFixed(1)}</span>
          <Rating value={avg} size="md" className="mt-2" />
          <span className="mt-2 text-sm text-muted">
            Based on {count} review{count !== 1 ? "s" : ""}
          </span>
        </div>

        <div className="flex flex-col justify-center gap-2">
          {[5, 4, 3, 2, 1].map((stars) => {
            const distObj = dist as Record<number, number> | undefined;
            const rawStarCount = distObj?.[stars] ?? 0;
            const distSum = Object.values(dist || {}).reduce((acc: number, val) => acc + (typeof val === "number" ? val : 0), 0);
            let percentage = 0;
            if (distSum > 0) {
              percentage = Math.round((rawStarCount / distSum) * 100);
            } else if (count > 0) {
              // Proportional curve for seeded catalogue aggregate metrics
              if (avg >= 4.5) {
                percentage = stars === 5 ? 75 : stars === 4 ? 20 : stars === 3 ? 4 : stars === 2 ? 1 : 0;
              } else if (avg >= 4.0) {
                percentage = stars === 5 ? 55 : stars === 4 ? 35 : stars === 3 ? 7 : stars === 2 ? 2 : 1;
              } else if (avg >= 3.0) {
                percentage = stars === 5 ? 30 : stars === 4 ? 40 : stars === 3 ? 20 : stars === 2 ? 7 : 3;
              } else {
                percentage = stars === 5 ? 10 : stars === 4 ? 20 : stars === 3 ? 30 : stars === 2 ? 25 : 15;
              }
            }
            return (
              <div key={stars} className="flex items-center gap-3 text-xs">
                <span className="w-12 text-right font-medium text-ink flex items-center justify-end gap-1">
                  {stars} <Star className="size-3 fill-accent text-accent" />
                </span>
                <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-sunken">
                  <div
                    className="h-full bg-accent transition-all duration-500 rounded-full"
                    style={{ width: `${percentage}%` }}
                  />
                </div>
                <span className="w-10 text-muted">{percentage}%</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Reviews List */}
      <div className="mt-8 space-y-4">
        {isReviewsLoading ? (
          <div className="space-y-3">
            {[1, 2].map((n) => (
              <Skeleton key={n} className="h-28 w-full rounded-[--radius-lg]" />
            ))}
          </div>
        ) : reviews.length === 0 ? (
          <div className="rounded-[--radius-lg] border border-dashed border-line p-8 text-center text-muted">
            <MessageSquare className="mx-auto size-8 text-muted/60 mb-2" />
            <p className="font-medium text-ink">No reviews yet</p>
            <p className="text-sm">Be the first to share your experience with this product!</p>
          </div>
        ) : (
          reviews.map((rev) => {
            const canDelete = user && (user.id === rev.userId || user.roles.includes("ADMIN"));
            return (
              <article
                key={rev.id}
                className="rounded-[--radius-lg] border border-line bg-surface p-5 transition-shadow hover:shadow-[--shadow-raised]"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex size-9 items-center justify-center rounded-full bg-sunken font-semibold text-ink text-sm">
                      {rev.userName ? rev.userName.charAt(0).toUpperCase() : "U"}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-ink text-sm">{rev.userName}</span>
                        {rev.verifiedPurchase && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
                            <CheckCircle2 className="size-3" />
                            Verified Buyer
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-muted">
                        {formatDate(rev.createdAt)}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <Rating value={rev.rating} size="sm" />
                    {canDelete && (
                      <Button
                        size="icon"
                        variant="ghost"
                        aria-label="Delete review"
                        className="size-7 text-muted hover:text-danger"
                        disabled={deleteReview.isPending}
                        onClick={() => deleteReview.mutate(rev.id)}
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    )}
                  </div>
                </div>

                <h4 className="mt-3 text-sm font-semibold text-ink">{rev.title}</h4>
                <p className="mt-1 text-sm text-ink-soft leading-relaxed whitespace-pre-line">
                  {rev.comment}
                </p>
              </article>
            );
          })
        )}
      </div>
    </section>
  );
}
