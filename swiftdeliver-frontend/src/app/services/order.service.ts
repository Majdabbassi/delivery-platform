import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';
import { OrderRealtimeEvent } from './realtime.service';

// Enums
export enum OrderStatus {
  PENDING = 'PENDING',
  OPEN_FOR_BID = 'OPEN_FOR_BID',
  ASSIGNED = 'ASSIGNED',
  CONFIRMED = 'CONFIRMED',
  IN_PROGRESS = 'IN_PROGRESS',
  PICKED_UP = 'PICKED_UP',
  IN_TRANSIT = 'IN_TRANSIT',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  FAILED = 'FAILED'
}

export enum OrderPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export enum OrderType {
  MARKETPLACE = 'MARKETPLACE',
  GENERAL_DELIVERY = 'GENERAL_DELIVERY'
}

export enum RoutingMode {
  DIRECT = 'DIRECT',
  OPEN_BID = 'OPEN_BID'
}

export enum PricingMode {
  FIXED = 'FIXED',
  MIN_MAX = 'MIN_MAX'
}

// DTOs and Interfaces
export interface CreateOrderDTO {
  vendorCompanyId: number;
  customerUserId: number;
  deliveryCompanyId?: number;
  partnershipId?: number;
  driverPersonId?: number;

  // Two order types: marketplace (products) and general delivery.
  orderType?: OrderType;
  routingMode?: RoutingMode;
  pricingMode?: PricingMode;

  // Sender / recipient (used for general delivery).
  senderName?: string;
  senderPhone?: string;
  recipientName?: string;
  recipientPhone?: string;

  // Proposed pricing (min/max) for general-delivery orders.
  proposedMinAmount?: number;
  proposedMaxAmount?: number;
  
  // Address (Required)
  pickupAddress: string;
  deliveryAddress: string;
  pickupLatitude?: number;
  pickupLongitude?: number;
  deliveryLatitude?: number;
  deliveryLongitude?: number;
  
  // Order Details
  description?: string;
  orderValue: number;
  deliveryFee?: number;
  estimatedDistance?: number;
  estimatedDurationMinutes?: number;
  
  // Scheduling
  priority?: OrderPriority;
  scheduledPickupTime?: string;
  estimatedDeliveryTime?: string;
  
  // Additional
  specialInstructions?: string;
  notes?: string;
  customerPhone?: string;
  customerEmail?: string;
}

export interface UpdateOrderDTO {
  deliveryCompanyId?: number;
  partnershipId?: number;
  driverPersonId?: number;
  
  // Address updates (before pickup)
  pickupAddress?: string;
  deliveryAddress?: string;
  pickupLatitude?: number;
  pickupLongitude?: number;
  deliveryLatitude?: number;
  deliveryLongitude?: number;
  
  // Order details
  description?: string;
  orderValue?: number;
  deliveryFee?: number;
  estimatedDistance?: number;
  estimatedDurationMinutes?: number;
  
  // Status & Priority
  status?: OrderStatus;
  priority?: OrderPriority;
  
  // Timestamps
  scheduledPickupTime?: string;
  actualPickupTime?: string;
  estimatedDeliveryTime?: string;
  actualDeliveryTime?: string;
  
  // Additional
  specialInstructions?: string;
  notes?: string;
  cancellationReason?: string;
  customerPhone?: string;
  customerEmail?: string;
}

export interface OrderDTO {
  id: number;
  orderNumber: string;
  trackingNumber: string;

  // Order type & routing
  orderType: OrderType;
  routingMode: RoutingMode;

  // Sender / recipient
  senderName?: string;
  senderPhone?: string;
  recipientName?: string;
  recipientPhone?: string;

  // Proposed pricing
  proposedMinAmount?: number;
  proposedMaxAmount?: number;
  
  // Company Information
  vendorCompanyId: number;
  vendorCompanyName: string;
  deliveryCompanyId?: number;
  deliveryCompanyName?: string;
  partnershipId?: number;
  
