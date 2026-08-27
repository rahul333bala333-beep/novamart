/**
 * One function per endpoint in the API contract.
 *
 * Components never build a URL or know a path. When the contract changes, this
 * file is the only place that has to follow, and TypeScript points at every
 * caller that needs updating.
 */

import { api } from "./client";
import type {
  Address,
  AddressInput,
  AppNotification,
  AuthTokens,
  Brand,
  Cart,
  Category,
  CreateOrderInput,
  InventoryItem,
  Order,
  OrderDetail,
  OrderStats,
  OrderStatus,
  Paged,
  Payment,
  Product,
  ProductDetail,
  ProductInput,
  ProductQuery,
  UserProfile,
} from "@/lib/types";

/* -------------------------------------------------------------------- auth */

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthTokens>("/auth/login", { email, password }, { auth: false }),

  register: (input: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phone?: string;
  }) => api.post<AuthTokens>("/auth/register", input, { auth: false }),

  logout: (refreshToken: string) => api.post<void>("/auth/logout", { refreshToken }),

  me: () => api.get<UserProfile>("/users/me"),

  updateMe: (input: { firstName: string; lastName: string; phone?: string | null }) =>
    api.put<UserProfile>("/users/me", input),

  listUsers: (query: { page?: number; size?: number; search?: string } = {}) =>
    api.get<Paged<UserProfile>>("/users", query),

  updateUserStatus: (id: string, enabled: boolean) =>
    api.put<UserProfile>(`/users/${id}/status`, { enabled }),

  updateUserRole: (id: string, roles: ("USER" | "ADMIN")[]) =>
    api.put<UserProfile>(`/users/${id}/role`, { roles }),
};

/* --------------------------------------------------------------- addresses */

export const addressApi = {
  list: () => api.get<Address[]>("/users/me/addresses"),
  create: (input: AddressInput) => api.post<Address>("/users/me/addresses", input),
  update: (id: string, input: AddressInput) =>
    api.put<Address>(`/users/me/addresses/${id}`, input),
  remove: (id: string) => api.delete<void>(`/users/me/addresses/${id}`),
};

/* --------------------------------------------------------------- catalogue */

export const catalogueApi = {
  // Public reads pass auth:false so a signed-out visitor can browse without the
  // client attaching a token it does not have.
  products: (query: ProductQuery = {}) =>
    api.get<Paged<Product>>("/products", query as Record<string, unknown>, { auth: false }),

  product: (idOrSlug: string) =>
    api.get<ProductDetail>(`/products/${idOrSlug}`, undefined, { auth: false }),

  categories: () => api.get<Category[]>("/categories", undefined, { auth: false }),

  brands: () => api.get<Brand[]>("/brands", undefined, { auth: false }),

  createProduct: (input: ProductInput) => api.post<Product>("/products", input),
  updateProduct: (id: string, input: ProductInput) => api.put<Product>(`/products/${id}`, input),
  deleteProduct: (id: string) => api.delete<void>(`/products/${id}`),
  uploadProductImage: (productId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.upload<{ imageUrl: string }>(`/products/${productId}/image`, formData);
  },

  createCategory: (input: { name: string; description?: string; imageUrl?: string }) =>
    api.post<Category>("/categories", input),
  updateCategory: (id: string, input: { name: string; description?: string; imageUrl?: string }) =>
    api.put<Category>(`/categories/${id}`, input),
  deleteCategory: (id: string) => api.delete<void>(`/categories/${id}`),
};

/* ---------------------------------------------------------------- reviews */

export const reviewApi = {
  listByProduct: (productId: string, query: { page?: number; size?: number } = {}) =>
    api.get<Paged<import("@/lib/types").ProductReview>>(`/products/${productId}/reviews`, query, { auth: false }),

  getSummary: (productId: string) =>
    api.get<import("@/lib/types").ProductReviewSummary>(`/products/${productId}/reviews/summary`, undefined, { auth: false }),

  create: (productId: string, input: import("@/lib/types").CreateReviewInput) =>
    api.post<import("@/lib/types").ProductReview>(`/products/${productId}/reviews`, input),

  update: (id: string, input: import("@/lib/types").CreateReviewInput) =>
    api.put<import("@/lib/types").ProductReview>(`/reviews/${id}`, input),

  delete: (id: string) => api.delete<void>(`/reviews/${id}`),
};

