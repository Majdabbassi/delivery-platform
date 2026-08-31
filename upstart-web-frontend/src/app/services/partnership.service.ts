import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// Partnership Interface
export interface Partnership {
  id?: number;
  vendorCompanyId: number;
  deliveryCompanyId: number;
  status: 'ACTIVE' | 'SUSPENDED' | 'TERMINATED' | 'PENDING';
  startDate: string;
  endDate?: string;
  isExclusive: boolean;
  serviceArea: string;
  commissionRate: number;
  totalOrders?: number;
  totalRevenue?: number;
  averageRating?: number;
  createdAt?: string;
  updatedAt?: string;
  performanceScore?: number;
  contractTerms?: string;
  notes?: string;
}

// Paginated Response Interface
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

// Partnership Search Parameters
export interface PartnershipSearchParams {
  vendorCompanyId?: number;
  deliveryCompanyId?: number;
  status?: string;
  serviceArea?: string;
  isExclusive?: boolean;
  minCommissionRate?: number;
  maxCommissionRate?: number;
  startDateAfter?: string;
  startDateBefore?: string;
  endDateAfter?: string;
  endDateBefore?: string;
  minTotalOrders?: number;
  minTotalRevenue?: number;
  minAverageRating?: number;
  createdAfter?: string;
  createdBefore?: string;
}

// Partnership Statistics Interface
export interface PartnershipStats {
  total: number;
  active: number;
  suspended: number;
  terminated: number;
  pending: number;
  exclusive: number;
  totalRevenue: number;
  averageRating: number;
  totalOrders: number;
}

@Injectable({
  providedIn: 'root'
})
export class PartnershipService {
  private baseUrl = `${API_BASE_URL}/partnerships`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  // CRUD Operations
  createPartnership(partnership: Partnership): Observable<Partnership> {
    return this.http.post<Partnership>(this.baseUrl, partnership);
  }

  getPartnershipById(id: number): Observable<Partnership> {
    return this.http.get<Partnership>(`${this.baseUrl}/${id}`);
  }

  getPartnershipByCompanies(vendorCompanyId: number, deliveryCompanyId: number): Observable<Partnership> {
    const params = new HttpParams()
      .set('vendorCompanyId', vendorCompanyId.toString())
      .set('deliveryCompanyId', deliveryCompanyId.toString());
    
    return this.http.get<Partnership>(`${this.baseUrl}/companies`, {
      params
    });
  }

