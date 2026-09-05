import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// CustomerUser Interface
export interface CustomerUser {
  id?: number;
  username: string;
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  role: string;
  isEnabled: boolean;
  createdAt?: string;
  updatedAt?: string;
  dateOfBirth?: string;
  address?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  paymentMethod?: string;
  loyaltyPoints?: number;
  defaultAddress?: string;
  preferredPaymentMethod?: string;
  totalOrders?: number;
  totalSpent?: number;
  lastOrderDate?: string;
  membershipLevel?: string;
  isVerified?: boolean;
  isPremium?: boolean;
  emergencyContact?: string;
  notificationPreferences?: string;
}


export interface CustomerUserSearchParams {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  enabled?: boolean;
  verified?: boolean;
  isPremium?: boolean;
  membershipLevel?: string;
  minLoyaltyPoints?: number;
  createdAfter?: string;
  createdBefore?: string;
  bornAfter?: string;
  bornBefore?: string;
  address?: string;
  emergencyContact?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface CustomerUserStats {
  total: number;
  active: number;
  verified: number;
  activeAndVerified: number;
  premium: number;
  withMultipleOrders: number;
}

@Injectable({
  providedIn: 'root'
})
export class CustomerUserService {
  private readonly baseUrl = `${API_BASE_URL}/customer-users`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  private handleError = (error: any): Observable<never> => {
    console.error('CustomerUser service error:', error);
    throw error;
  }

  // Create operations
  createCustomerUser(customerUser: CustomerUser): Observable<CustomerUser> {
    return this.http.post<CustomerUser>(this.baseUrl, customerUser).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getCustomerUserById(id: number): Observable<CustomerUser> {
    return this.http.get<CustomerUser>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getCustomerUserByUsername(username: string): Observable<CustomerUser> {
    return this.http.get<CustomerUser>(`${this.baseUrl}/username/${encodeURIComponent(username)}`).pipe(
      catchError(this.handleError)
    );
  }

  searchCustomerUsers(searchParams: CustomerUserSearchParams): Observable<PaginatedResponse<CustomerUser>> {
    let params = new HttpParams();
    
    Object.keys(searchParams).forEach(key => {
      const value = (searchParams as any)[key];
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value.toString());
      }
    });

    return this.http.get<PaginatedResponse<CustomerUser>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getAllCustomerUsers(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<CustomerUser>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<CustomerUser>>(this.baseUrl, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateCustomerUser(id: number, customerUser: CustomerUser): Observable<CustomerUser> {
    return this.http.put<CustomerUser>(`${this.baseUrl}/${id}`, customerUser).pipe(
      catchError(this.handleError)
    );
  }

  updateLoyaltyPoints(id: number, loyaltyPoints: number): Observable<CustomerUser> {
    const body = { loyaltyPoints };
    return this.http.patch<CustomerUser>(`${this.baseUrl}/${id}/loyalty-points`, body).pipe(
      catchError(this.handleError)
    );
  }

  updatePremiumStatus(id: number, isPremium: boolean): Observable<CustomerUser> {
    const body = { isPremium };
    return this.http.patch<CustomerUser>(`${this.baseUrl}/${id}/premium-status`, body).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  deleteCustomerUser(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  hardDeleteCustomerUser(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}/hard`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getCustomerUserStatistics(): Observable<CustomerUserStats> {
    return this.http.get<CustomerUserStats>(`${this.baseUrl}/statistics`).pipe(
      catchError(this.handleError)
    );
  }
}