/* -------------------------------------------------------------------- cart */

export const cartApi = {
  get: () => api.get<Cart>("/cart"),
  addItem: (productId: string, quantity: number) =>
    api.post<Cart>("/cart/items", { productId, quantity }),
  setQuantity: (productId: string, quantity: number) =>
    api.put<Cart>(`/cart/items/${productId}`, { quantity }),
  removeItem: (productId: string) => api.delete<Cart>(`/cart/items/${productId}`),
  clear: () => api.delete<void>("/cart"),
};

/* ---------------------------------------------------------------- wishlist */

export const wishlistApi = {
  get: () => api.get<import("@/lib/types").Wishlist>("/wishlist"),
  addItem: (productId: string) =>
    api.post<import("@/lib/types").Wishlist>("/wishlist", { productId }),
  removeItem: (productId: string) =>
    api.delete<import("@/lib/types").Wishlist>(`/wishlist/${productId}`),
  moveToCart: (productId: string) =>
    api.post<void>(`/wishlist/${productId}/move-to-cart`, {}),
};

/* ----------------------------------------------------------------- coupons */

export const couponApi = {
  validate: (code: string, orderSubtotal: number) =>
    api.post<import("@/lib/types").ValidateCouponResponse>("/coupons/validate", { code, orderSubtotal }, { auth: false }),
  list: () => api.get<import("@/lib/types").Coupon[]>("/coupons"),
  create: (input: import("@/lib/types").CreateCouponInput) =>
    api.post<import("@/lib/types").Coupon>("/coupons", input),
  update: (id: string, input: import("@/lib/types").CreateCouponInput) =>
    api.put<import("@/lib/types").Coupon>(`/coupons/${id}`, input),
  delete: (id: string) => api.delete<void>(`/coupons/${id}`),
};

/* ------------------------------------------------------------------ orders */

export const orderApi = {
  /**
   * Places an order.
   *
   * The idempotency key is generated per attempt and sent as a header, so a
   * double-clicked button or a retry after a dropped response returns the
   * original order instead of charging twice.
   */
  create: (input: CreateOrderInput, idempotencyKey: string) =>
    api.post<Order>("/orders", input, { idempotencyKey }),

  list: (query: { page?: number; size?: number; status?: OrderStatus; userId?: string } = {}) =>
    api.get<Paged<Order>>("/orders", query),

  detail: (id: string) => api.get<OrderDetail>(`/orders/${id}`),

  cancel: (id: string, reason?: string) => api.put<Order>(`/orders/${id}/cancel`, { reason }),

  updateStatus: (id: string, status: OrderStatus, note?: string) =>
    api.put<Order>(`/orders/${id}/status`, { status, note }),

  stats: () => api.get<OrderStats>("/orders/stats"),
};

/* ---------------------------------------------------------------- payments */

export const paymentApi = {
  list: (query: { page?: number; size?: number; status?: string } = {}) =>
    api.get<Paged<Payment>>("/payments", query),
  byOrder: (orderId: string) => api.get<Payment>(`/payments/by-order/${orderId}`),
};

/* --------------------------------------------------------------- inventory */

export const inventoryApi = {
  get: (productId: string) =>
    api.get<InventoryItem>(`/inventory/${productId}`, undefined, { auth: false }),
  list: (query: { page?: number; size?: number; lowStockOnly?: boolean } = {}) =>
    api.get<Paged<InventoryItem>>("/inventory", query),
  update: (productId: string, totalQuantity: number, reorderThreshold?: number) =>
    api.put<InventoryItem>(`/inventory/${productId}`, { totalQuantity, reorderThreshold }),
};

/* ----------------------------------------------------------- notifications */

export const notificationApi = {
  list: (query: { page?: number; size?: number; type?: string } = {}) =>
    api.get<Paged<AppNotification>>("/notifications", query),
  unreadCount: () => api.get<number>("/notifications/unread-count"),
  markRead: (id: string) => api.put<AppNotification>(`/notifications/${id}/read`, {}),
  markAllRead: () => api.put<void>("/notifications/read-all", {}),
};
