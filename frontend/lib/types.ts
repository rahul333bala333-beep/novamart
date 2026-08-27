/**
 * TypeScript shapes for the Nova Mart API.
 *
 * Hand-written from `api-contract/openapi.yaml` rather than generated, because
 * the generated output for this contract is a wall of `components["schemas"][...]`
 * indirection that is materially harder to read at the call site. The contract
 * remains the source of truth: if these drift, the contract wins.
 */

/* ---------------------------------------------------------------- envelope */

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Paged<T> {
  content: T[];
  page: PageMeta;
}

export interface FieldError {
  field: string;
  message: string;
}

/* -------------------------------------------------------------------- auth */

export type Role = "USER" | "ADMIN";

export interface UserProfile {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  roles: Role[];
  enabled: boolean;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export interface Address {
  id: string;
  label: string;
  recipientName: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
}

export type AddressInput = Omit<Address, "id">;

/* --------------------------------------------------------------- catalogue */

export interface Product {
  id: string;
  sku: string;
  slug: string;
  name: string;
  shortDescription: string | null;
  price: number;
  compareAtPrice: number | null;
  discountPercent: number | null;
  currency: string;
  categoryId: string;
  categoryName: string;
  categorySlug: string;
  brandId: string | null;
  brandName: string | null;
  imageUrl: string;
  ratingAverage: number;
  ratingCount: number;
  featured: boolean;
  active: boolean;
  createdAt: string;
}

export interface Availability {
  availableQuantity: number;
  inStock: boolean;
}

export interface Specification {
  label: string;
  value: string;
}

export interface ProductDetail {
  product: Product;
  description: string;
  images: string[];
  specifications: Specification[];
  /** Null when inventory-service could not be reached; the page still renders. */
  availability: Availability | null;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  productCount: number;
}

export interface Brand {
  id: string;
  name: string;
  slug: string;
  productCount: number;
}

export interface ProductQuery {
  page?: number;
  size?: number;
  search?: string;
  category?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  featured?: boolean;
  sort?: string;
}

export interface ProductInput {
  sku: string;
  name: string;
  shortDescription?: string;
  description: string;
  price: number;
  compareAtPrice?: number | null;
  categoryId: string;
  brandId?: string | null;
  imageUrl?: string;
  images?: string[];
  specifications?: Specification[];
  featured?: boolean;
  active?: boolean;
  initialStock?: number;
}

/* ---------------------------------------------------------------- reviews */

export interface ProductReview {
  id: string;
  productId: string;
  userId: string;
  userName: string;
  rating: number;
  title: string;
  comment: string;
  verifiedPurchase: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductReviewSummary {
  productId: string;
  ratingAverage: number;
  ratingCount: number;
  ratingDistribution: Record<number, number>;
}

export interface CreateReviewInput {
  rating: number;
  title: string;
  comment: string;
}

/* -------------------------------------------------------------------- cart */

export interface CartItem {
  productId: string;
  name: string;
  slug: string;
  imageUrl: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  availableQuantity: number;
  inStock: boolean;
}

export interface Cart {
  id: string;
  userId: string;
  items: CartItem[];
  subtotal: number;
  totalQuantity: number;
  currency: string;
  updatedAt: string;
}

/* ---------------------------------------------------------------- wishlist */

export interface WishlistItem {
  id: string;
  productId: string;
  name: string;
  slug: string;
  imageUrl: string;
  price: number;
  currency: string;
  availableQuantity: number;
  inStock: boolean;
  addedAt: string;
}

export interface Wishlist {
  userId: string;
  items: WishlistItem[];
  totalItems: number;
}

/* ----------------------------------------------------------------- coupons */

export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT" | "FREE_SHIPPING";

export interface Coupon {
  id: string;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscount: number | null;
  usageLimit: number | null;
  usageCount: number;
  active: boolean;
  expiresAt: string | null;
  createdAt: string;
}

export interface ValidateCouponResponse {
  valid: boolean;
  code: string | null;
  discountType: DiscountType | null;
  discountAmount: number;
  message: string;
}

export interface CreateCouponInput {
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscount?: number;
  usageLimit?: number;
  expiresAt?: string;
}

/* ------------------------------------------------------------------ orders */

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "SHIPPED"
  | "OUT_FOR_DELIVERY"
  | "DELIVERED"
  | "CANCELLED";

export type PaymentMethod = "MOCK_CARD" | "CASH_ON_DELIVERY";
export type PaymentStatus = "INITIATED" | "SUCCESS" | "FAILED" | "REFUNDED";

export interface OrderItem {
  productId: string;
  sku: string | null;
  name: string;
  slug: string | null;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface ShippingAddress {
  label: string | null;
  recipientName: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  userId: string;
  status: OrderStatus;
  items: OrderItem[];
  subtotal: number;
  deliveryFee: number;
  discount: number;
  total: number;
  currency: string;
  shippingAddress: ShippingAddress | null;
  paymentId: string | null;
  paymentStatus: string;
  paymentMethod: string;
  estimatedDeliveryDate: string | null;
  notes: string | null;
  cancelledReason: string | null;
  placedAt: string;
}

export interface OrderEvent {
  status: OrderStatus;
  note: string | null;
  occurredAt: string;
}

export interface OrderDetail {
  order: Order;
  timeline: OrderEvent[];
}

export interface CreateOrderInput {
  addressId: string;
  paymentMethod: PaymentMethod;
  notes?: string;
  couponCode?: string;
  /** Demo-only: forces the simulated gateway to decline. */
  simulateFailure?: boolean;
}

export interface DailyRevenue {
  date: string;
  revenue: number;
  orders: number;
}

export interface StatusCount {
  status: OrderStatus;
  count: number;
}

export interface OrderStats {
  totalRevenue: number;
  totalOrders: number;
  pendingOrders: number;
  cancelledOrders: number;
  averageOrderValue: number;
  revenueByDay: DailyRevenue[];
  statusBreakdown: StatusCount[];
}

/* ---------------------------------------------------------------- payments */

export interface Payment {
  id: string;
  orderId: string;
  userId: string;
  amount: number;
  currency: string;
  method: PaymentMethod;
  status: PaymentStatus;
  transactionReference: string;
  failureReason: string | null;
  createdAt: string;
  settledAt: string | null;
}

/* --------------------------------------------------------------- inventory */

export interface InventoryItem {
  productId: string;
  totalQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  reorderThreshold: number;
  inStock: boolean;
  lowStock: boolean;
  updatedAt: string;
}

/* ----------------------------------------------------------- notifications */

export type NotificationType =
  | "WELCOME"
  | "ORDER_CONFIRMATION"
  | "PAYMENT_CONFIRMATION"
  | "PAYMENT_FAILED"
  | "ORDER_SHIPPED"
  | "ORDER_DELIVERED"
  | "ORDER_CANCELLED";

export interface AppNotification {
  id: string;
  userId: string;
  type: NotificationType;
  channel: "EMAIL" | "SMS" | "IN_APP";
  recipient: string | null;
  subject: string;
  body: string;
  referenceId: string | null;
  status: "QUEUED" | "SENT" | "FAILED";
  read: boolean;
  createdAt: string;
  sentAt: string | null;
}
