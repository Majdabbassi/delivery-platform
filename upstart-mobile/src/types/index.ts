export enum UserRole {
  SUPER_ADMIN = 'SUPER_ADMIN',
  VENDOR_OWNER = 'VENDOR_OWNER',
  DELIVERY_OWNER = 'DELIVERY_OWNER',
  CLIENT = 'CLIENT',
  DRIVER = 'DRIVER',
}

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  isEnabled: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
  timestamp?: string;
  message?: string;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  ASSIGNED = 'ASSIGNED',
  CONFIRMED = 'CONFIRMED',
  IN_PROGRESS = 'IN_PROGRESS',
  PICKED_UP = 'PICKED_UP',
  IN_TRANSIT = 'IN_TRANSIT',
  DELIVERED = 'DELIVERED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  FAILED = 'FAILED',
}

export enum OrderPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}

export interface Order {
  id: number;
  orderNumber: string;
  trackingNumber: string;
  vendorCompanyId?: number;
  vendorCompanyName?: string;
  deliveryCompanyId?: number;
  deliveryCompanyName?: string;
  partnershipId?: number;
  customerUserId?: number;
  customerName?: string;
  driverPersonId?: number;
  driverName?: string;
  pickupAddress: string;
  deliveryAddress: string;
  pickupLatitude?: number;
  pickupLongitude?: number;
  deliveryLatitude?: number;
  deliveryLongitude?: number;
  orderAmount: number;
  deliveryFee?: number;
  totalAmount?: number;
  status: OrderStatus;
  priority: OrderPriority;
  description?: string;
  specialInstructions?: string;
  estimatedDeliveryTime?: string;
  actualPickupTime?: string;
  actualDeliveryTime?: string;
  scheduledPickupTime?: string;
  scheduledDeliveryTime?: string;
  distanceKm?: number;
  weightKg?: number;
  isFragile?: boolean;
  requiresSignature?: boolean;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderRealtimeEvent {
  type: string;
  orderId: number;
  orderNumber?: string;
  trackingNumber?: string;
  status?: OrderStatus;
  driverPersonId?: number;
  driverName?: string;
  latitude?: number;
  longitude?: number;
  speedKmh?: number;
  timestamp?: string;
}