  getAllPartnerships(page: number = 0, size: number = 20): Observable<PaginatedResponse<Partnership>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PaginatedResponse<Partnership>>(this.baseUrl, {
      params
    });
  }

  updatePartnership(id: number, partnership: Partnership): Observable<Partnership> {
    return this.http.put<Partnership>(`${this.baseUrl}/${id}`, partnership);
  }

  deletePartnership(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // Status Management
  updatePartnershipStatus(id: number, status: string): Observable<Partnership> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/status`, null, {
      params
    });
  }

  activatePartnership(id: number): Observable<Partnership> {
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/activate`, null);
  }

  suspendPartnership(id: number): Observable<Partnership> {
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/suspend`, null);
  }

  terminatePartnership(id: number): Observable<Partnership> {
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/terminate`, null);
  }

  // Performance Management
  updatePerformanceMetrics(id: number, performanceData: any): Observable<Partnership> {
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/performance`, performanceData);
  }

  incrementOrderCount(id: number): Observable<Partnership> {
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/increment-orders`, null);
  }

  addRevenue(id: number, amount: number): Observable<Partnership> {
    const body = { amount };
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/add-revenue`, body);
  }

  addRating(id: number, rating: number): Observable<Partnership> {
    const body = { rating };
    return this.http.patch<Partnership>(`${this.baseUrl}/${id}/add-rating`, body);
  }

  // Query Operations
  getPartnershipsByVendorCompany(vendorCompanyId: number): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/vendor-company/${vendorCompanyId}`);
  }

  getPartnershipsByDeliveryCompany(deliveryCompanyId: number): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/delivery-company/${deliveryCompanyId}`);
  }

  getActivePartnershipsByVendorCompany(vendorCompanyId: number): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/vendor-company/${vendorCompanyId}/active`);
  }

  getActivePartnershipsByDeliveryCompany(deliveryCompanyId: number): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/delivery-company/${deliveryCompanyId}/active`);
  }

  getExclusivePartnershipByVendorCompany(vendorCompanyId: number): Observable<Partnership> {
    return this.http.get<Partnership>(`${this.baseUrl}/vendor-company/${vendorCompanyId}/exclusive`);
  }

  getPartnershipsByStatus(status: string): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/status/${status}`);
  }

  getActivePartnerships(): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/active`);
  }

  getExpiredActivePartnerships(): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/expired`);
  }

  getPartnershipsExpiringWithin(days: number): Observable<Partnership[]> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<Partnership[]>(`${this.baseUrl}/expiring`, {
      params
    });
  }

  getPartnershipsByServiceArea(serviceArea: string): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/service-area/${serviceArea}`);
  }

  getEligiblePartnershipsForOrder(orderData: any): Observable<Partnership[]> {
    return this.http.post<Partnership[]>(`${this.baseUrl}/eligible-for-order`, orderData);
  }

  getHighPerformingPartnerships(): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/high-performing`);
  }

  getHighRatedPartnerships(): Observable<Partnership[]> {
    return this.http.get<Partnership[]>(`${this.baseUrl}/high-rated`);
  }

  // Statistics
  countPartnershipsByVendorCompany(vendorCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/vendor-company/${vendorCompanyId}`);
  }

  countPartnershipsByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/delivery-company/${deliveryCompanyId}`);
  }

  countPartnershipsByStatus(status: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/status/${status}`);
  }

  countActivePartnershipsByVendorCompany(vendorCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active/vendor-company/${vendorCompanyId}`);
  }

  countActivePartnershipsByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active/delivery-company/${deliveryCompanyId}`);
  }

  getTotalRevenueByVendorCompany(vendorCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/revenue/vendor-company/${vendorCompanyId}`);
  }

  getTotalRevenueByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/revenue/delivery-company/${deliveryCompanyId}`);
  }

  getAverageRatingByVendorCompany(vendorCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/rating/vendor-company/${vendorCompanyId}`);
  }

  getAverageRatingByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/rating/delivery-company/${deliveryCompanyId}`);
  }

  getTotalOrdersByVendorCompany(vendorCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/orders/vendor-company/${vendorCompanyId}`);
  }

  getTotalOrdersByDeliveryCompany(deliveryCompanyId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/orders/delivery-company/${deliveryCompanyId}`);
  }

  // Validation Methods
  existsPartnershipBetween(vendorCompanyId: number, deliveryCompanyId: number): Observable<boolean> {
    const params = new HttpParams()
      .set('vendorCompanyId', vendorCompanyId.toString())
      .set('deliveryCompanyId', deliveryCompanyId.toString());
    
    return this.http.get<boolean>(`${this.baseUrl}/exists`, {
      params
    });
  }

  existsActivePartnershipBetween(vendorCompanyId: number, deliveryCompanyId: number): Observable<boolean> {
    const params = new HttpParams()
      .set('vendorCompanyId', vendorCompanyId.toString())
      .set('deliveryCompanyId', deliveryCompanyId.toString());
    
    return this.http.get<boolean>(`${this.baseUrl}/exists/active`, {
      params
    });
  }

  hasExclusivePartnership(vendorCompanyId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/vendor-company/${vendorCompanyId}/has-exclusive`);
  }

  // Maintenance
  updateExpiredPartnerships(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/maintenance/update-expired`, null);
  }

  // Error handling helper
  private handleError(error: any): Observable<never> {
    console.error('Partnership service error:', error);
    throw error;
  }
}