  // Customer & Driver
  customerUserId: number;
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  driverPersonId?: number;
  driverName?: string;
  driverPhone?: string;
  
  // Addresses
  pickupAddress: string;
  deliveryAddress: string;
  pickupLatitude?: number;
  pickupLongitude?: number;
  deliveryLatitude?: number;
  deliveryLongitude?: number;
  
  // Order Details
  description?: string;
  orderValue: number;
  deliveryFee: number;
  totalAmount: number;
  estimatedDistance?: number;
  estimatedDurationMinutes?: number;
  
  // Status & Priority
  status: OrderStatus;
  priority: OrderPriority;
  
  // Timestamps
  orderDate: string;
  scheduledPickupTime?: string;
  actualPickupTime?: string;
  estimatedDeliveryTime?: string;
  actualDeliveryTime?: string;
  createdAt: string;
  updatedAt: string;
  
  // Rating & Review
  customerRating?: number;
  customerReview?: string;
  vendorRating?: number;
  vendorReview?: string;
  
  // Additional
  specialInstructions?: string;
  cancellationReason?: string;
  notes?: string;
  
  // Calculated Fields
  isOverdue: boolean;
  isAssigned: boolean;
  isCompleted: boolean;
  isCancelled: boolean;
  durationMinutes?: number;
  statusDisplayName: string;
  priorityDisplayName: string;
}

export interface OrderRatingDTO {
  orderId: number;
  
  // Main Ratings (1.0 - 5.0)
  customerRating?: number;
  customerReview?: string;
  vendorRating?: number;
  vendorReview?: string;
  
  // Detailed Ratings (1.0 - 5.0)
  deliverySpeedRating?: number;
  communicationRating?: number;
  professionalismRating?: number;
  packageConditionRating?: number;
  
  // Additional Feedback
  additionalComments?: string;
  wouldRecommend?: boolean;
  reportedIssue?: boolean;
  issueDescription?: string;
}

export interface OrderStats {
  totalCount: number;
  revenue: number;
  averageRating: number;
}


