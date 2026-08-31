import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// Enums
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
  FAILED = 'FAILED'
}

export enum OrderPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

// DTOs and Interfaces
export interface CreateOrderDTO {
  vendorCompanyId: number;
  customerUserId: number;
  deliveryCompanyId?: number;
  partnershipId?: number;
  driverPersonId?: number;
  
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

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
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
    return this.http.post<OrderDTO>(this.baseUrl, orderData);
  }

  getOrderById(id: number): Observable<OrderDTO> {
    return this.http.get<OrderDTO>(`${this.baseUrl}/${id}`);
  }

  getOrderByOrderNumber(orderNumber: string): Observable<OrderDTO> {
    return this.http.get<OrderDTO>(`${this.baseUrl}/order-number/${orderNumber}`);
  }

  getOrderByTrackingNumber(trackingNumber: string): Observable<OrderDTO> {
    // Public tracking - no auth required
    return this.http.get<OrderDTO>(`${this.baseUrl}/tracking/${trackingNumber}`);
  }

  getAllOrders(page: number = 0, size: number = 20): Observable<PaginatedResponse<OrderDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PaginatedResponse<OrderDTO>>(this.baseUrl, {
      params
    });
  }

  updateOrder(id: number, orderData: UpdateOrderDTO): Observable<OrderDTO> {
    return this.http.put<OrderDTO>(`${this.baseUrl}/${id}`, orderData);
  }

  deleteOrder(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // Order Status & Assignment Management
  updateOrderStatus(id: number, status: OrderStatus): Observable<OrderDTO> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/status`, null, {
      params
    });
  }

  assignDeliveryCompany(id: number, deliveryCompanyId: number): Observable<OrderDTO> {
    const params = new HttpParams().set('deliveryCompanyId', deliveryCompanyId.toString());
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/assign-delivery-company`, null, {
      params
    });
  }

  assignPartnership(id: number, partnershipId: number): Observable<OrderDTO> {
    const params = new HttpParams().set('partnershipId', partnershipId.toString());
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/assign-partnership`, null, {
      params
    });
  }

  addRatingAndReview(id: number, ratingData: OrderRatingDTO): Observable<OrderDTO> {
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/rating`, ratingData);
  }

  // Alias for backward compatibility
  addOrderRating(id: number, ratingData: OrderRatingDTO): Observable<OrderDTO> {
    return this.addRatingAndReview(id, ratingData);
  }

  cancelOrder(id: number, cancellationReason?: string): Observable<OrderDTO> {
    const body = cancellationReason ? { cancellationReason } : {};
    return this.http.patch<OrderDTO>(`${this.baseUrl}/${id}/cancel`, body);
  }

  // Query & Filtering Operations
  getOrdersByVendorCompany(vendorCompanyId: number): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/vendor-company/${vendorCompanyId}`);
  }

  getOrdersByDeliveryCompany(deliveryCompanyId: number): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/delivery-company/${deliveryCompanyId}`);
  }

  getOrdersByPartnership(partnershipId: number): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/partnership/${partnershipId}`);
  }

  getOrdersByStatus(status: OrderStatus): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/status/${status}`);
  }

  getPendingUnassignedOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/pending-unassigned`);
  }

  getOverdueOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/overdue`);
  }

  getActiveUrgentOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(`${this.baseUrl}/urgent`);
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
      [OrderStatus.ASSIGNED]: 'Assigned',
      [OrderStatus.CONFIRMED]: 'Confirmed',
      [OrderStatus.IN_PROGRESS]: 'In Progress',
      [OrderStatus.PICKED_UP]: 'Picked Up',
      [OrderStatus.IN_TRANSIT]: 'In Transit',
      [OrderStatus.DELIVERED]: 'Delivered',
      [OrderStatus.COMPLETED]: 'Completed',
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
      [OrderStatus.ASSIGNED]: 'status-assigned',
      [OrderStatus.CONFIRMED]: 'status-confirmed',
      [OrderStatus.IN_PROGRESS]: 'status-in-progress',
      [OrderStatus.PICKED_UP]: 'status-picked-up',
      [OrderStatus.IN_TRANSIT]: 'status-in-transit',
      [OrderStatus.DELIVERED]: 'status-delivered',
      [OrderStatus.COMPLETED]: 'status-completed',
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
    this.getAllOrders().subscribe(response => {
      this.ordersSubject.next(response.content);
    });
  }

  // Error handling helper
  private handleError(error: any): Observable<never> {
    console.error('Order service error:', error);
    throw error;
  }
}