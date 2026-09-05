import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// DeliveryOwner Interface
export interface DeliveryOwner {
  id?: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  nationalId?: string;
  dateOfBirth?: string;
  address?: string;
  emergencyContact?: string;
  enabled: boolean;
  verified: boolean;
  businessExperience?: number;
  preferredBusinessCategory?: string;
  totalCompanies?: number;
  totalDeliveries?: number;
  totalRevenue?: number;
  lastLoginDate?: string;
  profilePicture?: string;
  businessLicense?: string;
  insuranceNumber?: string;
  bankAccountInfo?: string;
  socialMediaLinks?: string[];
  businessDescription?: string;
  operatingAreas?: string[];
  vehicleTypes?: string[];
  rating?: number;
  reviewCount?: number;
  isActive?: boolean;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}


export interface DeliveryOwnerSearchParams {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  nationalId?: string;
  enabled?: boolean;
  verified?: boolean;
  preferredBusinessCategory?: string;
  minBusinessExperience?: number;
  createdAfter?: string;
  createdBefore?: string;
  bornAfter?: string;
  bornBefore?: string;
  address?: string;
  emergencyContact?: string;
  minRating?: number;
  operatingArea?: string;
  vehicleType?: string;
  isActive?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface DeliveryOwnerStats {
  total: number;
  active: number;
  verified: number;
  activeAndVerified: number;
  experienced: number;
  withMultipleCompanies: number;
  highRated: number;
}

@Injectable({
  providedIn: 'root'
})
export class DeliveryOwnerService {
  private readonly baseUrl = `${API_BASE_URL}/delivery-owners`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  private handleError = (error: any): Observable<never> => {
    console.error('Delivery owner service error:', error);
    throw error;
  }

  // Create operations
  createDeliveryOwner(owner: DeliveryOwner): Observable<DeliveryOwner> {
    return this.http.post<DeliveryOwner>(this.baseUrl, owner).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getDeliveryOwnerById(id: number): Observable<DeliveryOwner> {
    return this.http.get<DeliveryOwner>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAllDeliveryOwners(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<DeliveryOwner>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<DeliveryOwner>>(this.baseUrl, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchDeliveryOwners(searchParams: DeliveryOwnerSearchParams): Observable<PaginatedResponse<DeliveryOwner>> {
    let params = new HttpParams();
    
    Object.keys(searchParams).forEach(key => {
      const value = (searchParams as any)[key];
      if (value !== undefined && value !== null && value !== '') {
        if (Array.isArray(value)) {
          value.forEach(v => params = params.append(key, v));
        } else {
          params = params.set(key, value.toString());
        }
      }
    });

    return this.http.get<PaginatedResponse<DeliveryOwner>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Quick search endpoints
  searchByName(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryOwner>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<DeliveryOwner>>(`${this.baseUrl}/search/name`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchByContact(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryOwner>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<DeliveryOwner>>(`${this.baseUrl}/search/contact`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedDeliveryOwners(page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryOwner>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<DeliveryOwner>>(`${this.baseUrl}/active-verified`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getExperiencedDeliveryOwners(page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryOwner>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<DeliveryOwner>>(`${this.baseUrl}/experienced`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getNewDeliveryOwners(since?: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryOwner>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (since) {
      params = params.set('since', since);
    }

    return this.http.get<PaginatedResponse<DeliveryOwner>>(`${this.baseUrl}/new`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }



  // Lookup endpoints
  getByUsername(username: string): Observable<DeliveryOwner> {
    return this.http.get<DeliveryOwner>(`${this.baseUrl}/username/${encodeURIComponent(username)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByEmail(email: string): Observable<DeliveryOwner> {
    return this.http.get<DeliveryOwner>(`${this.baseUrl}/email/${encodeURIComponent(email)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByNationalId(nationalId: string): Observable<DeliveryOwner> {
    return this.http.get<DeliveryOwner>(`${this.baseUrl}/national-id/${encodeURIComponent(nationalId)}`).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateDeliveryOwner(id: number, owner: DeliveryOwner): Observable<DeliveryOwner> {
    return this.http.put<DeliveryOwner>(`${this.baseUrl}/${id}`, owner).pipe(
      catchError(this.handleError)
    );
  }

  updateEnabledStatus(id: number, enabled: boolean): Observable<DeliveryOwner> {
    const params = new HttpParams().set('enabled', enabled.toString());
    
    return this.http.patch<DeliveryOwner>(`${this.baseUrl}/${id}/enabled`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateVerifiedStatus(id: number, verified: boolean): Observable<DeliveryOwner> {
    const params = new HttpParams().set('verified', verified.toString());
    
    return this.http.patch<DeliveryOwner>(`${this.baseUrl}/${id}/verified`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateActiveStatus(id: number, isActive: boolean): Observable<DeliveryOwner> {
    const params = new HttpParams().set('isActive', isActive.toString());
    
    return this.http.patch<DeliveryOwner>(`${this.baseUrl}/${id}/active`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateRating(id: number, rating: number): Observable<DeliveryOwner> {
    const params = new HttpParams().set('rating', rating.toString());
    
    return this.http.patch<DeliveryOwner>(`${this.baseUrl}/${id}/rating`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  deleteDeliveryOwner(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  softDeleteDeliveryOwner(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}/soft`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getDeliveryOwnerCounts(): Observable<DeliveryOwnerStats> {
    return this.http.get<DeliveryOwnerStats>(`${this.baseUrl}/stats/count`).pipe(
      catchError(this.handleError)
    );
  }

  getTotalDeliveryOwnersCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/total`).pipe(
      catchError(this.handleError)
    );
  }

  getActiveDeliveryOwnersCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active`).pipe(
      catchError(this.handleError)
    );
  }

  getVerifiedDeliveryOwnersCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/verified`).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedDeliveryOwnersCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active-verified`).pipe(
      catchError(this.handleError)
    );
  }

  getExperiencedDeliveryOwnersCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/experienced`).pipe(
      catchError(this.handleError)
    );
  }

  getDeliveryOwnersWithMultipleCompaniesCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/multiple-companies`).pipe(
      catchError(this.handleError)
    );
  }

  // Validation endpoints
  existsByUsername(username: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/username/${encodeURIComponent(username)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByEmail(email: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(email)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByNationalId(nationalId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/national-id/${encodeURIComponent(nationalId)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByUsernameAndIdNot(username: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/username/${encodeURIComponent(username)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByEmailAndIdNot(email: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(email)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByNationalIdAndIdNot(nationalId: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/national-id/${encodeURIComponent(nationalId)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }
}