@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly baseUrl = `${API_BASE_URL}/orders`;
  private ordersSubject = new BehaviorSubject<OrderDTO[]>([]);
  public orders$ = this.ordersSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Order CRUD Operations
  createOrder(orderData: CreateOrderDTO): Observable<OrderDTO> {
    // The backend accepts the Order entity shape (nested references), not flat ids.
    const payload: any = {
      orderType: orderData.orderType || OrderType.MARKETPLACE,
      routingMode: orderData.routingMode || RoutingMode.OPEN_BID,
      pricingMode: orderData.pricingMode || PricingMode.FIXED,
      senderName: orderData.senderName,
      senderPhone: orderData.senderPhone,
      recipientName: orderData.recipientName,
      recipientPhone: orderData.recipientPhone,
      proposedMinAmount: orderData.proposedMinAmount,
      proposedMaxAmount: orderData.proposedMaxAmount,
      vendorCompany: orderData.vendorCompanyId ? { id: orderData.vendorCompanyId } : undefined,
      customerUser: orderData.customerUserId ? { id: orderData.customerUserId } : {},
      pickupAddress: orderData.pickupAddress,
      deliveryAddress: orderData.deliveryAddress,
      pickupLatitude: orderData.pickupLatitude,
      pickupLongitude: orderData.pickupLongitude,
      deliveryLatitude: orderData.deliveryLatitude,
      deliveryLongitude: orderData.deliveryLongitude,
      description: orderData.description,
      orderAmount: orderData.orderValue,
      deliveryFee: orderData.deliveryFee ?? 0,
      priority: orderData.priority || OrderPriority.NORMAL,
      specialInstructions: orderData.specialInstructions,
      scheduledPickupTime: orderData.scheduledPickupTime,
      estimatedDeliveryTime: orderData.estimatedDeliveryTime,
      notes: orderData.notes
    };
    return this.http.post<OrderDTO>(this.baseUrl, payload).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  getMyOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/my`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getOrderById(id: number): Observable<OrderDTO> {
    return this.http.get<OrderDTO>(`${this.baseUrl}/${id}`).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  getOrderByOrderNumber(orderNumber: string): Observable<OrderDTO> {
    return this.http.get<OrderDTO>(`${this.baseUrl}/order-number/${orderNumber}`).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  getOrderByTrackingNumber(trackingNumber: string): Observable<OrderDTO> {
    // Auth-aware tracking - the interceptor attaches the JWT when signed in
    return this.http.get<OrderDTO>(`${this.baseUrl}/tracking/${trackingNumber}`).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  getTrackingEvent(orderId: number): Observable<OrderRealtimeEvent | null> {
    return this.http.get<OrderRealtimeEvent>(`${API_BASE_URL}/tracking/orders/${orderId}/location`, {
      observe: 'response'
    }).pipe(
      map(response => response.body)
    );
  }

  updateOrderLocation(orderId: number, latitude: number, longitude: number, speedKmh?: number): Observable<OrderDTO> {
    const body = { latitude, longitude, ...(speedKmh != null ? { speedKmh } : {}) };
    return this.http.post<OrderDTO>(`${API_BASE_URL}/tracking/orders/${orderId}/location`, body).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  getAllOrders(page: number = 0, size: number = 20): Observable<PaginatedResponse<OrderDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PaginatedResponse<OrderDTO>>(this.baseUrl, {
      params
    }).pipe(
      map(response => ({
        ...response,
        content: response.content.map((o: any) => this.mapOrderEntity(o))
      }))
    );
  }

  updateOrder(id: number, orderData: UpdateOrderDTO): Observable<OrderDTO> {
    return this.http.put<OrderDTO>(`${this.baseUrl}/${id}`, orderData).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  deleteOrder(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // Order Status & Assignment Management
  updateOrderStatus(id: number, status: OrderStatus): Observable<OrderDTO> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/status`, null, {
      params
    }).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  assignDeliveryCompany(id: number, deliveryCompanyId: number): Observable<OrderDTO> {
    const params = new HttpParams().set('deliveryCompanyId', deliveryCompanyId.toString());
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/assign-delivery-company`, null, {
      params
    }).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  assignPartnership(id: number, partnershipId: number): Observable<OrderDTO> {
    const params = new HttpParams().set('partnershipId', partnershipId.toString());
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/assign-partnership`, null, {
      params
    }).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  addRatingAndReview(id: number, rating: number, review?: string): Observable<OrderDTO> {
    let params = new HttpParams()
      .set('rating', rating.toString());
    if (review) {
      params = params.set('review', review);
    }
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/rating`, null, {
      params
    }).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  // Alias for backward compatibility
  addOrderRating(id: number, rating: number, review?: string): Observable<OrderDTO> {
    return this.addRatingAndReview(id, rating, review);
  }

  cancelOrder(id: number, cancellationReason?: string): Observable<OrderDTO> {
    const params = new HttpParams().set('cancellationReason', cancellationReason || 'User requested cancellation');
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/cancel`, null, {
      params
    }).pipe(
      map(raw => this.mapOrderEntity(raw))
    );
  }

  // Query & Filtering Operations
  getOrdersByVendorCompany(vendorCompanyId: number): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/vendor-company/${vendorCompanyId}`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getOrdersByDeliveryCompany(deliveryCompanyId: number): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/delivery-company/${deliveryCompanyId}`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getOrdersByPartnership(partnershipId: number): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/partnership/${partnershipId}`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getOrdersByStatus(status: OrderStatus): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/status/${status}`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getPendingUnassignedOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/pending-unassigned`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getOpenForBidOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/open-for-bid`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getOverdueOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/overdue`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  getActiveUrgentOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/urgent`).pipe(
      map((orders: any[]) => orders.map(o => this.mapOrderEntity(o)))
    );
  }

  // Alias for backward compatibility
  getUrgentOrders(): Observable<OrderDTO[]> {
    return this.getActiveUrgentOrders();
  }

  // Statistics & Analytics
  countOrdersByVendorCompany(vendorCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/vendor-company/${vendorCompanyId}`);
  }

  countOrdersByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/delivery-company/${deliveryCompanyId}`);
  }

  countOrdersByStatus(status: OrderStatus): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/status/${status}`);
  }

  getTotalRevenueByVendorCompany(vendorCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/revenue/vendor-company/${vendorCompanyId}`);
  }

  getTotalDeliveryRevenueByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/revenue/delivery-company/${deliveryCompanyId}`);
  }

  getAverageRatingByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/rating/delivery-company/${deliveryCompanyId}`);
  }

  // Validation Methods
  existsByOrderNumber(orderNumber: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/order-number/${orderNumber}`);
  }

  existsByTrackingNumber(trackingNumber: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/tracking-number/${trackingNumber}`);
  }

  // Utility Methods
  getStatusDisplayName(status: OrderStatus): string {
    const statusNames: { [key in OrderStatus]: string } = {
      [OrderStatus.PENDING]: 'Pending',
      [OrderStatus.OPEN_FOR_BID]: 'Open for Bids',
      [OrderStatus.ASSIGNED]: 'Assigned',
      [OrderStatus.CONFIRMED]: 'Confirmed',
      [OrderStatus.IN_PROGRESS]: 'In Progress',
      [OrderStatus.PICKED_UP]: 'Picked Up',
      [OrderStatus.IN_TRANSIT]: 'In Transit',
      [OrderStatus.DELIVERED]: 'Delivered',
      [OrderStatus.CANCELLED]: 'Cancelled',
      [OrderStatus.FAILED]: 'Failed'
    };
    return statusNames[status] || status;
  }

  getPriorityDisplayName(priority: OrderPriority): string {
    const priorityNames: { [key in OrderPriority]: string } = {
      [OrderPriority.LOW]: 'Low',
      [OrderPriority.NORMAL]: 'Normal',
      [OrderPriority.HIGH]: 'High',
      [OrderPriority.URGENT]: 'Urgent'
    };
    return priorityNames[priority] || priority;
  }

  getStatusClass(status: OrderStatus): string {
    const statusClasses: { [key in OrderStatus]: string } = {
      [OrderStatus.PENDING]: 'status-pending',
      [OrderStatus.OPEN_FOR_BID]: 'status-open-for-bid',
      [OrderStatus.ASSIGNED]: 'status-assigned',
      [OrderStatus.CONFIRMED]: 'status-confirmed',
      [OrderStatus.IN_PROGRESS]: 'status-in-progress',
      [OrderStatus.PICKED_UP]: 'status-picked-up',
      [OrderStatus.IN_TRANSIT]: 'status-in-transit',
      [OrderStatus.DELIVERED]: 'status-delivered',
      [OrderStatus.CANCELLED]: 'status-cancelled',
      [OrderStatus.FAILED]: 'status-failed'
    };
    return statusClasses[status] || 'status-default';
  }

  getPriorityClass(priority: OrderPriority): string {
    const priorityClasses: { [key in OrderPriority]: string } = {
      [OrderPriority.LOW]: 'priority-low',
      [OrderPriority.NORMAL]: 'priority-normal',
      [OrderPriority.HIGH]: 'priority-high',
      [OrderPriority.URGENT]: 'priority-urgent'
    };
    return priorityClasses[priority] || 'priority-default';
  }

  // Real-time updates (implement WebSocket or polling as needed)
  refreshOrders(): void {
    const user = this.authService.getCurrentUser();
    if (user?.role === 'SUPER_ADMIN') {
      this.getAllOrders().subscribe(response => {
        this.ordersSubject.next(response.content);
      });
    } else {
      this.getMyOrders().subscribe(orders => {
        this.ordersSubject.next(orders);
      });
    }
  }

  // Maps the backend Order entity JSON into the flat UI model used by the templates.
  public mapOrderEntity(raw: any): OrderDTO {
    if (!raw) return raw;
    const numberOr = (v: any): number | undefined =>
      (v === null || v === undefined) ? undefined : Number(v);
    const numberOrZero = (v: any): number =>
      (v === null || v === undefined) ? 0 : Number(v);

    const vendor = raw.vendorCompany || {};
    const delivery = raw.deliveryCompany || {};
    const customer = raw.customerUser || {};
    const driver = raw.driverPerson || {};

    const status = raw.status as OrderStatus;
    const completedStatuses = [OrderStatus.DELIVERED, OrderStatus.CANCELLED];
    const isOverdue = raw.isOverdue === true || (
      raw.estimatedDeliveryTime != null &&
      !completedStatuses.includes(status) &&
      new Date(raw.estimatedDeliveryTime).getTime() < Date.now()
    );

    return {
      id: raw.id,
      orderNumber: raw.orderNumber,
      trackingNumber: raw.trackingNumber,
      orderType: (raw.orderType as OrderType) || OrderType.MARKETPLACE,
      routingMode: (raw.routingMode as RoutingMode) || RoutingMode.OPEN_BID,
      senderName: raw.senderName,
      senderPhone: raw.senderPhone,
      recipientName: raw.recipientName,
      recipientPhone: raw.recipientPhone,
      proposedMinAmount: numberOr(raw.proposedMinAmount),
      proposedMaxAmount: numberOr(raw.proposedMaxAmount),
      vendorCompanyId: vendor.id,
      vendorCompanyName: vendor.companyName,
      deliveryCompanyId: delivery.id,
      deliveryCompanyName: delivery.companyName,
      partnershipId: raw.partnership ? raw.partnership.id : undefined,
      customerUserId: customer.id,
      customerName: [customer.firstName, customer.lastName].filter(Boolean).join(' ') || customer.username || '',
      customerPhone: customer.phoneNumber || '',
      customerEmail: customer.email || '',
      driverPersonId: raw.driverPersonId ?? driver.id,
      driverName: [driver.firstName, driver.lastName].filter(Boolean).join(' ') || '',
      driverPhone: driver.phoneNumber || '',
      pickupAddress: raw.pickupAddress,
      deliveryAddress: raw.deliveryAddress,
      pickupLatitude: numberOr(raw.pickupLatitude),
      pickupLongitude: numberOr(raw.pickupLongitude),
      deliveryLatitude: numberOr(raw.deliveryLatitude),
      deliveryLongitude: numberOr(raw.deliveryLongitude),
      description: raw.description,
      orderValue: numberOrZero(raw.orderAmount),
      deliveryFee: numberOrZero(raw.deliveryFee),
      totalAmount: numberOrZero(raw.totalAmount),
      estimatedDistance: numberOr(raw.distanceKm),
      estimatedDurationMinutes: undefined,
      status,
      priority: (raw.priority as OrderPriority) || OrderPriority.NORMAL,
      orderDate: raw.createdAt,
      scheduledPickupTime: raw.scheduledPickupTime,
      actualPickupTime: raw.actualPickupTime,
      estimatedDeliveryTime: raw.estimatedDeliveryTime,
      actualDeliveryTime: raw.actualDeliveryTime,
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
      customerRating: numberOr(raw.rating),
      customerReview: raw.review,
      specialInstructions: raw.specialInstructions,
      cancellationReason: raw.cancellationReason,
      notes: raw.notes,
      isOverdue,
      isAssigned: !!raw.driverPersonId || !!driver.id,
      isCompleted: status === OrderStatus.DELIVERED,
      isCancelled: status === OrderStatus.CANCELLED,
      durationMinutes: undefined,
      statusDisplayName: this.getStatusDisplayName(status),
      priorityDisplayName: this.getPriorityDisplayName((raw.priority as OrderPriority) || OrderPriority.NORMAL)
    };
  }

  // Error handling helper
  private handleError(error: any): Observable<never> {
    console.error('Order service error:', error);
    throw error;
  }
}