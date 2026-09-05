import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// VendorOwner Interface
export interface VendorOwner {
  id?: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  nationalId?: string;
  enabled: boolean;
  verified: boolean;
  preferredBusinessCategory?: string;
  businessExperience?: number;
  businessExperienceYears?: number;
  businessLicenseNumber?: string;
  maxCompaniesAllowed?: number;
  preferredBusinessCategories?: string;
  password?: string;
  isEnabled?: boolean;
  isVerifiedOwner?: boolean;
  totalProductsManaged?: number;
  totalRevenue?: number;
  totalCompanies?: number;
  createdAt?: string;
  updatedAt?: string;
  dateOfBirth?: string;
  address?: string;
  emergencyContact?: string;
}

// Paginated Response Interface

// Search Parameters Interface
export interface VendorOwnerSearchParams {
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
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

// Statistics Interface
export interface VendorOwnerStats {
  total: number;
  active: number;
  verified: number;
  activeAndVerified: number;
  experienced: number;
  withMultipleCompanies: number;
  totalCompanies: number;
  totalRevenue: number;
  avgExperience: number;
}

@Injectable({
  providedIn: 'root'
})
export class VendorOwnerService {
  private readonly baseUrl = `${API_BASE_URL}/vendor-owners`;

  constructor(private http: HttpClient) {}


  // Create operations
  createVendorOwner(vendorOwner: VendorOwner): Observable<VendorOwner> {
    return this.http.post<VendorOwner>(this.baseUrl, vendorOwner).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  searchVendorOwners(searchParams: VendorOwnerSearchParams): Observable<PaginatedResponse<VendorOwner>> {
    let params = new HttpParams();
    
    // Add all search parameters
    Object.keys(searchParams).forEach(key => {
      const value = (searchParams as any)[key];
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, value.toString());
      }
    });

    return this.http.get<PaginatedResponse<VendorOwner>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateVendorOwner(id: number, vendorOwner: VendorOwner): Observable<VendorOwner> {
    return this.http.put<VendorOwner>(`${this.baseUrl}/${id}`, vendorOwner).pipe(
      catchError(this.handleError)
    );
  }

  updateEnabledStatus(id: number, enabled: boolean): Observable<VendorOwner> {
    const params = new HttpParams().set('enabled', enabled.toString());
    
    return this.http.patch<VendorOwner>(`${this.baseUrl}/${id}/enabled`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateVerifiedStatus(id: number, verified: boolean): Observable<VendorOwner> {
    const params = new HttpParams().set('verified', verified.toString());
    
    return this.http.patch<VendorOwner>(`${this.baseUrl}/${id}/verified`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  deleteVendorOwner(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getVendorOwnerCounts(): Observable<VendorOwnerStats> {
    return this.http.get<VendorOwnerStats>(`${this.baseUrl}/stats/count`).pipe(
      catchError(this.handleError)
    );
  }

  // Utility methods
  getStatusClass(enabled: boolean, verified: boolean): string {
    if (enabled && verified) return 'status-active-verified';
    if (enabled) return 'status-active';
    if (verified) return 'status-verified';
    return 'status-inactive';
  }

  getStatusDisplayName(enabled: boolean, verified: boolean): string {
    if (enabled && verified) return 'Active & Verified';
    if (enabled) return 'Active';
    if (verified) return 'Verified';
    return 'Inactive';
  }

  formatBusinessExperience(years?: number): string {
    if (!years) return 'No experience';
    return years === 1 ? '1 year' : `${years} years`;
  }

  getFullName(vendorOwner: VendorOwner): string {
    return `${vendorOwner.firstName} ${vendorOwner.lastName}`.trim();
  }

  private handleError(error: any): Observable<never> {
    console.error('VendorOwner service error:', error);
    throw error;
  }
}